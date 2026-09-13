package com.hackwestx.bloomscrolling.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.hackwestx.bloomscrolling.data.UsageDao
import com.hackwestx.bloomscrolling.data.UsageLog
import com.hackwestx.bloomscrolling.util.startOfToday
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * Cronometra o tempo em um app monitorado e grava o resultado em usage_log.
 *
 * Quem manda nele é o [MonitorAccessibilityService]: ele detecta a troca de
 * app e chama [start] ou [stop]. Este serviço não decide nada sobre blocklist —
 * só conta o tempo do pacote que mandarem contar.
 */
@AndroidEntryPoint
class UsageTimerService : Service() {

    @Inject lateinit var usageDao: UsageDao

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Serializa start/stop para dois comandos rápidos não se atropelarem. */
    private val sessionMutex = Mutex()

    /** Serializa o ler-somar-gravar do banco (ver [addUsage]). */
    private val dbMutex = Mutex()

    private var tickJob: Job? = null
    private var trackedPackage: String? = null

    /** Relógio monotônico do início da sessão atual. */
    private var sessionStartElapsed = 0L

    /** Quantos segundos desta sessão já viraram minutos gravados no banco. */
    private var persistedSeconds = 0L

    /**
     * A tela apagar NÃO gera evento de acessibilidade — sem isto, o Instagram
     * ficaria "aberto" a noite inteira com o celular no bolso.
     */
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            scope.launch {
                stopTracking()
                stopSelf()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        ContextCompat.registerReceiver(
            this,
            screenOffReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android 14+ exige startForeground() nos primeiros ~5 segundos,
        // independentemente do que o comando peça. Por isso vem antes do when.
        startForeground(
            NOTIFICATION_ID,
            NotificationHelper.buildNotification(this, "Contando tempo de uso")
        )

        when (intent?.action) {
            ACTION_START -> {
                val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
                if (packageName != null) {
                    scope.launch { startTracking(packageName) }
                }
            }

            ACTION_STOP -> scope.launch {
                stopTracking()
                stopSelf()
            }
        }

        // START_NOT_STICKY: se o sistema matar o serviço, NÃO queremos que ele
        // volte sozinho com intent nula — voltaria sem saber qual app contar.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        unregisterReceiver(screenOffReceiver)
        scope.cancel()
        super.onDestroy()
    }

    // -----------------------------------------------------------------------
    // Sessão
    // -----------------------------------------------------------------------

    private suspend fun startTracking(packageName: String) = sessionMutex.withLock {
        // Já contando este mesmo app: não é um novo "pickup", nem reinicia o
        // relógio. Acontece bastante — abrir um diálogo dentro do app dispara
        // evento de janela de novo.
        if (packageName == trackedPackage) return@withLock

        stopTrackingInternal()

        trackedPackage = packageName
        sessionStartElapsed = SystemClock.elapsedRealtime()
        persistedSeconds = 0L

        // Pickup gravado JÁ na abertura, com zero minutos. Se esperássemos o
        // primeiro minuto fechar, uma espiada de 10 segundos não contaria —
        // e é exatamente esse tipo de abertura compulsiva que o app quer medir.
        addUsage(packageName, minutesToAdd = 0, pickupsToAdd = 1)

        Log.d(TAG, "Iniciou contagem: $packageName")

        tickJob = scope.launch {
            while (isActive) {
                delay(TICK_MILLIS)
                persistElapsedMinutes(packageName, roundToNearestMinute = false)
            }
        }
    }

    private suspend fun stopTracking() = sessionMutex.withLock { stopTrackingInternal() }

    private suspend fun stopTrackingInternal() {
        val packageName = trackedPackage ?: return

        tickJob?.cancelAndJoin()
        tickJob = null

        // Fecha a conta com arredondamento: 40 segundos viram 1 minuto,
        // 20 segundos viram 0. Sem isso, toda sobra de menos de um minuto
        // seria descartada e o total ficaria sistematicamente menor que o real.
        persistElapsedMinutes(packageName, roundToNearestMinute = true)

        Log.d(TAG, "Parou contagem: $packageName")
        trackedPackage = null
    }

    // -----------------------------------------------------------------------
    // Escrita
    // -----------------------------------------------------------------------

    /**
     * Converte o tempo decorrido ainda não gravado em minutos inteiros e grava.
     *
     * Usa [SystemClock.elapsedRealtime] (relógio monotônico) em vez de contar
     * os ticks: um `delay(15s)` pode atrasar quando o sistema economiza
     * bateria, e contar ticks acumularia esse erro. O relógio não mente.
     */
    private suspend fun persistElapsedMinutes(packageName: String, roundToNearestMinute: Boolean) {
        val elapsedSeconds = (SystemClock.elapsedRealtime() - sessionStartElapsed) / 1000
        val pendingSeconds = elapsedSeconds - persistedSeconds

        val minutes = if (roundToNearestMinute) {
            (pendingSeconds + SECONDS_PER_MINUTE / 2) / SECONDS_PER_MINUTE
        } else {
            pendingSeconds / SECONDS_PER_MINUTE
        }

        if (minutes <= 0) return

        persistedSeconds += minutes * SECONDS_PER_MINUTE
        addUsage(packageName, minutesToAdd = minutes.toInt(), pickupsToAdd = 0)
    }

    /**
     * Soma minutos/pickups à linha de hoje daquele app, criando-a se não existir.
     *
     * O mutex é necessário porque isto é um ler-somar-gravar: se o tick e o
     * stop rodassem ao mesmo tempo, os dois leriam o mesmo valor antigo e um
     * sobrescreveria o outro (o total ficaria menor que o real).
     */
    private suspend fun addUsage(packageName: String, minutesToAdd: Int, pickupsToAdd: Int) {
        dbMutex.withLock {
            // Data resolvida na hora de gravar, não no início da sessão: uma
            // sessão que atravessa a meia-noite continua no dia correto.
            val today = startOfToday()
            val existing = usageDao.getForToday(today, packageName)

            usageDao.upsert(
                UsageLog(
                    date = today,
                    packageName = packageName,
                    totalMinutes = (existing?.totalMinutes ?: 0) + minutesToAdd,
                    pickupCount = (existing?.pickupCount ?: 0) + pickupsToAdd
                )
            )
        }
    }

    companion object {
        private const val TAG = "BloomScrolling"
        private const val NOTIFICATION_ID = 1002
        private const val SECONDS_PER_MINUTE = 60L
        private const val TICK_MILLIS = 15_000L

        private const val ACTION_START = "com.hackwestx.bloomscrolling.TIMER_START"
        private const val ACTION_STOP = "com.hackwestx.bloomscrolling.TIMER_STOP"
        private const val EXTRA_PACKAGE_NAME = "extra_package_name"

        /** Começa (ou continua) a contar o tempo de [packageName]. */
        fun start(context: Context, packageName: String) {
            val intent = Intent(context, UsageTimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PACKAGE_NAME, packageName)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        /** Fecha a sessão atual (se houver) e encerra o serviço. */
        fun stop(context: Context) {
            val intent = Intent(context, UsageTimerService::class.java).apply {
                action = ACTION_STOP
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
