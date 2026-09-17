package com.maintenance.supervisor.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {
    private lateinit var db: AppDatabase
    @Before fun setup() { db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).allowMainThreadQueries().build() }
    @After fun close() = db.close()
    @Test fun reportAndAnswersSurviveReload() = runTest {
        val dao = db.maintenanceDao()
        dao.upsertFactory(FactoryEntity(1, "مصنع", "F")); dao.upsertUser(UserEntity(1, "010", "MAINTENANCE_SUPERVISOR", 1))
        dao.upsertReport(MaintenanceReportEntity("id", 1, null, 9, "2026-09-17", "2026-09-17T08:00:00Z", null, "2026-09-17T08:01:00Z", "LOCAL_DRAFT"))
        dao.upsertAnswers(listOf(MaintenanceAnswerEntity("id", 2, true, "سليم")))
        val loaded = dao.report("id")!!
        assertEquals(true, loaded.answers.single().checked); assertEquals("سليم", loaded.answers.single().note)
    }
}
