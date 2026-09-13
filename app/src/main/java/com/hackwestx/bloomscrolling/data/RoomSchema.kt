package com.hackwestx.bloomscrolling.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

// ---------------------------------------------------------------------------
// This file is the contract every workstream builds against. Get it right
// (or at least agreed-upon) in Phase 1 before the team splits up for Phase 3
// — changing table shapes after three people have written code against them
// is the single biggest source of last-day merge pain in a hackathon.
// Split into separate files later if you have time; one file is fine for now.
// ---------------------------------------------------------------------------

// ---- Entities (matches the 3-table spec from planning) ----

@Entity(tableName = "survey_response")
data class SurveyResponse(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val packageName: String,
    val reason: String // "bored" | "habit" | "specific_task" | "messaging"
)

@Entity(tableName = "usage_log", primaryKeys = ["date", "packageName"])
data class UsageLog(
    val date: String, // yyyy-MM-dd, device-local date
    val packageName: String,
    val totalMinutes: Int,
    val pickupCount: Int
)

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val packageName: String,
    val dailyLimitMinutes: Int,
    val isBlocked: Boolean,
    // Quando a survey foi mostrada pela última vez para ESTE app (epoch millis).
    // 0 = nunca mostrada, então a primeira abertura sempre pergunta.
    // O defaultValue precisa bater com o DEFAULT 0 da MIGRATION_1_2 abaixo,
    // senão o Room reclama que o schema do banco não confere com o esperado.
    @ColumnInfo(defaultValue = "0") val lastSurveyTimestamp: Long = 0
)

/**
 * Estado global da streak — uma linha só, id sempre [STREAK_STATE_ID].
 *
 * Fica numa tabela própria (e não em user_settings) porque a streak é do
 * usuário, não de um app: user_settings tem uma linha POR APP, então guardar
 * isto lá significaria repetir o mesmo número em cada linha e ter que decidir
 * qual delas é a verdadeira.
 */
@Entity(tableName = "streak_state")
data class StreakState(
    @PrimaryKey val id: Int = STREAK_STATE_ID,

    // Quantos "revives" o usuário tem guardados. Começa em 1 e nunca passa
    // de 1 (regra do hackathon: um de cada vez).
    @ColumnInfo(defaultValue = "1") val revivesAvailable: Int = 1,

    // Datas perdoadas por um revive, em "yyyy-MM-dd" separadas por vírgula.
    // Atalho de hackathon: o certo seria uma tabela à parte com uma linha por
    // data. Como no máximo uma data é perdoada a cada 7 dias de streak, a
    // lista nunca passa de um punhado de itens.
    @ColumnInfo(defaultValue = "''") val revivedDates: String = "",

    // Quantos marcos de 7 dias já renderam um revive. Impede que o mesmo
    // marco premie de novo toda vez que a tela recalcular.
    @ColumnInfo(defaultValue = "0") val milestonesRewarded: Int = 0
)

const val STREAK_STATE_ID = 0

// ---- DAOs ----

data class ReasonCount(val reason: String, val count: Int)

@Dao
interface SurveyDao {
    @Insert
    suspend fun insert(response: SurveyResponse)

    @Query("SELECT * FROM survey_response ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<SurveyResponse>>

    @Query("SELECT reason, COUNT(*) as count FROM survey_response GROUP BY reason")
    suspend fun reasonBreakdown(): List<ReasonCount>
}

data class DailyUsageTotal(val date: String, val totalMinutes: Int, val pickupCount: Int)

@Dao
interface UsageDao {
    @Query("SELECT * FROM usage_log WHERE date = :date AND packageName = :packageName LIMIT 1")
    suspend fun getForToday(date: String, packageName: String): UsageLog?

    // @Upsert em vez de @Insert(REPLACE): com chave composta (date, packageName),
    // o REPLACE apaga a linha e insere outra — o que zera qualquer coluna que a
    // nova linha não trouxer. O @Upsert atualiza a linha existente no lugar.
    @Upsert
    suspend fun upsert(log: UsageLog)

    @Query("SELECT * FROM usage_log WHERE date = :date")
    fun observeForDate(date: String): Flow<List<UsageLog>>

    // Linhas cruas (uma por app por dia) a partir de uma data — é o que o
    // StreakCalculator precisa, já que ele compara o uso de CADA app com o
    // limite dele. observeDailyTotals não serve: ela soma tudo e perde o
    // packageName.
    @Query("SELECT * FROM usage_log WHERE date >= :since ORDER BY date ASC")
    fun observeSince(since: String): Flow<List<UsageLog>>

    @Query(
        """
        SELECT date, SUM(totalMinutes) as totalMinutes, SUM(pickupCount) as pickupCount
        FROM usage_log
        WHERE date >= :startDate
        GROUP BY date
        ORDER BY date ASC
        """
    )
    fun observeDailyTotals(startDate: String): Flow<List<DailyUsageTotal>>
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM user_settings")
    fun observeAll(): Flow<List<UserSettings>>

    @Query("SELECT * FROM user_settings WHERE packageName = :packageName LIMIT 1")
    suspend fun get(packageName: String): UserSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: UserSettings)
}

@Dao
interface StreakStateDao {
    @Query("SELECT * FROM streak_state WHERE id = $STREAK_STATE_ID LIMIT 1")
    fun observe(): Flow<StreakState?>

    @Query("SELECT * FROM streak_state WHERE id = $STREAK_STATE_ID LIMIT 1")
    suspend fun get(): StreakState?

    @Upsert
    suspend fun upsert(state: StreakState)
}

// ---- Database ----

/**
 * v1 -> v2: acrescenta user_settings.lastSurveyTimestamp (cooldown da survey).
 *
 * ADD COLUMN ... DEFAULT 0 preenche as linhas que já existem, então nenhum
 * dado de teste no aparelho é perdido — é por isso que vale escrever a
 * migration em vez de deixar o fallbackToDestructiveMigration apagar tudo.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE user_settings ADD COLUMN lastSurveyTimestamp INTEGER NOT NULL DEFAULT 0"
        )
    }
}

/**
 * v2 -> v3: cria a tabela streak_state (mecanismo de revive da streak).
 *
 * É um CREATE TABLE, não um ALTER: nenhuma tabela existente é tocada, então
 * usage_log, survey_response e user_settings ficam intactos. O texto do SQL
 * precisa bater EXATAMENTE com o que o Room gera para a entidade — foi
 * conferido contra o AppDatabase_Impl gerado na build.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `streak_state` (" +
                "`id` INTEGER NOT NULL, " +
                "`revivesAvailable` INTEGER NOT NULL DEFAULT 1, " +
                "`revivedDates` TEXT NOT NULL DEFAULT '', " +
                "`milestonesRewarded` INTEGER NOT NULL DEFAULT 0, " +
                "PRIMARY KEY(`id`))"
        )
    }
}

@Database(
    entities = [
        SurveyResponse::class,
        UsageLog::class,
        UserSettings::class,
        StreakState::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun surveyDao(): SurveyDao
    abstract fun usageDao(): UsageDao
    abstract fun settingsDao(): SettingsDao
    abstract fun streakStateDao(): StreakStateDao
}
