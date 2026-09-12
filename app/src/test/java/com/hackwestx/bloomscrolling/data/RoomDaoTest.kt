package com.hackwestx.bloomscrolling.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// targetSdk is 37, newer than what Robolectric 4.16.1 ships shadows for
// (max 36) — pin these tests to a supported API level.
@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class RoomDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var usageDao: UsageDao
    private lateinit var surveyDao: SurveyDao
    private lateinit var settingsDao: SettingsDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        usageDao = db.usageDao()
        surveyDao = db.surveyDao()
        settingsDao = db.settingsDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `usage upsert replaces the row for the same date and package instead of duplicating`() = runTest {
        usageDao.upsert(UsageLog(date = "2026-09-12", packageName = "com.instagram.android", totalMinutes = 10, pickupCount = 3))
        usageDao.upsert(UsageLog(date = "2026-09-12", packageName = "com.instagram.android", totalMinutes = 25, pickupCount = 7))

        val rows = usageDao.observeForDate("2026-09-12").first()
        assertEquals(1, rows.size)
        assertEquals(25, rows.single().totalMinutes)
        assertEquals(7, rows.single().pickupCount)
    }

    @Test
    fun `usage upsert for a different package on the same date keeps both rows`() = runTest {
        usageDao.upsert(UsageLog("2026-09-12", "com.instagram.android", 10, 3))
        usageDao.upsert(UsageLog("2026-09-12", "com.tiktok.android", 5, 1))

        val rows = usageDao.observeForDate("2026-09-12").first()
        assertEquals(2, rows.size)
    }

    @Test
    fun `getForToday returns null when no row exists yet`() = runTest {
        val result = usageDao.getForToday("2026-09-12", "com.instagram.android")
        assertNull(result)
    }

    @Test
    fun `getForToday returns the matching row`() = runTest {
        usageDao.upsert(UsageLog("2026-09-12", "com.instagram.android", 42, 9))
        val result = usageDao.getForToday("2026-09-12", "com.instagram.android")
        assertEquals(42, result?.totalMinutes)
    }

    @Test
    fun `survey reasonBreakdown groups counts by reason`() = runTest {
        surveyDao.insert(SurveyResponse(timestamp = 1L, packageName = "a", reason = "bored"))
        surveyDao.insert(SurveyResponse(timestamp = 2L, packageName = "a", reason = "bored"))
        surveyDao.insert(SurveyResponse(timestamp = 3L, packageName = "a", reason = "habit"))

        val breakdown = surveyDao.reasonBreakdown().associate { it.reason to it.count }
        assertEquals(2, breakdown["bored"])
        assertEquals(1, breakdown["habit"])
    }

    @Test
    fun `survey observeAll orders by timestamp descending`() = runTest {
        surveyDao.insert(SurveyResponse(timestamp = 1L, packageName = "a", reason = "bored"))
        surveyDao.insert(SurveyResponse(timestamp = 3L, packageName = "a", reason = "habit"))
        surveyDao.insert(SurveyResponse(timestamp = 2L, packageName = "a", reason = "messaging"))

        val all = surveyDao.observeAll().first()
        assertEquals(listOf(3L, 2L, 1L), all.map { it.timestamp })
    }

    @Test
    fun `settings upsert replaces the existing row for the same package`() = runTest {
        settingsDao.upsert(UserSettings("com.instagram.android", dailyLimitMinutes = 30, isBlocked = true))
        settingsDao.upsert(UserSettings("com.instagram.android", dailyLimitMinutes = 60, isBlocked = false))

        val settings = settingsDao.get("com.instagram.android")
        assertEquals(60, settings?.dailyLimitMinutes)
        assertEquals(false, settings?.isBlocked)
    }
}
