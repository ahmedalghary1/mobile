package com.maintenance.supervisor.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
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
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
        TopAppBar(title = { Column { Text("فحص ${daily?.asset?.code.orEmpty()}", fontWeight = FontWeight.Bold); Text("يُحفظ كل تغيير تلقائيًا", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "رجوع") } })
    }, bottomBar = {
        Surface(shadowElevation = 8.dp) { Button(onClick = { vm.complete(onSaved) }, enabled = report != null,
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(62.dp)) { Icon(Icons.Outlined.CheckCircle, null); Spacer(Modifier.width(8.dp)); Text("حفظ وإنهاء الفحص", fontSize = 20.sp) } }
    }) { padding ->
        if (daily == null || report == null) Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            daily.sections.forEach { section ->
                item(key = "s${section.id}") { Text(section.title, color = MaterialTheme.colorScheme.primary, fontSize = 23.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) }
                val uiItems = section.items.map { it to answerMap[it.id] }
                items(uiItems, key = { it.first.id }) { (item, answer) ->
                    ChecklistRow(item, answer?.checked == true, answer?.note.orEmpty()) { checked, note -> vm.update(item.id, checked, note) }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable private fun ChecklistRow(item: ChecklistItem, initialChecked: Boolean, initialNote: String, onChange: (Boolean, String) -> Unit) {
    var checked by remember(initialChecked) { mutableStateOf(initialChecked) }
    var note by remember(initialNote) { mutableStateOf(initialNote) }
    
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { 
            val newVal = !checked; checked = newVal; onChange(newVal, note) 
        }, verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked, { newVal -> checked = newVal; onChange(newVal, note) }, Modifier.size(48.dp))
            Spacer(Modifier.width(8.dp)); Text(item.text, Modifier.weight(1f), fontSize = 18.sp, lineHeight = 27.sp)
        }
        OutlinedTextField(note, { newNote -> note = newNote; onChange(checked, newNote) }, Modifier.fillMaxWidth(), label = { Text("ملاحظة - اختياري") }, minLines = 1, maxLines = 3)
    } }
}

