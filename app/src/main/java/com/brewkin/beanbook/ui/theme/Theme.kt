package com.brewkin.beanbook.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brewkin.beanbook.R

/** 色板：浅色 / 深色各一套，BrewColors 按当前主题动态取值。 */
data class BrewPalette(
    val paper: Color,
    val paperSoft: Color,
    val card: Color,
    val cardPure: Color,
    val ink: Color,
    val inkSolid: Color,
    val inkSoft: Color,
    val muted: Color,
    val mutedLight: Color,
    val line: Color,
    val lineSoft: Color,
    val highlight: Color,
    val lime: Color,
    val limeDeep: Color,
    val limeSoft: Color,
    val butter: Color,
    val butterDeep: Color,
    val rose: Color,
    val roseDeep: Color,
    val sky: Color,
    val skyDeep: Color,
    val peach: Color,
    val forest: Color,
    val brown: Color,
    val dark: Color
)

private val LightPalette = BrewPalette(
    paper = Color(0xFFEDECE7),
    paperSoft = Color(0xFFF3F2EE),
    card = Color(0xFFFBFAF7),
    cardPure = Color(0xFFFFFFFF),
    ink = Color(0xFF26231F),
    inkSolid = Color(0xFF26231F),
    inkSoft = Color(0xFF48433C),
    muted = Color(0xFF8E897E),
    mutedLight = Color(0xFFB4AFA4),
    line = Color(0xFFDFDCD3),
    lineSoft = Color(0xFFE9E6DF),
    highlight = Color(0xFFEEEDE8),
    lime = Color(0xFFA3CD39),
    limeDeep = Color(0xFF7FA51D),
    limeSoft = Color(0xFFEAF2CE),
    butter = Color(0xFFF3E8BE),
    butterDeep = Color(0xFFD7BC62),
    rose = Color(0xFFF0DEDA),
    roseDeep = Color(0xFFB77A6A),
    sky = Color(0xFFDDE9EF),
    skyDeep = Color(0xFF6E8F9D),
    peach = Color(0xFFF1E3C9),
    forest = Color(0xFF4E5B31),
    brown = Color(0xFF6B4A33),
    dark = Color(0xFF1F1D1A)
)

private val DarkPalette = BrewPalette(
    paper = Color(0xFF141417),
    paperSoft = Color(0xFF1C1B20),
    card = Color(0xFF222126),
    cardPure = Color(0xFF26252B),
    ink = Color(0xFFF3F1EC),
    inkSolid = Color(0xFF26231F),
    inkSoft = Color(0xFFD6D2C8),
    muted = Color(0xFFA29D92),
    mutedLight = Color(0xFF79746B),
    line = Color(0xFF3B3941),
    lineSoft = Color(0xFF322F37),
    highlight = Color(0xFF32313A),
    lime = Color(0xFFB7DA4E),
    limeDeep = Color(0xFF9CC22F),
    limeSoft = Color(0xFF2E3A1B),
    butter = Color(0xFF3B3220),
    butterDeep = Color(0xFFE3C877),
    rose = Color(0xFF3A2825),
    roseDeep = Color(0xFFD79A8A),
    sky = Color(0xFF1F2C33),
    skyDeep = Color(0xFF9FC0CD),
    peach = Color(0xFF3A2F1F),
    forest = Color(0xFFC4D8A3),
    brown = Color(0xFFD2A585),
    dark = Color(0xFF0E0D10)
)

/**
 * 全局品牌色。浅色 / 深色由 [BeanBookTheme] 在重组时切换，
 * 所有 `BrewColors.X` 读取都是当前主题下的颜色。
 */
object BrewColors {
    internal var palette: BrewPalette = LightPalette

    val isDark: Boolean get() = palette === DarkPalette

