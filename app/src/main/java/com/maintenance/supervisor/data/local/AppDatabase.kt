package com.maintenance.supervisor.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE reports ADD COLUMN isLocked INTEGER NOT NULL DEFAULT 0")
    }
}

@Database(
    entities = [UserEntity::class, FactoryEntity::class, AssetEntity::class, ChecklistTemplateEntity::class,
        ChecklistSectionEntity::class, ChecklistItemEntity::class, CurrentMaintenanceEntity::class,
        MaintenanceReportEntity::class, MaintenanceAnswerEntity::class, SyncQueueEntity::class, MetadataEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() { abstract fun maintenanceDao(): MaintenanceDao }
