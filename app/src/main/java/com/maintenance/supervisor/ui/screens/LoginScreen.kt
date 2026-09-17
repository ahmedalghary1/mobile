package com.maintenance.supervisor.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maintenance.supervisor.ui.LoginViewModel

@Composable fun LoginScreen(vm: LoginViewModel, onDone: () -> Unit) {
    val state by vm.state.collectAsState()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Outlined.Build, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(20.dp)); Text("إدارة الصيانة", fontSize = 30.sp, color = MaterialTheme.colorScheme.primary)
            Text("دخول مشرف الصيانة", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(36.dp))
            OutlinedTextField(state.phone, vm::phone, Modifier.fillMaxWidth().heightIn(min = 60.dp), label = { Text("رقم الهاتف") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), enabled = !state.loading)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(state.password, vm::password, Modifier.fillMaxWidth().heightIn(min = 60.dp), label = { Text("كلمة المرور") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), enabled = !state.loading)
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) }
            Spacer(Modifier.height(24.dp))
            Button(vm::submit, Modifier.fillMaxWidth().height(60.dp), enabled = !state.loading) {
                if (state.loading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Text("تسجيل الدخول", fontSize = 19.sp)
            }
        }
    }
}
