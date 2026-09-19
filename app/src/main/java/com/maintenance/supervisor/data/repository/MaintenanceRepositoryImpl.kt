package com.maintenance.supervisor.data.repository

import androidx.room.withTransaction
import com.maintenance.supervisor.data.local.*
import com.maintenance.supervisor.data.remote.*
import com.maintenance.supervisor.domain.model.*
import com.maintenance.supervisor.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaintenanceRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val api: MaintenanceApi,
    private val clock: Clock
) : MaintenanceRepository {
    private val dao = db.maintenanceDao()
    private val cairo = ZoneId.of("Africa/Cairo")

    override fun observeHome(): Flow<HomeSnapshot> = combine(
        dao.observeUser(), dao.observeFactory(), dao.observeAssets(), dao.observeCurrent(), dao.observeReports(), dao.observeMetadata("last_sync")
    ) { values ->
        val user = values[0] as UserEntity?
        val factory = values[1] as FactoryEntity?
        val assets = values[2] as List<AssetEntity>
        val current = values[3] as CurrentMaintenanceEntity?
        val reports = values[4] as List<ReportWithAnswers>
        val lastSync = values[5] as String?
        val today = LocalDate.now(clock.withZone(cairo))

        // Find an open draft or today's completed report
        val open = reports.firstOrNull { it.report.completedAtDevice == null }
        val todayCompleted = reports.firstOrNull { relation -> relation.report.completedAtDevice?.let {
            OffsetDateTime.parse(it).atZoneSameInstant(cairo).toLocalDate() == today
        } == true }
        // Also find a synced report for today (downloaded from server)
        val todaySynced = reports.firstOrNull { relation ->
            LocalDate.parse(relation.report.reportDate) == today && relation.report.syncStatus == SyncStatus.SYNCED.name
        }
        val chosenReport = open ?: todayCompleted ?: todaySynced

        val baseIndex = assets.indexOfFirst { it.id == current?.assetId }
        val advances = reports.count { it.report.completedAtDevice != null && LocalDate.parse(it.report.reportDate) < today && current != null && !LocalDate.parse(it.report.reportDate).isBefore(LocalDate.parse(current.reportDate)) }
        val asset = chosenReport?.let { r -> assets.firstOrNull { it.id == r.report.assetId } }
            ?: current?.takeIf { baseIndex >= 0 }?.let { MaintenanceCycle.assetAt(assets, baseIndex, advances) }
        val daily = asset?.let {
            val sections = it.checklistTemplateId?.let { id -> dao.checklist(id) }.orEmpty().map { relation ->
                ChecklistSection(relation.section.id, relation.section.title, relation.section.sequence,
                    relation.items.sortedBy { item -> item.sequence }.map { item -> ChecklistItem(item.id, item.sectionId, item.text, item.sequence) })
            }
            val total = current?.total?.takeIf { count -> count > 0 } ?: assets.size
            val position = current?.position?.let { start -> if (total > 0) ((start - 1 + advances).mod(total)) + 1 else start } ?: (assets.indexOf(it) + 1)
            DailyMaintenance(it.toDomain(), chosenReport?.report?.reportDate?.let(LocalDate::parse) ?: today,
                position, total, sections, chosenReport?.toDomain())
        }
        HomeSnapshot(user?.let { u -> factory?.let { u.toDomain(it) } }, factory?.toDomain(), daily, lastSync)
    }

    override suspend fun bootstrap(): AppResult<Unit> {
        return try {
        // Don't skip bootstrap if there's a pending local draft — we still need fresh data
        // But we must preserve the local draft report
        val localDraft = dao.openReport()
        val value = api.bootstrap()
        db.withTransaction {
            dao.clearUsers(); dao.clearFactories()
            dao.upsertFactory(FactoryEntity(value.factory.id, value.factory.name, value.factory.code))
            dao.upsertUser(UserEntity(value.user.id, value.user.phone, value.user.role, value.user.factory.id))
            dao.clearItems(); dao.clearSections(); dao.clearTemplates(); dao.clearAssets()
            dao.upsertTemplates(value.checklistTemplates.map { ChecklistTemplateEntity(it.id, it.name) })
            dao.upsertSections(value.checklistTemplates.flatMap { t -> t.sections.map { ChecklistSectionEntity(it.id, t.id, it.title, it.sequence) } })
            dao.upsertItems(value.checklistTemplates.flatMap { it.sections }.flatMap { s -> s.items.map { ChecklistItemEntity(it.id, s.id, it.text, it.sequence) } })

            val templateCodes = value.checklistTemplates.associate { it.code to it.id }
            fun getTemplateId(type: String): Int? {
                val code = when (type) {
                    "REGULAR_MACHINE", "PRESS" -> "STANDARD"
                    "SPRING_MACHINE" -> "SPRING"
                    else -> "STANDARD"
                }
                return templateCodes[code]
            }

            dao.upsertAssets(value.assets.map { AssetEntity(it.id, it.code, it.name, it.typeName, it.sequence, getTemplateId(it.assetType), it.isActive) })

            // Extract current asset info from the bootstrap response
            val currentAssetId = value.asset?.id
            val currentReportDate = value.serverDate
            if (currentAssetId != null && currentReportDate != null) {
                dao.upsertCurrent(CurrentMaintenanceEntity(assetId = currentAssetId, reportDate = currentReportDate, position = 1, total = value.assets.size))
            }

            // Save server report for today if it exists and there's no local draft for same day
            val serverReport = value.report
            if (serverReport != null && (localDraft == null || localDraft.reportDate != serverReport.reportDate)) {
                val today = LocalDate.now(clock.withZone(cairo))
                val reportDate = LocalDate.parse(serverReport.reportDate)
                val isLocked = serverReport.isLocked || reportDate < today

                // Clean up old synced reports to avoid duplicates
                dao.clearSyncedReports()

                dao.upsertReport(MaintenanceReportEntity(
                    clientReportId = serverReport.clientReportId,
                    ownerUserId = value.user.id,
                    serverId = serverReport.id,
                    assetId = serverReport.asset?.id ?: currentAssetId ?: 0,
                    reportDate = serverReport.reportDate,
                    startedAtDevice = serverReport.startedAtDevice,
                    completedAtDevice = serverReport.completedAtDevice,
                    lastModifiedAtDevice = serverReport.lastModifiedAtDevice,
                    syncStatus = SyncStatus.SYNCED.name,
                    isLocked = isLocked
                ))
                // Save the answers from server report
                if (serverReport.answers.isNotEmpty()) {
                    dao.upsertAnswers(serverReport.answers.map {
                        MaintenanceAnswerEntity(serverReport.clientReportId, it.checklistItemId, it.checked, it.note)
                    })
                }
            }

            dao.putMetadata(MetadataEntity("last_sync", OffsetDateTime.now(clock).toString()))
        }
        AppResult.Success(Unit)
    } catch (t: Throwable) { AppResult.Error("تعذر تحميل بيانات المصنع. يمكنك متابعة العمل بالبيانات المحفوظة.", t) }

    }

    override suspend fun startOrLoadToday(): AppResult<MaintenanceReport> {
        return try {
        val today = LocalDate.now(clock.withZone(cairo))
        val todayStr = today.toString()

        // First: check if there's already a report for today (draft or synced)
        val existingReport = dao.todayReport(todayStr)
        if (existingReport != null) {
            val report = existingReport.toDomain()
            // If it's a locked server report, user can view but not edit
            return AppResult.Success(report)
        }

        // No report for today — create a new draft
        val snapshot = observeHome().mapLatest { it.daily }.firstNonNull()
        if (snapshot.sections.isEmpty() || snapshot.sections.all { it.items.isEmpty() }) {
            return AppResult.Error("قائمة الفحص غير متاحة لهذه الماكينة. قم بالمزامنة ثم أعد المحاولة.")
        }
        val now = OffsetDateTime.now(clock)
        val ownerId = observeHome().mapLatest { it.user?.id }.firstNonNull()
        val report = MaintenanceReportEntity(UUID.randomUUID().toString(), ownerId, null, snapshot.asset.id, todayStr, now.toString(), null, now.toString(), SyncStatus.LOCAL_DRAFT.name)
        val answers = snapshot.sections.flatMap { it.items }.map { MaintenanceAnswerEntity(report.clientReportId, it.id, false, "") }
        db.withTransaction { dao.upsertReport(report); dao.upsertAnswers(answers) }
        AppResult.Success(dao.report(report.clientReportId)!!.toDomain())
    } catch (t: Throwable) { AppResult.Error("لا توجد بيانات صيانة جاهزة. قم بالمزامنة أولًا.", t) }

    }

    override suspend fun saveAnswer(reportId: String, answer: MaintenanceAnswer) {
        db.withTransaction {
            val existing = dao.report(reportId)?.report ?: return@withTransaction
            // Don't allow saving if report is locked
            if (existing.isLocked) return@withTransaction
            dao.upsertAnswers(listOf(MaintenanceAnswerEntity(reportId, answer.checklistItemId, answer.checked, answer.note)))
            dao.upsertReport(existing.copy(lastModifiedAtDevice = OffsetDateTime.now(clock).toString(), syncStatus = SyncStatus.LOCAL_DRAFT.name))
        }
    }

    override suspend fun completeReport(reportId: String): AppResult<Unit> = try {
        db.withTransaction {
            val report = dao.report(reportId)?.report ?: error("missing report")
            // Don't allow completing a locked report
            if (report.isLocked) error("التقرير مقفل ولا يمكن تعديله.")
            val now = OffsetDateTime.now(clock).toString()
            dao.upsertReport(report.copy(completedAtDevice = now, lastModifiedAtDevice = now, syncStatus = SyncStatus.PENDING_SYNC.name))
            dao.enqueue(SyncQueueEntity(clientReportId = reportId, enqueuedAt = now))
        }
        AppResult.Success(Unit)
    } catch (t: Throwable) { AppResult.Error("تعذر حفظ التقرير على الهاتف.", t) }

    override suspend fun sync(): AppResult<Unit> = try {
        val pending = dao.pendingReports()
        if (pending.isNotEmpty()) {
            pending.forEach { local ->
                dao.setReportStatus(local.report.clientReportId, SyncStatus.SYNCING.name)
                try {
                    val response = api.syncReports(BatchSyncRequest(listOf(local.toInput())))
                    val result = response.reports.firstOrNull { it.clientReportId == local.report.clientReportId }
                    when {
                        result == null -> dao.setReportStatus(local.report.clientReportId, SyncStatus.SYNC_ERROR.name, error = "لم يؤكد الخادم استلام التقرير")
                        result.status.lowercase() in setOf("synced", "already_synced", "success") -> {
                            dao.setReportStatus(local.report.clientReportId, SyncStatus.SYNCED.name, result.id)
                            dao.dequeue(local.report.clientReportId)
                        }
                        result.status.lowercase() in setOf("conflict") -> {
                            // Conflict means server has different data — mark synced to avoid retry loop
                            dao.setReportStatus(local.report.clientReportId, SyncStatus.SYNCED.name, result.id)
                            dao.dequeue(local.report.clientReportId)
                        }
                        result.status.lowercase() == "rejected" -> {
                            // Rejected — keep the error so user knows
                            dao.setReportStatus(local.report.clientReportId, SyncStatus.SYNC_ERROR.name, error = result.detail ?: "تم رفض التقرير من الخادم")
                            dao.dequeue(local.report.clientReportId)
                        }
                        else -> dao.setReportStatus(local.report.clientReportId, SyncStatus.SYNC_ERROR.name, error = result.detail ?: result.status)
                    }
                } catch (t: retrofit2.HttpException) {
                    if (t.code() == 409) {
                        dao.setReportStatus(local.report.clientReportId, SyncStatus.SYNCED.name, null)
                        dao.dequeue(local.report.clientReportId)
                    } else if (t.code() == 400) {
                        val errorBody = t.response()?.errorBody()?.string() ?: "بيانات غير صالحة"
                        dao.setReportStatus(local.report.clientReportId, SyncStatus.SYNC_ERROR.name, error = "خطأ في البيانات: $errorBody")
                        dao.dequeue(local.report.clientReportId)
                    } else {
                        dao.setReportStatus(local.report.clientReportId, SyncStatus.SYNC_ERROR.name, error = t.javaClass.simpleName)
                    }
                } catch (t: kotlinx.serialization.SerializationException) {
                    dao.setReportStatus(local.report.clientReportId, SyncStatus.SYNC_ERROR.name, error = "خطأ في تحليل الاستجابة")
                } catch (t: Throwable) {
                    dao.setReportStatus(local.report.clientReportId, SyncStatus.SYNC_ERROR.name, error = t.javaClass.simpleName)
                }
            }
        }
        when (val pulled = bootstrap()) { is AppResult.Error -> pulled; is AppResult.Success -> AppResult.Success(Unit) }
    } catch (t: Throwable) {
        AppResult.Error("تعذر الإرسال الآن، وسيتم المحاولة تلقائيًا.", t)
    }

    private suspend fun <T : Any> Flow<T?>.firstNonNull(): T = this.first { it != null }!!
}

