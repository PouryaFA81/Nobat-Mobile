package app.nobat.mobile.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColors = darkColorScheme(
    primary = Brand,
    onPrimary = OnBrand,
    primaryContainer = BrandMuted,
    onPrimaryContainer = InkDark,
    background = BgDark,
    onBackground = InkDark,
    surface = SurfaceDark,
    onSurface = InkDark,
    surfaceVariant = SurfaceRaisedDark,
    onSurfaceVariant = MutedDark,
    error = Danger,
    onError = OnBrand,
)

private val LightColors = lightColorScheme(
    primary = Brand,
    onPrimary = OnBrand,
    primaryContainer = BrandMuted,
    onPrimaryContainer = InkLight,
    background = BgLight,
    onBackground = InkLight,
    surface = SurfaceLight,
    onSurface = InkLight,
    surfaceVariant = SurfaceRaisedLight,
    onSurfaceVariant = MutedLight,
    error = Danger,
    onError = OnBrand,
)

private val NobatShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun NobatTheme(
    darkTheme: Boolean = true, // dark-first (matches mark)
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        shapes = NobatShapes,
        content = content,
    )
}
