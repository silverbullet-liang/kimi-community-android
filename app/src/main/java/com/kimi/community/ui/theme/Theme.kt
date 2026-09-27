package com.kimi.community.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** 可选主题色（按名称映射到品牌色板） */
object ThemeColors {
    const val KIMI = "kimi"       // 品牌红
    const val BLUE = "blue"
    const val GREEN = "green"
    const val PURPLE = "purple"
    const val ORANGE = "orange"
    const val TEAL = "teal"
    const val DYNAMIC = "dynamic" // Material You 系统壁纸色

    data class Option(val id: String, val label: String, val color: Color)

    val options: List<Option> = listOf(
        Option(KIMI, "Kimi 红", Color(0xFFD93025)),
        Option(BLUE, "星蓝", Color(0xFF1A73E8)),
        Option(GREEN, "翠绿", Color(0xFF188038)),
        Option(PURPLE, "罗兰紫", Color(0xFF7B1FA2)),
        Option(ORANGE, "暖橙", Color(0xFFF57C00)),
        Option(TEAL, "青碧", Color(0xFF00796B)),
        Option(DYNAMIC, "跟随壁纸", Color(0xFF607D8B))
    )
}

private fun themePrimary(colorId: String): Color = when (colorId) {
    ThemeColors.BLUE -> Color(0xFF1A73E8)
    ThemeColors.GREEN -> Color(0xFF188038)
    ThemeColors.PURPLE -> Color(0xFF7B1FA2)
    ThemeColors.ORANGE -> Color(0xFFF57C00)
    ThemeColors.TEAL -> Color(0xFF00796B)
    else -> Color(0xFFD93025) // kimi
}

private fun lightScheme(primary: Color): androidx.compose.material3.ColorScheme = lightColorScheme(
    primary = primary,
    onPrimary = Color.White,
    primaryContainer = primary.copy(alpha = 0.15f),
    onPrimaryContainer = primary.copy(alpha = 0.95f),
    secondary = primary.copy(alpha = 0.7f),
    onSecondary = Color.White,
    secondaryContainer = primary.copy(alpha = 0.12f),
    onSecondaryContainer = primary.copy(alpha = 0.85f),
    tertiary = Color(0xFF6B5F77),
    onTertiary = Color.White,
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF49454F),
    surfaceTint = primary,
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    scrim = Color.Black
)

private fun darkScheme(primary: Color): androidx.compose.material3.ColorScheme = darkColorScheme(
    primary = primary,
    onPrimary = Color.White,
    primaryContainer = primary.copy(alpha = 0.35f),
    onPrimaryContainer = Color.White,
    secondary = primary.copy(alpha = 0.8f),
    onSecondary = Color.White,
    secondaryContainer = primary.copy(alpha = 0.25f),
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFFD0BCFF),
    onTertiary = Color(0xFF381E72),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF2D2D2D),
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceTint = primary,
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF313033),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    scrim = Color.Black
)

/**
 * 主题封装。
 * darkMode: 0=跟随系统, 1=浅色, 2=深色
 * themeColor: 主题色 ID（ThemeColors），"dynamic" 时使用系统动态壁纸色（Material You）
 */
@Composable
fun KimiCommunityTheme(
    darkMode: Int = 0,
    themeColor: String = ThemeColors.KIMI,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val useDark = when (darkMode) {
        1 -> false
        2 -> true
        else -> systemDark
    }

    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && themeColor == ThemeColors.DYNAMIC -> {
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDark -> darkScheme(themePrimary(themeColor))
        else -> lightScheme(themePrimary(themeColor))
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
