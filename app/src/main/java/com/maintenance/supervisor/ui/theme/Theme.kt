package com.maintenance.supervisor.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val Primary = Color(0xFF12304A)
val Secondary = Color(0xFF0F8B7B)
val Success = Color(0xFF2E7D32)
val Warning = Color(0xFFE6A700)
val Error = Color(0xFFC62828)
val Background = Color(0xFFF5F7F9)
val TextPrimary = Color(0xFF17212B)
val TextSecondary = Color(0xFF66717D)

private val colors = lightColorScheme(primary = Primary, secondary = Secondary, error = Error, background = Background,
    surface = Color.White, onPrimary = Color.White, onSecondary = Color.White, onBackground = TextPrimary, onSurface = TextPrimary)

@Composable fun MaintenanceTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = Typography(), shapes = Shapes(
        small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        medium = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        large = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)), content = content)
}
