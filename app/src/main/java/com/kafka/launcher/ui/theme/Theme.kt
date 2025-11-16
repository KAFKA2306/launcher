package com.kafka.launcher.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kafka.launcher.config.LauncherConfig

@Composable
fun KafkaLauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LauncherColorScheme,
        typography = LauncherTypography,
        shapes = LauncherShapes,
        content = content
    )
}

private val LauncherColorScheme = darkColorScheme(
    primary = Color(LauncherConfig.primaryButtonColor),
    onPrimary = Color(LauncherConfig.primaryButtonContentColor),
    surface = Color(LauncherConfig.cardBackgroundColor),
    surfaceVariant = Color(LauncherConfig.surfaceLowColor),
    background = Color(LauncherConfig.homeBackgroundColor),
    onSurface = Color(LauncherConfig.sectionTitleColor),
    onSurfaceVariant = Color(LauncherConfig.sectionTitleVariantColor),
    onBackground = Color(LauncherConfig.sectionTitleColor),
    outline = Color(LauncherConfig.surfaceBorderColor)
)

private val RoundedShape = RoundedCornerShape(LauncherConfig.sectionCardCornerRadiusDp.dp)

private val LauncherShapes = Shapes(
    extraSmall = RoundedShape,
    small = RoundedShape,
    medium = RoundedShape,
    large = RoundedShape,
    extraLarge = RoundedShape
)

private val LauncherTypography = Typography(
    titleMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = LauncherConfig.sectionTitleLineHeightSp.sp
    )
)

val Typography.appGridLabel: TextStyle
    get() = TextStyle(
        fontSize = LauncherConfig.appGridLabelFontSizeSp.sp,
        lineHeight = LauncherConfig.appGridLabelLineHeightSp.sp,
        fontWeight = FontWeight.Medium
    )
