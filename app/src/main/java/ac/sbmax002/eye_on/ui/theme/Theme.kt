package ac.sbmax002.eye_on.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EyeOnColorScheme = darkColorScheme(
    primary = Color(0xFF2196F3),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1976D2),
    onPrimaryContainer = Color.White,

    secondary = Color(0xFFE53935),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC62828),
    onSecondaryContainer = Color.White,

    tertiary = Color(0xFFFFC107),
    onTertiary = Color.Black,

    background = Color(0xFF0A0A0A),
    onBackground = Color.White,

    surface = Color(0xFF1A1A1A),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFF9E9E9E),

    error = Color(0xFFE53935),
    onError = Color.White,
)

@Composable
fun EyeOnTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EyeOnColorScheme,
        typography = Typography,
        content = content
    )
}