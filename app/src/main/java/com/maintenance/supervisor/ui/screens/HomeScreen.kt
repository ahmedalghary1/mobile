package com.maintenance.supervisor.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maintenance.supervisor.domain.model.SyncStatus
import com.maintenance.supervisor.ui.HomeViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun HomeScreen(vm: HomeViewModel, onInspect: () -> Unit, onLogout: () -> Unit) {
    val state by vm.state.collectAsState(); val daily = state.snapshot.daily
    var menu by remember { mutableStateOf(false) }
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
        TopAppBar(title = { Text("إدارة الصيانة", fontWeight = FontWeight.Bold) }, actions = {
            IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.AccountCircle, "الحساب") }
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem({ Text("مزامنة البيانات") }, { menu = false; vm.refresh() }, leadingIcon = { Icon(Icons.Outlined.Sync, null) })
                DropdownMenuItem({ Text("تسجيل الخروج") }, { menu = false; vm.logout(onLogout) }, leadingIcon = { Icon(Icons.Outlined.Logout, null) })
            }
        })
    }) { padding ->
        Column(Modifier.padding(padding).padding(20.dp).fillMaxSize()) {
            Text(state.snapshot.factory?.name ?: "بيانات المصنع غير متاحة", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (state.refreshing) Icons.Outlined.Sync else if (state.connected) Icons.Outlined.CloudDone else Icons.Outlined.CloudOff, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(8.dp)); Text(if (state.refreshing) "جاري المزامنة" else if (!state.connected) "غير متصل - العمل محفوظ" else syncLabel(daily?.report?.status), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            Spacer(Modifier.height(22.dp))
            if (daily == null) {
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(24.dp)) {
                    Icon(if (state.snapshot.factory == null) Icons.Outlined.CloudOff else Icons.Outlined.CheckCircle, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(16.dp)); Text(if (state.snapshot.factory == null) "يجب الاتصال بالإنترنت مرة واحدة لتحميل بيانات المصنع." else "لا توجد ماكينة محددة للصيانة حاليًا.", fontSize = 19.sp)
                    Spacer(Modifier.height(20.dp)); Button(vm::refresh, Modifier.fillMaxWidth().height(56.dp)) { Text("إعادة المحاولة") }
                } }
            } else {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(3.dp)) {
                    Column(Modifier.padding(24.dp)) {
                        Text(if (daily.report?.completedAt != null) "تم تسجيل صيانة اليوم" else "صيانة اليوم", color = MaterialTheme.colorScheme.secondary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(18.dp)); Text(daily.asset.typeName.ifBlank { daily.asset.name }, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 18.sp)
                        Text(daily.asset.code, color = MaterialTheme.colorScheme.primary, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                        Text(daily.reportDate.format(DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy", Locale("ar", "EG"))), fontSize = 17.sp)
                        Text("الترتيب ${daily.position} من ${daily.total}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(26.dp))
                        Button(onClick = { vm.start(onInspect) }, Modifier.fillMaxWidth().height(64.dp)) {
                            Icon(if (daily.report == null) Icons.Outlined.PlayArrow else Icons.Outlined.Edit, null); Spacer(Modifier.width(8.dp))
                            Text(when { daily.report?.completedAt != null -> "تعديل تقرير اليوم"; daily.report != null -> "استكمال الفحص"; else -> "بدء الفحص" }, fontSize = 20.sp)
                        }
                    }
                }
                state.snapshot.lastSync?.let { Text("آخر مزامنة محفوظة: ${it.take(16).replace('T', ' ')}", Modifier.padding(top = 14.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

private fun syncLabel(status: SyncStatus?) = when (status) {
    SyncStatus.SYNCED -> "تمت المزامنة"
    SyncStatus.PENDING_SYNC, SyncStatus.LOCAL_DRAFT -> "محفوظ على الهاتف"
    SyncStatus.SYNCING -> "جاري المزامنة"
    SyncStatus.SYNC_ERROR -> "سيتم الإرسال عند توفر الإنترنت"
    null -> "جاهز للعمل دون إنترنت"
}

