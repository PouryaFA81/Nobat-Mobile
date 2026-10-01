package app.nobat.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NobatOrange = Color(0xFFE95420)
private val NobatBg = Color(0xFF1C1C1C)
private val NobatSurface = Color(0xFF2A2A2A)

private val DarkColors = darkColorScheme(
    primary = NobatOrange,
    onPrimary = Color.White,
    background = NobatBg,
    surface = NobatSurface,
    onBackground = Color(0xFFF5F5F5),
    onSurface = Color(0xFFF5F5F5),
)

private val LightColors = lightColorScheme(
    primary = NobatOrange,
    onPrimary = Color.White,
    background = Color(0xFFF0F0F0),
    surface = Color.White,
    onBackground = Color(0xFF1C1C1C),
    onSurface = Color(0xFF1C1C1C),
)

@Composable
fun NobatTheme(
    darkTheme: Boolean = true, // Mobile defaults to dark (family mark)
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme || isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
