package app.nobat.mobile

import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import app.nobat.mobile.ui.HomeScreen
import app.nobat.mobile.ui.theme.NobatTheme

/**
 * Must be [AppCompatActivity] so [androidx.appcompat.app.AppCompatDelegate.setApplicationLocales]
 * recreates with the chosen locale and string resources reload correctly.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        syncWindowLayoutDirection()
        val app = application as NobatApp
        setContent {
            NobatTheme {
                val layoutDirection = rememberAppLayoutDirection()
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        HomeScreen(app = app)
                    }
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        syncWindowLayoutDirection()
    }

    /** Keep Android view / dialog chrome aligned with the Compose layout direction. */
    private fun syncWindowLayoutDirection() {
        val lang = resources.configuration.locales[0]?.language.orEmpty()
        val dir = if (lang.startsWith("fa", ignoreCase = true)) {
            View.LAYOUT_DIRECTION_RTL
        } else {
            View.LAYOUT_DIRECTION_LTR
        }
        window.decorView.layoutDirection = dir
    }
}

/** LayoutDirection from the active app locale (strings), not a hard-coded Rtl. */
@Composable
private fun rememberAppLayoutDirection(): LayoutDirection {
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        val lang = configuration.locales[0]?.language.orEmpty()
        if (lang.startsWith("fa", ignoreCase = true)) {
            LayoutDirection.Rtl
        } else {
            LayoutDirection.Ltr
        }
    }
}
