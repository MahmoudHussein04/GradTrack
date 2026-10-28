package com.gradtrack.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.gradtrack.app.R

object Gt {
    val Primary = Color(0xFF3B6B52)
    val PrimaryDark = Color(0xFF25463B)
    val PrimaryContainer = Color(0xFFE3EDE4)
    val PrimarySoft = Color(0xFFEAF1EC)
    val Background = Color(0xFFF8F4EA)
    val Surface = Color(0xFFFFFFFF)
    val Outline = Color(0xFFEDE7D7)
    val Text = Color(0xFF1F2A37)
    val TextSecondary = Color(0xFF5E6B66)
    val Subtitle = Color(0xFF4F7A64)
    val Muted = Color(0xFFB9CCC0)
    val Error = Color(0xFFB3261E)
    val Track = Color(0xFFE6E9E4)
    val Skeleton = Color(0xFFECE6D6)
    val HeroStart = Color(0xFF2F5A49)
    val HeroEnd = Color(0xFF1C3A2F)
    val GlassFill = Color(0x1FFFFFFF)
    val GlassStroke = Color(0x33FFFFFF)

    val Hero = Brush.linearGradient(listOf(HeroStart, HeroEnd))
}

enum class Tone(val fg: Color, val bg: Color) {
    Green(Color(0xFF2F5D46), Color(0xFFE3EDE4)),
    Yellow(Color(0xFF7A5B0A), Color(0xFFF8EFCF)),
    Orange(Color(0xFFA4501A), Color(0xFFFCE5D4)),
    Red(Color(0xFF9C2B25), Color(0xFFF9DCDA)),
    Gray(Color(0xFF5E6B66), Color(0xFFECE8DA)),
    Blue(Color(0xFF274C86), Color(0xFFDDE8F7)),
}

val Inter =
    FontFamily(
        Font(R.font.inter_w400, FontWeight.Normal),
        Font(R.font.inter_w500, FontWeight.Medium),
        Font(R.font.inter_w600, FontWeight.SemiBold),
        Font(R.font.inter_w700, FontWeight.Bold),
    )

object GtType {
    val Display = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.01).em, color = Gt.Text)
    val Headline = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp, color = Gt.Text)
    val Title = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp, color = Gt.Text)
    val Label = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, color = Gt.Text)
    val Body = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, color = Gt.Text)
    val Caption = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, color = Gt.TextSecondary)
    val Overline = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.08.em, color = Gt.TextSecondary)
    val PageSubtitle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp, color = Gt.Subtitle)
    val Pill = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.sp)
}

private val Colors =
    lightColorScheme(
        primary = Gt.Primary,
        onPrimary = Color.White,
        primaryContainer = Gt.PrimaryContainer,
        onPrimaryContainer = Gt.PrimaryDark,
        secondary = Gt.PrimaryDark,
        onSecondary = Color.White,
        secondaryContainer = Gt.PrimaryContainer,
        onSecondaryContainer = Gt.PrimaryDark,
        background = Gt.Background,
        onBackground = Gt.Text,
        surface = Gt.Surface,
        onSurface = Gt.Text,
        surfaceVariant = Gt.PrimaryContainer,
        onSurfaceVariant = Gt.TextSecondary,
        surfaceContainerLowest = Gt.Surface,
        surfaceContainerLow = Gt.Surface,
        surfaceContainer = Gt.Surface,
        surfaceContainerHigh = Gt.Surface,
        surfaceContainerHighest = Gt.Surface,
        outline = Gt.TextSecondary,
        outlineVariant = Gt.Outline,
        error = Gt.Error,
    )

private val AppTypography =
    Typography(
        displaySmall = GtType.Display,
        headlineSmall = GtType.Headline,
        titleLarge = GtType.Title,
        titleMedium = GtType.Title,
        bodyLarge = GtType.Body.copy(fontSize = 15.sp),
        bodyMedium = GtType.Body,
        bodySmall = GtType.Caption,
        labelLarge = GtType.Label,
        labelMedium = GtType.Label.copy(fontSize = 12.sp),
        labelSmall = GtType.Pill,
    )

private val AppShapes =
    Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(14.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )

@Composable
fun GradTrackTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = AppTypography, shapes = AppShapes, content = content)
}
