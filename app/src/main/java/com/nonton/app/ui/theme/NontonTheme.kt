package com.nonton.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ============================================================
// Design Tokens – meniru streaming-app-ui.html CSS variables
// ============================================================

// Dark scheme (default) – Netflix-inspired
private val DarkColorScheme = darkColorScheme(
    primary           = Color(0xFFE50914),  // Brand red
    onPrimary         = Color(0xFFFFFFFF),
    primaryContainer  = Color(0xFF2D0004),
    onPrimaryContainer = Color(0xFFFFDAD6),

    secondary         = Color(0xFFE50914),
    onSecondary       = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF2D0004),
    onSecondaryContainer = Color(0xFFFFDAD6),

    tertiary          = Color(0xFFFFD700),   // Gold / star rating
    onTertiary        = Color(0xFF0D0D0D),

    // Surfaces
    background        = Color(0xFF0D0D0D),   // --bg-primary
    onBackground      = Color(0xFFFFFFFF),
    surface           = Color(0xFF1A1A1A),   // --bg-secondary
    onSurface         = Color(0xFFFFFFFF),
    surfaceVariant    = Color(0xFF262626),   // --bg-tertiary
    onSurfaceVariant  = Color(0xFFB3B3B3),   // --text-secondary

    // Misc
    error             = Color(0xFFEF4444),
    onError           = Color(0xFFFFFFFF),
    outline           = Color(0xFF333333),   // --border-color
    inverseSurface    = Color(0xFFE6E1E5),
    inverseOnSurface  = Color(0xFF1A1A1A),
    inversePrimary    = Color(0xFFFF4B4B),
    surfaceTint       = Color(0xFFE50914)
)

private val LightColorScheme = lightColorScheme(
    primary           = Color(0xFFE50914),
    onPrimary         = Color(0xFFFFFFFF),
    primaryContainer  = Color(0xFFE50914),
    onPrimaryContainer = Color(0xFF0D0D0D),
    secondary         = Color(0xFFE50914),
    onSecondary       = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFC1C1),
    onSecondaryContainer = Color(0xFF0D0D0D),
    tertiary          = Color(0xFFFFD700),
    onTertiary        = Color(0xFF0D0D0D),
    background        = Color(0xFFF5F5F5),
    onBackground      = Color(0xFF0D0D0D),
    surface           = Color(0xFFFFFFFF),
    onSurface         = Color(0xFF0D0D0D),
    surfaceVariant    = Color(0xFFEEEEEE),
    onSurfaceVariant  = Color(0xFF4A4A4A),
    error             = Color(0xFFEF4444),
    onError           = Color(0xFFFFFFFF),
    outline           = Color(0xFFCCCCCC),
    inverseSurface    = Color(0xFF0D0D0D),
    inverseOnSurface  = Color(0xFFFFFFFF),
    inversePrimary    = Color(0xFFFF4B4B),
    surfaceTint       = Color(0xFFE50914)
)

@Composable
fun NontonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = NontonTypography,
        content = content
    )
}

// ============================================================
// Typography
// ============================================================
private val NontonTypography = androidx.compose.material3.Typography(
    // Display – digunakan untuk hero title besar
    displayLarge = TextStyle(
        fontSize = 36.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.03).sp,
        color = Color.White,
        lineHeight = 40.sp
    ),
    displayMedium = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.02).sp,
        color = Color.White
    ),
    // Headlines – section titles
    headlineLarge = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    ),
    headlineMedium = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    ),
    headlineSmall = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    ),
    // Title – card titles, section labels
    titleLarge = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = Color.White
    ),
    // Body – overview, description
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        color = Color(0xFFB3B3B3),
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = Color(0xFFB3B3B3),
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        color = Color(0xFF737373)
    ),
    // Label
    labelLarge = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFFB3B3B3)
    ),
    labelSmall = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF737373)
    )
)