private fun FactoryEntity.toDomain() = Factory(id, name, code)
private fun UserEntity.toDomain(factory: FactoryEntity) = User(id, phone, role, factory.toDomain())
private fun AssetEntity.toDomain() = Asset(id, code, name, typeName, sequence)
private fun ReportWithAnswers.toDomain(): MaintenanceReport {
    val today = LocalDate.now()
    val reportDate = LocalDate.parse(report.reportDate)
    // Report is locked if server says so, or if the report date is in the past
    val locked = report.isLocked || reportDate < today
    return MaintenanceReport(
        report.clientReportId, report.serverId, report.assetId,
        reportDate,
        OffsetDateTime.parse(report.startedAtDevice),
        report.completedAtDevice?.let(OffsetDateTime::parse),
        OffsetDateTime.parse(report.lastModifiedAtDevice),
        SyncStatus.valueOf(report.syncStatus),
        answers.map { MaintenanceAnswer(it.checklistItemId, it.checked, it.note) },
        locked
    )
}
private fun ReportWithAnswers.toInput() = ReportInput(report.clientReportId, report.assetId, report.reportDate, report.startedAtDevice, requireNotNull(report.completedAtDevice), report.lastModifiedAtDevice, answers.map { AnswerInput(it.checklistItemId, it.checked, it.note) })
