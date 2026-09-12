package com.hackwestx.bloomscrolling.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
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
    val isBlocked: Boolean
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

@Dao
interface UsageDao {
    @Query("SELECT * FROM usage_log WHERE date = :date AND packageName = :packageName LIMIT 1")
    suspend fun getForToday(date: String, packageName: String): UsageLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: UsageLog)

    @Query("SELECT * FROM usage_log WHERE date = :date")
    fun observeForDate(date: String): Flow<List<UsageLog>>
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

@Database(
    entities = [SurveyResponse::class, UsageLog::class, UserSettings::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun surveyDao(): SurveyDao
    abstract fun usageDao(): UsageDao
    abstract fun settingsDao(): SettingsDao
}
