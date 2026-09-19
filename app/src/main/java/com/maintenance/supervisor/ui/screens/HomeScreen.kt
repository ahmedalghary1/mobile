package com.maintenance.supervisor.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maintenance.supervisor.domain.model.DailyMaintenance
import com.maintenance.supervisor.domain.model.SyncStatus
import com.maintenance.supervisor.ui.HomeViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun HomeScreen(vm: HomeViewModel, onInspect: () -> Unit, onLogout: () -> Unit) {
    val state by vm.state.collectAsState()
    val daily = state.snapshot.daily
    var menu by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Column { Text("إدارة الصيانة", fontWeight = FontWeight.Bold); Text("لوحة مشرف الصيانة", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                actions = {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "القائمة") }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem({ Text("مزامنة البيانات") }, { menu = false; vm.refresh() }, leadingIcon = { Icon(Icons.Outlined.Sync, null) })
                        DropdownMenuItem({ Text("تسجيل الخروج") }, { menu = false; vm.logout(onLogout) }, leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Logout, null) })
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text("مرحبًا،", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(state.snapshot.factory?.name ?: "بيانات المصنع غير متاحة", style = MaterialTheme.typography.headlineSmall)
                }
            }
            item { SyncBanner(state.refreshing, state.connected, daily?.report?.status, daily?.report?.lastError, vm::refresh) }
            state.message?.let { message ->
                item {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(10.dp)); Text(message, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            item {
                if (daily == null) EmptyMaintenanceCard(state.snapshot.factory == null, vm::refresh)
                else DailyMaintenanceCard(daily) { vm.start(onInspect) }
            }
            state.snapshot.lastSync?.let { value ->
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.History, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(6.dp)); Text("آخر تحديث: ${value.take(16).replace('T', ' ')}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable private fun SyncBanner(refreshing: Boolean, connected: Boolean, status: SyncStatus?, error: String?, onRefresh: () -> Unit) {
    val isError = status == SyncStatus.SYNC_ERROR
    val container = when { isError -> MaterialTheme.colorScheme.errorContainer; !connected -> MaterialTheme.colorScheme.surfaceVariant; else -> MaterialTheme.colorScheme.secondaryContainer }
    val content = when { isError -> MaterialTheme.colorScheme.onErrorContainer; !connected -> MaterialTheme.colorScheme.onSurfaceVariant; else -> MaterialTheme.colorScheme.onSecondaryContainer }
    val text = when {
        refreshing -> "جاري تحديث البيانات..."
        !connected -> "أنت تعمل دون إنترنت — بياناتك محفوظة"
        else -> syncLabel(status, error)
    }
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.medium) {
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (refreshing) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = content)
            else Icon(if (!connected) Icons.Outlined.CloudOff else if (isError) Icons.Outlined.CloudSync else Icons.Outlined.CloudDone, null, Modifier.size(23.dp))
            Spacer(Modifier.width(10.dp)); Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = onRefresh, enabled = !refreshing) { Icon(Icons.Outlined.Refresh, "تحديث") }
        }
    }
}

@Composable private fun DailyMaintenanceCard(daily: DailyMaintenance, onOpen: () -> Unit) {
    val report = daily.report
    val locked = report?.isLocked == true
    val completed = report?.completedAt != null
    val progress = if (daily.total > 0) daily.position.toFloat() / daily.total else 0f
    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = if (completed) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                    Icon(if (completed) Icons.Outlined.TaskAlt else Icons.Outlined.BuildCircle, null, Modifier.padding(11.dp).size(28.dp), tint = if (completed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(when { locked -> "تقرير اليوم"; completed -> "تم إنجاز فحص اليوم"; report != null -> "فحص قيد التنفيذ"; else -> "مهمة اليوم" }, style = MaterialTheme.typography.titleMedium)
                    Text(daily.reportDate.format(DateTimeFormatter.ofPattern("EEEE، d MMMM", Locale.forLanguageTag("ar-EG"))), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (locked) Icon(Icons.Outlined.Lock, "مقفل", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(22.dp))
            Text(daily.asset.typeName.ifBlank { daily.asset.name }, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(daily.asset.code, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("تقدم دورة المصنع", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${daily.position} من ${daily.total}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(7.dp)); LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, Modifier.fillMaxWidth().height(7.dp), strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
            Spacer(Modifier.height(22.dp)); HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .55f)); Spacer(Modifier.height(16.dp))
            Button(onClick = onOpen, Modifier.fillMaxWidth().height(58.dp)) {
                Icon(when { locked -> Icons.Outlined.Visibility; report == null -> Icons.Outlined.PlayArrow; else -> Icons.Outlined.Edit }, null)
                Spacer(Modifier.width(8.dp)); Text(when { locked -> "عرض التقرير"; completed -> "مراجعة تقرير اليوم"; report != null -> "استكمال الفحص"; else -> "بدء الفحص الآن" })
            }
        }
    }
}

@Composable private fun EmptyMaintenanceCard(needsInternet: Boolean, onRefresh: () -> Unit) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.large) { Icon(if (needsInternet) Icons.Outlined.CloudOff else Icons.Outlined.EventAvailable, null, Modifier.padding(18.dp).size(34.dp), tint = MaterialTheme.colorScheme.secondary) }
        Spacer(Modifier.height(16.dp)); Text(if (needsInternet) "نحتاج اتصالًا أول مرة" else "لا توجد مهمة صيانة حاليًا", style = MaterialTheme.typography.titleLarge)
        Text(if (needsInternet) "اتصل بالإنترنت لتحميل بيانات المصنع وقائمة الفحص." else "ستظهر هنا الماكينة التالية فور تحديدها.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp)); OutlinedButton(onRefresh) { Icon(Icons.Outlined.Refresh, null); Spacer(Modifier.width(7.dp)); Text("إعادة المحاولة") }
    } }
}

private fun syncLabel(status: SyncStatus?, lastError: String?) = when (status) {
    SyncStatus.SYNCED -> "تمت مزامنة التقرير مع الخادم"
    SyncStatus.PENDING_SYNC -> "التقرير جاهز للإرسال"
    SyncStatus.LOCAL_DRAFT -> "مسودة محفوظة على الهاتف"
    SyncStatus.SYNCING -> "جاري إرسال التقرير..."
    SyncStatus.SYNC_ERROR -> lastError ?: "تعذر إرسال التقرير إلى الخادم"
    null -> "البيانات محدّثة وجاهزة للعمل"
}
