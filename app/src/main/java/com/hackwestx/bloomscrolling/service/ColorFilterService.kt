package com.hackwestx.bloomscrolling.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Camada laranja/vermelha desenhada por cima de tudo enquanto a pessoa está
 * num app bloqueado. Começa transparente e vai ficando mais forte quanto mais
 * tempo a sessão dura, até um teto — a ideia é a tela ficar visualmente
 * "menos convidativa" quanto mais tempo a pessoa fica no app.
 *
 * Quem manda nele é o MonitorAccessibilityService, no mesmo esquema do
 * UsageTimerService: start() quando abre um app bloqueado, stop() quando sai
 * dele. Este serviço não decide nada sobre blocklist, só desenha a camada.
 *
 * Não toca no banco nem usa Hilt de propósito — é puramente visual, então
 * fica isolado (mais fácil de mexer sem afetar o resto do time).
 */
class ColorFilterService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tickJob: Job? = null
    private var overlayView: View? = null
    private var sessionStartElapsed = 0L

    private val windowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }

    /**
     * A tela apagar NÃO gera evento de acessibilidade — sem isto, a camada
     * laranja continuaria pendurada por cima de tudo, inclusive da tela de
     * bloqueio, quando o aparelho fosse acordado. Mesmo tratamento que o
     * UsageTimerService já faz para a contagem de tempo.
     */
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // onReceive já roda na thread principal, que é onde o
            // WindowManager exige ser chamado.
            stopFilter()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // Sem isto, startForeground() pode rodar antes de qualquer outro
        // serviço ter criado o canal e derrubar o app com
        // "Bad notification for startForeground: No Channel found".
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
        // independentemente do que o comando peça (mesma regra do
        // MyForegroundService e do UsageTimerService).
        startForeground(
            NOTIFICATION_ID,
            NotificationHelper.buildNotification(this, "Filtro de cor ativo")
        )

        when (intent?.action) {
            ACTION_START -> startFilter()
            ACTION_STOP -> stopFilter()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        unregisterReceiver(screenOffReceiver)
        tickJob?.cancel()
        removeOverlay()
        scope.cancel()
        super.onDestroy()
    }

    // -----------------------------------------------------------------------
    // Sessão
    // -----------------------------------------------------------------------

    private fun startFilter() {
        // Já rodando (ex.: dois eventos de acessibilidade seguidos pro mesmo
        // app) — não reinicia a rampa, senão o filtro nunca chegaria no forte.
        if (overlayView != null) return

        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Sem permissão de overlay — filtro de cor não vai aparecer")
            stopSelf()
            return
        }

        sessionStartElapsed = SystemClock.elapsedRealtime()
        addOverlay()

        tickJob = scope.launch {
            while (isActive) {
                updateAlpha()
                delay(TICK_MILLIS)
            }
        }
    }

    private fun stopFilter() {
        tickJob?.cancel()
        tickJob = null
        removeOverlay()
        stopSelf()
    }

    // -----------------------------------------------------------------------
    // Overlay
    // -----------------------------------------------------------------------

    private fun addOverlay() {
        val view = View(this).apply {
            setBackgroundColor(Color.argb(0, FILTER_RED, FILTER_GREEN, FILTER_BLUE))
        }

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType,
            // NOT_FOCUSABLE + NOT_TOUCHABLE: é só visual, não atrapalha o
            // toque no app por baixo. LAYOUT_NO_LIMITS: cobre a tela inteira,
            // incluindo atrás da barra de status.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        runCatching { windowManager.addView(view, params) }
            .onFailure { Log.w(TAG, "Falha ao adicionar overlay: ${it.message}") }

        overlayView = view
    }

    private fun updateAlpha() {
        val elapsedSeconds = (SystemClock.elapsedRealtime() - sessionStartElapsed) / 1000
        val progress = (elapsedSeconds.toFloat() / RAMP_SECONDS).coerceIn(0f, 1f)
        val alpha = (progress * MAX_ALPHA).toInt()
        overlayView?.setBackgroundColor(Color.argb(alpha, FILTER_RED, FILTER_GREEN, FILTER_BLUE))
    }

    private fun removeOverlay() {
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
    }

    companion object {
        private const val TAG = "BloomScrolling"
        private const val NOTIFICATION_ID = 1003

        private const val ACTION_START = "com.hackwestx.bloomscrolling.COLOR_FILTER_START"
        private const val ACTION_STOP = "com.hackwestx.bloomscrolling.COLOR_FILTER_STOP"

        private const val TICK_MILLIS = 1_000L

        // Quanto tempo (em segundos) até o filtro chegar na força máxima.
        // 60s de propósito para dar pra ver crescendo numa demonstração —
        // pra uso real, provavelmente vale subir pra uns 300s (5 min).
        private const val RAMP_SECONDS = 60f

        // Opacidade máxima, de 0 a 255. 140 ~= 55%: dá pra notar bem sem
        // deixar o app de baixo completamente ilegível.
        private const val MAX_ALPHA = 140

        // Tom quente laranja/vermelho (Material Deep Orange 700, #E64A19).
        private const val FILTER_RED = 0xE6
        private const val FILTER_GREEN = 0x4A
        private const val FILTER_BLUE = 0x19

        /** Começa (ou continua) a rampa do filtro. */
        fun start(context: Context) {
            val intent = Intent(context, ColorFilterService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        /** Remove o filtro e encerra o serviço. */
        fun stop(context: Context) {
            val intent = Intent(context, ColorFilterService::class.java).apply {
                action = ACTION_STOP
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
