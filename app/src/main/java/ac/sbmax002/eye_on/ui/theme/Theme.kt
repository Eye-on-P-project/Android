package ac.sbmax002.eye_on.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = EyeOnBlue,
    onPrimary = LightSurface,
    primaryContainer = Color(0xFFEAF2FF),
    onPrimaryContainer = Color(0xFF073763),

    secondary = EyeOnGreen,
    onSecondary = LightSurface,
    secondaryContainer = Color(0xFFE7F8F1),
    onSecondaryContainer = Color(0xFF064E3B),

    tertiary = EyeOnOrange,
    onTertiary = Color(0xFF211400),
    tertiaryContainer = Color(0xFFFFF4DB),
    onTertiaryContainer = Color(0xFF6B3F00),

    background = LightBackground,
    onBackground = LightText,

    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightMutedText,

    outline = LightOutline,
    outlineVariant = Color(0xFFE9EDF3),
    error = EyeOnRed,
    onError = LightSurface
)

private val DarkColorScheme = darkColorScheme(
    primary = EyeOnBlueDark,
    onPrimary = Color(0xFF041F3A),
    primaryContainer = Color(0xFF143B67),
    onPrimaryContainer = Color(0xFFDCEBFF),

    secondary = Color(0xFF67D9AD),
    onSecondary = Color(0xFF052D22),
    secondaryContainer = Color(0xFF143F34),
    onSecondaryContainer = Color(0xFFCAF5E5),

    tertiary = Color(0xFFFFC46B),
    onTertiary = Color(0xFF3E2600),
    tertiaryContainer = Color(0xFF5A3B08),
    onTertiaryContainer = Color(0xFFFFE3AA),

    background = DarkBackground,
    onBackground = DarkText,

    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMutedText,

    outline = DarkOutline,
    outlineVariant = Color(0xFF282E39),
    error = Color(0xFFFF8A8D),
    onError = Color(0xFF3B0508)
)

private val EyeOnShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun EyeOnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = EyeOnShapes,
        content = content
    )
}
