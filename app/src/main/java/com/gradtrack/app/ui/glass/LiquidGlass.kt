package com.gradtrack.app.ui.glass

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gradtrack.app.ui.theme.Gt
import com.gradtrack.app.ui.theme.Inter
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.isRenderEffectSupported
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule

data class GlassTab(
    val route: String,
    @DrawableRes val icon: Int,
    @StringRes val label: Int,
)

private val LabelOff = Color(0xFFD3E0D7)

@Composable
fun GlassTabBar(
    tabs: List<GlassTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val glassSupported = remember { isRenderEffectSupported() }
    val tint = Gt.PrimaryDark.copy(alpha = if (glassSupported) 0.74f else 0.95f)

    BoxWithConstraints(modifier.fillMaxWidth().height(66.dp)) {
        val inner = 5.dp
        val tabWidth = (maxWidth - inner * 2) / tabs.size.coerceAtLeast(1)
        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * selectedIndex.coerceAtLeast(0),
            animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
            label = "glassIndicator",
        )

        Box(
            Modifier
                .fillMaxSize()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
                    effects = {
                        vibrancy()
                        blur(6.dp.toPx())
                        lens(refractionHeight = 18.dp.toPx(), refractionAmount = 26.dp.toPx())
                    },
                    highlight = { Highlight.Default.copy(alpha = 0.7f) },
                    shadow = { Shadow(radius = 22.dp, color = Color.Black.copy(alpha = 0.22f)) },
                    onDrawSurface = { drawRect(tint) },
                ),
        )

        if (selectedIndex >= 0) {
            Box(
                Modifier
                    .padding(inner)
                    .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                    .width(tabWidth)
                    .fillMaxHeight()
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = {
                            vibrancy()
                            lens(refractionHeight = 10.dp.toPx(), refractionAmount = 16.dp.toPx(), chromaticAberration = true)
                        },
                        highlight = { Highlight.Default },
                        shadow = { Shadow(radius = 10.dp, color = Color.Black.copy(alpha = 0.14f)) },
                        onDrawSurface = {
                            drawRect(Gt.PrimaryDark.copy(alpha = if (glassSupported) 0.45f else 0.85f))
                            drawRect(Gt.Background.copy(alpha = 0.20f))
                        },
                    ),
            )
        }

        Row(Modifier.fillMaxSize().padding(inner), verticalAlignment = Alignment.CenterVertically) {
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                val color = if (selected) Color.White else LabelOff
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics {
                            role = Role.Tab
                            this.selected = selected
                        }
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            if (!selected) {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                onSelect(index)
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(painterResource(tab.icon), contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.height(3.dp))
                    Text(
                        stringResource(tab.label),
                        maxLines = 1,
                        style = TextStyle(color = color, fontSize = 11.sp, fontFamily = Inter, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
                    )
                }
            }
        }
    }
}
