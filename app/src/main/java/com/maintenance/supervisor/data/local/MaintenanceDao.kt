package com.maintenance.supervisor.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertFactory(value: FactoryEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertUser(value: UserEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAssets(values: List<AssetEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertTemplates(values: List<ChecklistTemplateEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertSections(values: List<ChecklistSectionEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertItems(values: List<ChecklistItemEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertCurrent(value: CurrentMaintenanceEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertReport(value: MaintenanceReportEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAnswers(values: List<MaintenanceAnswerEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun enqueue(value: SyncQueueEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putMetadata(value: MetadataEntity)

    @Query("SELECT * FROM users LIMIT 1") fun observeUser(): Flow<UserEntity?>
    @Query("SELECT * FROM factories LIMIT 1") fun observeFactory(): Flow<FactoryEntity?>
    @Query("SELECT * FROM assets WHERE active = 1 ORDER BY sequence") fun observeAssets(): Flow<List<AssetEntity>>
    @Query("SELECT * FROM current_maintenance WHERE singletonId = 1") fun observeCurrent(): Flow<CurrentMaintenanceEntity?>
    @Transaction @Query("SELECT * FROM reports WHERE ownerUserId = (SELECT id FROM users LIMIT 1) ORDER BY reportDate") fun observeReports(): Flow<List<ReportWithAnswers>>
    @Transaction @Query("SELECT * FROM reports WHERE clientReportId = :id") suspend fun report(id: String): ReportWithAnswers?
    @Transaction @Query("SELECT * FROM reports WHERE reportDate = :date AND ownerUserId = (SELECT id FROM users LIMIT 1) LIMIT 1") suspend fun reportForDate(date: String): ReportWithAnswers?
    @Query("SELECT * FROM reports WHERE syncStatus = 'LOCAL_DRAFT' AND ownerUserId = (SELECT id FROM users LIMIT 1) LIMIT 1") suspend fun openReport(): MaintenanceReportEntity?
    @Transaction @Query("SELECT * FROM checklist_sections WHERE templateId = :templateId ORDER BY sequence") suspend fun checklist(templateId: Int): List<SectionWithItems>
    @Transaction @Query("SELECT * FROM reports WHERE ownerUserId = (SELECT id FROM users LIMIT 1) AND syncStatus IN ('PENDING_SYNC','SYNCING','SYNC_ERROR') AND completedAtDevice IS NOT NULL ORDER BY reportDate ASC, completedAtDevice ASC") suspend fun pendingReports(): List<ReportWithAnswers>
    @Query("UPDATE reports SET syncStatus = :status, serverId = COALESCE(:serverId, serverId), lastError = :error WHERE clientReportId = :id") suspend fun setReportStatus(id: String, status: String, serverId: Int? = null, error: String? = null)
    @Query("DELETE FROM sync_queue WHERE clientReportId = :id") suspend fun dequeue(id: String)
    @Query("SELECT value FROM metadata WHERE `key` = :key") fun observeMetadata(key: String): Flow<String?>
    @Query("DELETE FROM assets") suspend fun clearAssets()
    @Query("DELETE FROM users") suspend fun clearUsers()
    @Query("DELETE FROM factories") suspend fun clearFactories()
    @Query("DELETE FROM checklist_templates") suspend fun clearTemplates()
    @Query("DELETE FROM checklist_sections") suspend fun clearSections()
    @Query("DELETE FROM checklist_items") suspend fun clearItems()
}