    val Paper: Color get() = palette.paper
    val PaperSoft: Color get() = palette.paperSoft
    val Card: Color get() = palette.card
    val CardPure: Color get() = palette.cardPure
    /** 主文字色：浅色下深墨、深色下浅米。 */
    val Ink: Color get() = palette.ink
    /** 始终为深色的实底（英雄卡 / 深色玻璃条等），不随主题反转。 */
    val InkSolid: Color get() = palette.inkSolid
    val InkSoft: Color get() = palette.inkSoft
    val Muted: Color get() = palette.muted
    val MutedLight: Color get() = palette.mutedLight
    val Line: Color get() = palette.line
    val LineSoft: Color get() = palette.lineSoft
    /** 底部导航选中态浅灰圆角高亮块。 */
    val Highlight: Color get() = palette.highlight
    val Lime: Color get() = palette.lime
    val LimeDeep: Color get() = palette.limeDeep
    val LimeSoft: Color get() = palette.limeSoft
    val Butter: Color get() = palette.butter
    val ButterDeep: Color get() = palette.butterDeep
    val Rose: Color get() = palette.rose
    val RoseDeep: Color get() = palette.roseDeep
    val Sky: Color get() = palette.sky
    val SkyDeep: Color get() = palette.skyDeep
    val Peach: Color get() = palette.peach
    val Forest: Color get() = palette.forest
    val Brown: Color get() = palette.brown
    val Dark: Color get() = palette.dark
}

val DMMono = FontFamily(
    Font(R.font.dm_mono_regular, FontWeight.Normal),
    Font(R.font.dm_mono_medium, FontWeight.Medium)
)

private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 42.sp,
        lineHeight = 46.sp,
        letterSpacing = (-1.4).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.6).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.6).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 23.sp,
        lineHeight = 29.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 25.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 23.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 21.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = DMMono,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.9.sp
    ),
    labelSmall = TextStyle(
        fontFamily = DMMono,
        fontWeight = FontWeight.Normal,
        fontSize = 9.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.6.sp
    )
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

private fun buildScheme(palette: BrewPalette, dark: Boolean): ColorScheme {
    val base = if (dark) {
        darkColorScheme(
            primary = palette.lime,
            onPrimary = palette.inkSolid,
            primaryContainer = palette.limeSoft,
            onPrimaryContainer = palette.ink,
            secondary = palette.brown,
            onSecondary = palette.inkSolid,
            secondaryContainer = palette.peach,
            onSecondaryContainer = palette.ink,
            tertiary = palette.skyDeep,
            onTertiary = palette.inkSolid,
            background = palette.paper,
            onBackground = palette.ink,
            surface = palette.card,
            onSurface = palette.ink,
            surfaceVariant = palette.paperSoft,
            onSurfaceVariant = palette.muted,
            outline = palette.line,
            outlineVariant = palette.lineSoft,
            error = Color(0xFFE08A79),
            onError = palette.inkSolid
        )
    } else {
        lightColorScheme(
            primary = palette.lime,
            onPrimary = palette.inkSolid,
            primaryContainer = palette.limeSoft,
            onPrimaryContainer = palette.ink,
            secondary = palette.brown,
            onSecondary = Color.White,
            secondaryContainer = palette.peach,
            onSecondaryContainer = palette.ink,
            tertiary = palette.skyDeep,
            onTertiary = Color.White,
            background = palette.paper,
            onBackground = palette.ink,
            surface = palette.card,
            onSurface = palette.ink,
            surfaceVariant = palette.paperSoft,
            onSurfaceVariant = palette.muted,
            outline = palette.line,
            outlineVariant = palette.lineSoft,
            error = Color(0xFFB45B4A),
            onError = Color.White
        )
    }
    return base
}

/**
 * App 主题：根据 [darkTheme]（跟随系统 / 手动浅色 / 手动深色，由设置页持久化）切换
 * 品牌色板（BrewColors）与 Material3 ColorScheme。
 */
@Composable
fun BeanBookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    BrewColors.palette = if (darkTheme) DarkPalette else LightPalette
    MaterialTheme(
        colorScheme = buildScheme(BrewColors.palette, darkTheme),
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
