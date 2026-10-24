package com.gradtrack.app.ui.glass

import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gradtrack.app.R
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.isRenderEffectSupported
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule

/*
 * Liquid Glass built on Kyant0/AndroidLiquidGlass ("backdrop", Apache-2.0). The library refracts Compose content;
 * our screens are XML, so ViewBackdrop feeds it the View hierarchy under the glass instead.
 */

/** One tab of the glass bar; [destination] is the nav graph destination it opens. */
data class GlassTab(
    @IdRes val destination: Int,
    @DrawableRes val icon: Int,
    @StringRes val label: Int,
)

/**
 * A [Backdrop] that draws an Android [View] (here: the NavHost with the current screen) under the glass, so the lens
 * and blur bend the real XML content. [frame] must change whenever that view redraws, so the glass re-records it.
 */
class ViewBackdrop(
    private val view: View,
    @ColorInt private val background: Int,
    private val frame: State<Int>,
) : Backdrop {
    override val isCoordinatesDependent: Boolean = true

    private val location = IntArray(2)

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?,
    ) {
        frame.value // read so the glass redraws when the screen under it changes
        val glass = coordinates?.positionInWindow() ?: return
        view.getLocationInWindow(location)
        drawIntoCanvas { canvas ->
            // Transparent parts of the screen would let unrefracted pixels show through: paint the page colour first.
            canvas.nativeCanvas.drawColor(background)
            translate(location[0] - glass.x, location[1] - glass.y) {
                drawIntoCanvas { view.draw(it.nativeCanvas) }
            }
        }
    }
}

private val GlassDark = Color(0xFF25463B)
private val Cream = Color(0xFFF8F4EA)
private val Muted = Color(0xFFD3E0D7)
private val Inter = FontFamily(Font(R.font.inter_w500, FontWeight.Medium), Font(R.font.inter_w600, FontWeight.SemiBold))

/**
 * Floating liquid glass tab bar: a refracting, blurred capsule tinted with the brand green, and a smaller glass
 * "droplet" that springs to the selected tab with a stronger lens and chromatic aberration (after the library's
 * LiquidBottomTabs sample). Without RenderEffect (Android < 12) it falls back to a near-opaque tinted capsule.
 */
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
    val tint = GlassDark.copy(alpha = if (glassSupported) 0.74f else 0.95f)

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(66.dp),
    ) {
        val inner = 5.dp
        val tabWidth = (maxWidth - inner * 2) / tabs.size.coerceAtLeast(1)
        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * selectedIndex.coerceAtLeast(0),
            animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
            label = "glassIndicator",
        )

        // The bar: vibrancy + blur + lens refraction, rim highlight and a soft drop shadow.
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

        // The selected-tab droplet.
        if (selectedIndex >= 0) {
            Box(
                Modifier
                    .padding(inner)
                    .offset(x = indicatorOffset)
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
                            drawRect(GlassDark.copy(alpha = if (glassSupported) 0.45f else 0.85f))
                            drawRect(Cream.copy(alpha = 0.20f))
                        },
                    ),
            )
        }

        Row(
            Modifier
                .fillMaxSize()
                .padding(inner),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                val color = if (selected) Color.White else Muted
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
                    Image(
                        painter = painterResource(tab.icon),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(color),
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.height(3.dp))
                    BasicText(
                        text = stringResource(tab.label),
                        maxLines = 1,
                        style =
                            TextStyle(
                                color = color,
                                fontSize = 11.sp,
                                fontFamily = Inter,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            ),
                    )
                }
            }
        }
    }
}
