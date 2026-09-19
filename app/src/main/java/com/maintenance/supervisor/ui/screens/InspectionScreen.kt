package com.maintenance.supervisor.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maintenance.supervisor.domain.model.ChecklistItem
import com.maintenance.supervisor.ui.InspectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun InspectionScreen(vm: InspectionViewModel, onBack: () -> Unit, onSaved: () -> Unit) {
    val state by vm.state.collectAsState(); val daily = state.home?.daily; val report = daily?.report
    val answerMap = report?.answers?.associateBy { it.checklistItemId }.orEmpty()
    val isLocked = report?.isLocked == true
    val totalItems = daily?.sections?.sumOf { it.items.size } ?: 0
    val checkedItems = answerMap.values.count { it.checked }
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
        TopAppBar(title = { Column {
            Text("فحص ${daily?.asset?.code.orEmpty()}", fontWeight = FontWeight.Bold)
            Text(
                if (isLocked) "تقرير مقفل - للعرض فقط" else "يُحفظ كل تغيير تلقائيًا",
                fontSize = 13.sp,
                color = if (isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        } },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "رجوع") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface))
    }, bottomBar = {
        if (!isLocked) {
            Surface(shadowElevation = 10.dp, color = MaterialTheme.colorScheme.surface) { Button(onClick = { vm.complete(onSaved) }, enabled = report != null && !state.saving,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(58.dp)) {
                if (state.saving) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                else Icon(Icons.Outlined.CheckCircle, null)
                Spacer(Modifier.width(8.dp)); Text(if (state.saving) "جاري الحفظ" else "حفظ وإنهاء الفحص", fontSize = 20.sp)
            } }
        } else {
            Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(Modifier.fillMaxWidth().padding(16.dp).height(62.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text("هذا التقرير مقفل ولا يمكن تعديله", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }) { padding ->
        if (daily == null || report == null) Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "progress") {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column { Text("تقدم الفحص", style = MaterialTheme.typography.titleMedium); Text("يمكنك إضافة ملاحظة لأي بند", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .72f)) }
                            Text("$checkedItems / $totalItems", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(11.dp)); LinearProgressIndicator(progress = { if (totalItems == 0) 0f else checkedItems.toFloat() / totalItems }, Modifier.fillMaxWidth().height(7.dp), strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
                    }
                }
            }
            state.error?.let { message ->
                item(key = "save-error") { Text(message, color = MaterialTheme.colorScheme.error) }
            }
            daily.sections.forEach { section ->
                item(key = "s${section.id}") {
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(section.title, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.extraSmall) { Text("${section.items.count { answerMap[it.id]?.checked == true }} / ${section.items.size}", Modifier.padding(horizontal = 9.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium) }
                    }
                }
                val uiItems = section.items.map { it to answerMap[it.id] }
                items(uiItems, key = { it.first.id }) { (item, answer) ->
                    ChecklistRow(item, answer?.checked == true, answer?.note.orEmpty(), isLocked) { checked, note -> vm.update(item.id, checked, note) }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable private fun ChecklistRow(item: ChecklistItem, initialChecked: Boolean, initialNote: String, isLocked: Boolean, onChange: (Boolean, String) -> Unit) {
    var checked by remember(initialChecked) { mutableStateOf(initialChecked) }
    var note by remember(initialNote) { mutableStateOf(initialNote) }

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (checked) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .55f) else MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (checked) MaterialTheme.colorScheme.secondary.copy(alpha = .28f) else MaterialTheme.colorScheme.outline.copy(alpha = .65f))
    ) { Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).then(
            if (isLocked) Modifier else Modifier.clickable {
                val newVal = !checked; checked = newVal; onChange(newVal, note)
            }
        ), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked, onCheckedChange = if (isLocked) null else { newVal -> checked = newVal; onChange(newVal, note) }, Modifier.size(48.dp), enabled = !isLocked)
            Spacer(Modifier.width(8.dp)); Text(item.text, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal)
        }
        OutlinedTextField(note, onValueChange = if (isLocked) { _ -> } else { newNote -> note = newNote; onChange(checked, newNote) },
            Modifier.fillMaxWidth(), label = { Text("ملاحظة اختيارية") }, leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Notes, null) }, minLines = 1, maxLines = 3, readOnly = isLocked)
    } }
}
