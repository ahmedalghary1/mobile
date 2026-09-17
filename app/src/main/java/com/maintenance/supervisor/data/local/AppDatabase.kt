package com.maintenance.supervisor.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [UserEntity::class, FactoryEntity::class, AssetEntity::class, ChecklistTemplateEntity::class,
        ChecklistSectionEntity::class, ChecklistItemEntity::class, CurrentMaintenanceEntity::class,
        MaintenanceReportEntity::class, MaintenanceAnswerEntity::class, SyncQueueEntity::class, MetadataEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() { abstract fun maintenanceDao(): MaintenanceDao }
