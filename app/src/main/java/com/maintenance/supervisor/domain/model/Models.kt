package com.maintenance.supervisor.domain.model

import java.time.LocalDate
import java.time.OffsetDateTime

enum class SyncStatus { LOCAL_DRAFT, PENDING_SYNC, SYNCING, SYNCED, SYNC_ERROR }

data class Factory(val id: Int, val name: String, val code: String)
data class User(val id: Int, val phone: String, val role: String, val factory: Factory)
data class Asset(val id: Int, val code: String, val name: String, val typeName: String, val sequence: Int)
data class ChecklistItem(val id: Int, val sectionId: Int, val text: String, val sequence: Int)
data class ChecklistSection(val id: Int, val title: String, val sequence: Int, val items: List<ChecklistItem>)
data class DailyMaintenance(
    val asset: Asset,
    val reportDate: LocalDate,
    val position: Int,
    val total: Int,
    val sections: List<ChecklistSection>,
    val report: MaintenanceReport? = null
)
data class MaintenanceAnswer(val checklistItemId: Int, val checked: Boolean, val note: String)
data class MaintenanceReport(
    val clientReportId: String,
    val serverId: Int?,
    val assetId: Int,
    val reportDate: LocalDate,
    val startedAt: OffsetDateTime,
    val completedAt: OffsetDateTime?,
    val lastModifiedAt: OffsetDateTime,
    val status: SyncStatus,
    val answers: List<MaintenanceAnswer>,
    val isLocked: Boolean = false
)
