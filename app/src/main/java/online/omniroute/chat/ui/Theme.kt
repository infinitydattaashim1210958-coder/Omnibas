package online.omniroute.chat.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val OmniBg = Color(0xFF090B0E)
val OmniSurface = Color(0xFF12151A)
val OmniSurface2 = Color(0xFF1A1E26)
val OmniSurface3 = Color(0xFF232833)
val OmniFg = Color(0xFFEEF0F2)
val OmniMuted = Color(0xFF8B929C)
val OmniSubtle = Color(0xFF5C6370)
val OmniAccent = Color(0xFF6EC9B8)
val OmniAccentFg = Color(0xFF07110F)
val OmniAccentDim = Color(0xFF16352F)
val OmniUser = Color(0xFF1C433C)
val OmniDanger = Color(0xFFD97A6C)

private val DarkColors = darkColorScheme(
    primary = OmniAccent,
    onPrimary = OmniAccentFg,
    background = OmniBg,
    onBackground = OmniFg,
    surface = OmniSurface,
    onSurface = OmniFg,
    surfaceVariant = OmniSurface2,
    onSurfaceVariant = OmniMuted,
    error = OmniDanger,
    onError = OmniFg,
    outline = Color(0x1AEEF0F2),
    secondary = OmniAccentDim,
    onSecondary = OmniAccent,
)

@Composable
fun OmniTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme() || true
    MaterialTheme(
        colorScheme = if (dark) DarkColors else DarkColors,
        content = content,
    )
}
