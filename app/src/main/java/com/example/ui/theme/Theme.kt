package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemeMode(
    val title: String,
    val subtitle: String,
    val isDark: Boolean,
    val previewPrimary: Color,
    val previewSecondary: Color,
    val previewBg: Color
) {
    CYBER_NEON(
        title = "CYBER NEON",
        subtitle = "Fondo medianoche, acentos neón y orbe Ai brillante (Diseño principal)",
        isDark = true,
        previewPrimary = CyberGlowCyan,
        previewSecondary = CyberGlowAmber,
        previewBg = CyberDarkBg
    ),
    NEON_EMERALD(
        title = "CLASSIC",
        subtitle = "Verde esmeralda, negro y plata · elegante y sobrio",
        isDark = true,
        previewPrimary = NeonEmerald,
        previewSecondary = NeonTurquoise,
        previewBg = NeonDarkBg
    ),
    MIDNIGHT_PURPLE(
        title = "FUTURISTIC",
        subtitle = "Azul eléctrico, violeta y negro · tecnología y energía",
        isDark = true,
        previewPrimary = PurplePrimary,
        previewSecondary = PurpleCyanAccent,
        previewBg = PurpleDarkBg
    ),
    SOLAR_EMBER(
        title = "VIBRANT",
        subtitle = "Colores vivos, acentos intensos y gradientes dinámicos",
        isDark = true,
        previewPrimary = EmberOrange,
        previewSecondary = EmberAmber,
        previewBg = EmberDarkBg
    ),
    CLEAN_MINIMAL(
        title = "MINIMAL",
        subtitle = "Blanco, claridad y elegancia · interfaz despejada",
        isDark = false,
        previewPrimary = CleanPrimary,
        previewSecondary = EmeraldPrimary,
        previewBg = CleanBg
    )
}

val CyberNeonColorScheme: ColorScheme = darkColorScheme(
    primary = CyberGlowCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF00363A),
    onPrimaryContainer = Color(0xFF84FFFF),
    secondary = CyberGlowAmber,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF3E2723),
    onSecondaryContainer = Color(0xFFFFD54F),
    tertiary = CyberGlowPurple,
    onTertiary = Color.Black,
    background = CyberDarkBg,
    onBackground = Color(0xFFF1F5F9),
    surface = CyberDarkSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = CyberDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = CyberBorder
)

val NeonEmeraldColorScheme: ColorScheme = darkColorScheme(
    primary = NeonEmerald,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = NeonMint,
    secondary = NeonTurquoise,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = NeonCyan,
    tertiary = NeonCyan,
    onTertiary = Color.Black,
    background = NeonDarkBg,
    onBackground = Color(0xFFE2E8F0),
    surface = NeonDarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = NeonDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = NeonBorder
)

val MidnightPurpleColorScheme: ColorScheme = darkColorScheme(
    primary = PurplePrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF4C1D95),
    onPrimaryContainer = Color(0xFFE9D5FF),
    secondary = PurpleCyanAccent,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF1E1B4B),
    onSecondaryContainer = Color(0xFFC084FC),
    tertiary = PurpleNeon,
    background = PurpleDarkBg,
    onBackground = Color(0xFFF3E8FF),
    surface = PurpleDarkSurface,
    onSurface = Color(0xFFF5F3FF),
    surfaceVariant = PurpleDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFA78BFA),
    outline = PurpleBorder
)

val SolarEmberColorScheme: ColorScheme = darkColorScheme(
    primary = EmberOrange,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF7C2D12),
    onPrimaryContainer = Color(0xFFFFEDD5),
    secondary = EmberAmber,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF451A03),
    onSecondaryContainer = Color(0xFFFED7AA),
    tertiary = EmberCoral,
    background = EmberDarkBg,
    onBackground = Color(0xFFFFF7ED),
    surface = EmberDarkSurface,
    onSurface = Color(0xFFFFFBEB),
    surfaceVariant = EmberDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFFDBA74),
    outline = EmberBorder
)

val CleanMinimalColorScheme: ColorScheme = lightColorScheme(
    primary = CleanPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = EmeraldPrimary,
    onSecondary = Color.White,
    secondaryContainer = EmeraldContainer,
    onSecondaryContainer = EmeraldDark,
    tertiary = AmberDeal,
    background = CleanBg,
    onBackground = CleanNavy,
    surface = CleanSurface,
    onSurface = CleanNavy,
    surfaceVariant = CleanSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    outline = CleanBorder
)

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.CYBER_NEON,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        AppThemeMode.CYBER_NEON -> CyberNeonColorScheme
        AppThemeMode.NEON_EMERALD -> NeonEmeraldColorScheme
        AppThemeMode.MIDNIGHT_PURPLE -> MidnightPurpleColorScheme
        AppThemeMode.SOLAR_EMBER -> SolarEmberColorScheme
        AppThemeMode.CLEAN_MINIMAL -> CleanMinimalColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
