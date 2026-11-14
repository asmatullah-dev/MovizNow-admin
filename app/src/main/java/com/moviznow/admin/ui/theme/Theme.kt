package com.moviznow.admin.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object MovizColors {
    val BgPrimary = Color(0xFF09090B)      // zinc-950
    val BgSurface = Color(0xFF18181B)      // zinc-900
    val BgElevated = Color(0xFF27272A)     // zinc-800
    val Border = Color(0xFF27272A)
    val BorderMuted = Color(0xFF3F3F46)
    val TextPrimary = Color(0xFFFAFAFA)
    val TextSecondary = Color(0xFFA1A1AA)
    val TextMuted = Color(0xFF71717A)

    val Emerald = Color(0xFF10B981)
    val EmeraldDark = Color(0xFF059669)
    val Amber = Color(0xFFF59E0B)
    val Sky = Color(0xFF0EA5E9)
    val Blue = Color(0xFF3B82F6)
    val Purple = Color(0xFFA855F7)
    val Pink = Color(0xFFEC4899)
    val Red = Color(0xFFEF4444)
    val Cyan = Color(0xFF06B6D4)
}

private val MovizDarkColorScheme = darkColorScheme(
    primary = MovizColors.Emerald,
    onPrimary = Color(0xFF022C22),
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = MovizColors.Sky,
    onSecondary = Color(0xFF082F49),
    background = MovizColors.BgPrimary,
    onBackground = MovizColors.TextPrimary,
    surface = MovizColors.BgSurface,
    onSurface = MovizColors.TextPrimary,
    surfaceVariant = MovizColors.BgElevated,
    onSurfaceVariant = MovizColors.TextSecondary,
    outline = MovizColors.BorderMuted,
    error = MovizColors.Red,
    onError = Color.White
)

@Composable
fun MovizNowAdminTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MovizDarkColorScheme,
        content = content
    )
}

@Composable
fun StatusBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (leadingIcon != null) {
                leadingIcon()
            }
            Text(
                text = text,
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

fun roleColor(role: String): Color = when (role.lowercase()) {
    "owner" -> MovizColors.Emerald
    "admin" -> MovizColors.Purple
    "vip" -> MovizColors.Amber
    "basic" -> MovizColors.Sky
    "selected_content" -> MovizColors.Pink
    "content_manager" -> MovizColors.Blue
    "user_manager" -> MovizColors.Cyan
    "manager" -> MovizColors.Emerald
    "trial" -> Color(0xFFEAB308)
    else -> MovizColors.TextSecondary
}

fun roleLabel(role: String): String = when (role.lowercase()) {
    "vip" -> "VIP"
    "basic" -> "Basic"
    "selected_content" -> "Selected Content"
    "content_manager" -> "Content Manager"
    "user_manager" -> "User Manager"
    "manager" -> "Manager"
    "owner" -> "Owner"
    "admin" -> "Admin"
    "trial" -> "Trial"
    else -> role.replaceFirstChar { it.uppercase() }.replace("_", " ")
}

fun statusColor(status: String): Color = when (status.lowercase()) {
    "active", "approved", "published", "added", "resolved" -> MovizColors.Emerald
    "expired", "declined", "rejected" -> MovizColors.Red
    "suspended" -> MovizColors.Pink
    "draft" -> MovizColors.TextSecondary
    else -> MovizColors.Amber
}
