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

@Database(
    entities = [SurveyResponse::class, UsageLog::class, UserSettings::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun surveyDao(): SurveyDao
    abstract fun usageDao(): UsageDao
    abstract fun settingsDao(): SettingsDao
}
