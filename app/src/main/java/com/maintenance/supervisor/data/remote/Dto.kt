package com.maintenance.supervisor.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class LoginRequest(val phone: String, val password: String)
@Serializable data class RefreshRequest(val refresh: String)
@Serializable data class TokenResponse(val access: String, val refresh: String? = null)
@Serializable data class FactoryDto(val id: Int, val name: String, val code: String = "")

@Serializable data class UserDto(val id: Int, val phone: String, val role: String, val factory: FactoryDto)
@Serializable data class AssetDto(
    val id: Int,
    @SerialName("asset_code") val code: String = "",
    val name: String = "",
    @SerialName("asset_type_display") val typeName: String = "",
    @SerialName("sequence_order") val sequence: Int = 0,
    @SerialName("checklist_template_id") val checklistTemplateId: Int? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("asset_type") val assetType: String = ""
)
@Serializable data class ChecklistItemDto(val id: Int, @SerialName("section_id") val sectionId: Int = 0, val text: String, @SerialName("sequence_order") val sequence: Int = 0)
@Serializable data class ChecklistSectionDto(val id: Int, @SerialName("template_id") val templateId: Int = 0, @SerialName("name") val title: String = "", @SerialName("sequence_order") val sequence: Int = 0, val items: List<ChecklistItemDto> = emptyList())
@Serializable data class ChecklistTemplateDto(val id: Int, val name: String = "", val code: String = "", val sections: List<ChecklistSectionDto> = emptyList())
@Serializable data class CurrentMaintenanceDto(
    @SerialName("asset_id") val assetId: Int,
    @SerialName("report_date") val reportDate: String,
    val position: Int = 1,
    val total: Int = 1
)
@Serializable data class BootstrapDto(
    val user: UserDto,
    val factory: FactoryDto = user.factory,
    @SerialName("active_assets") val assets: List<AssetDto> = emptyList(),
    @SerialName("checklist_templates") val checklistTemplates: List<ChecklistTemplateDto> = emptyList(),
    @SerialName("current_maintenance") val currentMaintenance: CurrentMaintenanceDto? = null,
    val asset: AssetDto? = null,
    @SerialName("server_date") val serverDate: String? = null
)
@Serializable data class AnswerInput(
    @SerialName("checklist_item_id") val checklistItemId: Int,
    val checked: Boolean,
    val note: String = ""
)
@Serializable data class ReportInput(
    @SerialName("client_report_id") val clientReportId: String,
    @SerialName("asset_id") val assetId: Int,
    @SerialName("report_date") val reportDate: String,
    @SerialName("started_at_device") val startedAtDevice: String,
    @SerialName("completed_at_device") val completedAtDevice: String,
    @SerialName("last_modified_at_device") val lastModifiedAtDevice: String,
    val answers: List<AnswerInput>
)
@Serializable data class BatchSyncRequest(val reports: List<ReportInput>)
@Serializable data class ReportResultDto(
    @SerialName("client_report_id") val clientReportId: String,
    @SerialName("report_id") val id: Int? = null,
    val status: String = "accepted",
    @SerialName("reason") val detail: String? = null
)
@Serializable data class BatchSyncResponse(@SerialName("results") val reports: List<ReportResultDto> = emptyList())
@Serializable data class ReportResponseDto(val id: Int, @SerialName("client_report_id") val clientReportId: String)
