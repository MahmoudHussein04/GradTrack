package com.gradtrack.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.gradtrack.app.R
import com.gradtrack.app.data.AppError
import com.gradtrack.app.data.model.StepState
import com.gradtrack.app.ui.common.LocalTabBarSpace
import com.gradtrack.app.ui.common.PillStyle
import com.gradtrack.app.ui.common.UiState
import com.gradtrack.app.ui.common.initials
import com.gradtrack.app.ui.common.message
import com.gradtrack.app.ui.theme.Gt
import com.gradtrack.app.ui.theme.GtType
import com.gradtrack.app.ui.theme.Inter
import com.gradtrack.app.ui.theme.Tone

private val ScreenPadding = 16.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenList(
    refreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val bottom = LocalTabBarSpace.current + ScreenPadding
    val list: @Composable () -> Unit = {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, top = ScreenPadding, bottom = bottom),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
    if (onRefresh != null) {
        PullToRefreshBox(isRefreshing = refreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) { list() }
    } else {
        list()
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun <T> LoadableScreen(
    state: UiState<T>,
    onRetry: () -> Unit,
    refreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    content: LazyListScope.(T) -> Unit,
) {
    updateTransition(state, label = "loadable").Crossfade(contentKey = { it::class }) { current ->
        when (current) {
            is UiState.Loading -> SkeletonPage()
            is UiState.Error -> ErrorState(current.error, onRetry)
            is UiState.Success -> ScreenList(refreshing, onRefresh) { content(current.data) }
        }
    }
}

@Composable
fun PageHeader(
    title: String,
    overline: String? = null,
    subtitle: String? = null,
) {
    Column {
        if (overline != null) Text(overline.uppercase(), style = GtType.Overline, modifier = Modifier.padding(bottom = 4.dp))
        Text(title, style = GtType.Display)
        if (subtitle != null) Text(subtitle, style = GtType.PageSubtitle, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun GtCard(
    modifier: Modifier = Modifier,
    padding: Dp = 18.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val body: @Composable () -> Unit = { Column(Modifier.padding(padding), content = content) }
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = shape, color = Gt.Surface, border = BorderStroke(1.dp, Gt.Outline)) { body() }
    } else {
        Surface(modifier = modifier.fillMaxWidth(), shape = shape, color = Gt.Surface, border = BorderStroke(1.dp, Gt.Outline)) { body() }
    }
}

@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = GtType.Title, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun MutedText(text: String, modifier: Modifier = Modifier) {
    Text(text, style = GtType.Body.copy(color = Gt.TextSecondary), modifier = modifier)
}

@Composable
fun Pill(style: PillStyle, modifier: Modifier = Modifier) = Pill(stringResource(style.label), style.tone, modifier)

@Composable
fun Pill(text: String, tone: Tone, modifier: Modifier = Modifier) {
    Text(
        text,
        style = GtType.Pill.copy(color = tone.fg),
        maxLines = 1,
        modifier = modifier.background(tone.bg, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun IconTile(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    tint: Color = Gt.Primary,
    background: Color = Gt.PrimaryContainer,
) {
    Box(modifier.size(size).background(background, RoundedCornerShape(size / 4)), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    @DrawableRes icon: Int? = null,
    textValue: Boolean = false,
) {
    GtCard(modifier.fillMaxHeight(), padding = 14.dp) {
        if (icon != null) {
            IconTile(icon, size = 34.dp)
            Spacer(Modifier.height(10.dp))
        }
        Text(label, style = GtType.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (textValue) {
            Text(
                value,
                style = GtType.Title.copy(fontSize = 19.sp, lineHeight = 23.sp),
                maxLines = 2,
                autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 19.sp),
                modifier = Modifier.padding(top = 2.dp),
            )
        } else {
            Text(value, style = GtType.Headline, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
        }
        if (caption != null) Text(caption, style = GtType.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun StatRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp), content = content)
}

@Composable
fun Avatar(
    name: String,
    imageUrl: String? = null,
    size: Dp = 40.dp,
    background: Color = Gt.PrimaryContainer,
) {
    Box(Modifier.size(size).clip(CircleShape).background(background), contentAlignment = Alignment.Center) {
        Text(name.initials(), style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = (size.value * 0.36f).sp, color = Gt.PrimaryDark))
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(model = imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
fun LabeledValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(top = 10.dp)) {
        Text(label, style = GtType.Caption)
        Text(value, style = GtType.Label)
    }
}

@Composable
fun ProgressBar(progress: Int, modifier: Modifier = Modifier, color: Color = Gt.Primary, track: Color = Gt.Track) {
    Box(modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(track)) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0, 100) / 100f).height(8.dp).clip(CircleShape).background(color))
    }
}

@Composable
fun ProgressRing(progress: Int, modifier: Modifier = Modifier, size: Dp = 88.dp) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            val inset = stroke / 2
            val arcSize = androidx.compose.ui.geometry.Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(Color.White.copy(alpha = 0.2f), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(Color.White, -90f, 360f * progress.coerceIn(0, 100) / 100f, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.percent, progress), style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 21.sp, color = Color.White))
            Text(stringResource(R.string.complete), style = GtType.Caption.copy(color = Gt.Muted, fontSize = 10.sp))
        }
    }
}

@Composable
fun HeroCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Gt.Hero),
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(alpha = 0.08f), radius = 90.dp.toPx(), center = Offset(size.width + 20.dp.toPx(), 10.dp.toPx()))
            drawCircle(Color.White.copy(alpha = 0.05f), radius = 60.dp.toPx(), center = Offset(size.width - 130.dp.toPx(), size.height + 10.dp.toPx()))
        }
        Column(Modifier.padding(20.dp), content = content)
    }
}

@Composable
fun GlassPill(text: String) {
    Text(
        text,
        style = GtType.Pill.copy(color = Color.White),
        modifier = Modifier.background(Gt.GlassFill, CircleShape).border(1.dp, Gt.GlassStroke, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun HeroButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Gt.Background, contentColor = Gt.PrimaryDark),
        contentPadding = PaddingValues(start = 20.dp, end = 14.dp),
    ) {
        Text(text, style = GtType.Label.copy(color = Gt.PrimaryDark))
        Spacer(Modifier.width(6.dp))
        Icon(painterResource(R.drawable.ic_chevron_right), null, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun NextStepHero(@DrawableRes icon: Int, title: String, body: String, action: String, onAction: () -> Unit) {
    HeroCard {
        Box(
            Modifier.size(48.dp).background(Gt.GlassFill, RoundedCornerShape(16.dp)).border(1.dp, Gt.GlassStroke, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(painterResource(icon), null, tint = Color.White, modifier = Modifier.size(24.dp)) }
        Text(title, style = GtType.Title.copy(color = Color.White, fontSize = 19.sp), modifier = Modifier.padding(top = 14.dp))
        Text(body, style = GtType.Caption.copy(color = Gt.Muted, fontSize = 13.sp, lineHeight = 18.sp), modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(16.dp))
        HeroButton(action, onAction)
    }
}

@Composable
fun EmptyState(
    @DrawableRes icon: Int,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    primary: String? = null,
    onPrimary: () -> Unit = {},
) {
    Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(72.dp).background(Gt.PrimarySoft, CircleShape), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), null, tint = Gt.Primary, modifier = Modifier.size(32.dp))
        }
        Text(title, style = GtType.Title, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp))
        Text(body, style = GtType.Body.copy(color = Gt.TextSecondary), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        if (primary != null) {
            Spacer(Modifier.height(20.dp))
            PrimaryButton(primary, onPrimary)
        }
    }
}

@Composable
fun ErrorState(error: AppError, onRetry: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(72.dp).background(Gt.PrimarySoft, CircleShape), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_error), null, tint = Gt.Error, modifier = Modifier.size(32.dp))
        }
        Text(stringResource(R.string.error_title), style = GtType.Title, modifier = Modifier.padding(top = 16.dp))
        Text(error.message(context), style = GtType.Body.copy(color = Gt.TextSecondary), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(20.dp))
        PrimaryButton(stringResource(R.string.retry), onRetry, icon = R.drawable.ic_refresh)
    }
}

@Composable
fun SkeletonPage() {
    val alpha by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 1f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    Column(Modifier.fillMaxSize().padding(ScreenPadding).alpha(alpha), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SkeletonBlock(Modifier.width(120.dp).height(12.dp))
        SkeletonBlock(Modifier.width(220.dp).height(30.dp))
        SkeletonBlock(Modifier.fillMaxWidth().height(150.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonBlock(Modifier.weight(1f).height(96.dp))
            SkeletonBlock(Modifier.weight(1f).height(96.dp))
        }
        SkeletonBlock(Modifier.fillMaxWidth().height(180.dp))
    }
}

@Composable
private fun SkeletonBlock(modifier: Modifier) {
    Box(modifier.background(Gt.Skeleton, RoundedCornerShape(16.dp)))
}

@Composable
fun TimelineRow(
    title: String,
    subtitle: String?,
    state: StepState,
    isLast: Boolean,
    pill: PillStyle? = null,
) {
    val (icon, tint) =
        when (state) {
            StepState.DONE -> R.drawable.ic_check_circle to Gt.Primary
            StepState.CURRENT -> R.drawable.ic_clock to Tone.Yellow.fg
            StepState.WAITING -> R.drawable.ic_circle_outline to Gt.TextSecondary
        }
    Row(Modifier.fillMaxWidth()) {
        Box(Modifier.width(28.dp)) {
            if (!isLast) {
                Box(Modifier.padding(top = 26.dp).width(2.dp).height(44.dp).align(Alignment.TopCenter).background(Gt.Track))
            }
            Icon(painterResource(icon), null, tint = tint, modifier = Modifier.size(24.dp).align(Alignment.TopCenter))
        }
        Row(Modifier.weight(1f).padding(start = 12.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = GtType.Label)
                if (!subtitle.isNullOrBlank()) Text(subtitle, style = GtType.Caption)
            }
            if (pill != null) Pill(pill)
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    @DrawableRes icon: Int? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Gt.Primary, contentColor = Color.White),
        contentPadding = PaddingValues(horizontal = 22.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            if (icon != null) {
                Icon(painterResource(icon), null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = GtType.Label.copy(color = Color.White))
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
    danger: Boolean = false,
) {
    val color = if (danger) Gt.Error else Gt.PrimaryDark
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (danger) Gt.Error else Gt.Outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        contentPadding = PaddingValues(horizontal = 18.dp),
    ) {
        if (icon != null) {
            Icon(painterResource(icon), null, modifier = Modifier.size(18.dp), tint = color)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = GtType.Label.copy(color = color))
    }
}

@Composable
fun TextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier) { Text(text, style = GtType.Label.copy(color = Gt.Primary)) }
}

@Composable
fun GtTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    helper: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    @DrawableRes leadingIcon: Int? = null,
    trailing: (@Composable () -> Unit)? = null,
    suffix: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        isError = error != null,
        supportingText = (error ?: helper)?.let { { Text(it) } },
        singleLine = singleLine,
        minLines = minLines,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        leadingIcon = leadingIcon?.let { { Icon(painterResource(it), null, modifier = Modifier.size(20.dp)) } },
        trailingIcon = trailing,
        suffix = suffix?.let { { Text(it) } },
        shape = RoundedCornerShape(14.dp),
        colors = fieldColors(),
        textStyle = GtType.Body.copy(fontSize = 15.sp, color = if (enabled) Gt.Text else Gt.TextSecondary),
    )
}

@Composable
private fun fieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Gt.Surface,
        unfocusedContainerColor = Gt.Surface,
        disabledContainerColor = Gt.Background,
        disabledBorderColor = Gt.Outline,
        disabledLabelColor = Gt.TextSecondary,
        disabledLeadingIconColor = Gt.TextSecondary,
        errorContainerColor = Gt.Surface,
        focusedBorderColor = Gt.Primary,
        unfocusedBorderColor = Gt.Outline,
        focusedLabelColor = Gt.Primary,
        cursorColor = Gt.Primary,
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownField(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    helper: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected?.let(optionLabel).orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            isError = error != null,
            supportingText = (error ?: helper)?.let { { Text(it) } },
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors(),
            textStyle = GtType.Body.copy(fontSize = 15.sp),
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(optionLabel(option)) }, onClick = {
                    onSelect(option)
                    expanded = false
                })
            }
        }
    }
}

@Composable
fun TextPromptDialog(
    title: String,
    label: String,
    confirm: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    numeric: Boolean = false,
    multiline: Boolean = false,
) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val required = stringResource(R.string.error_required)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = GtType.Title) },
        text = {
            GtTextField(
                value = text,
                onValueChange = {
                    text = it
                    error = null
                },
                label = label,
                error = error,
                singleLine = !multiline,
                minLines = if (multiline) 3 else 1,
                keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text,
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val value = text.trim()
                if (value.isEmpty()) error = required else onConfirm(value)
            }) { Text(confirm, style = GtType.Label.copy(color = Gt.Primary)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), style = GtType.Label.copy(color = Gt.TextSecondary)) } },
        containerColor = Gt.Surface,
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirm: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    danger: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = GtType.Title) },
        text = { Text(message, style = GtType.Body.copy(color = Gt.TextSecondary)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirm, style = GtType.Label.copy(color = if (danger) Gt.Error else Gt.Primary)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), style = GtType.Label.copy(color = Gt.TextSecondary)) } },
        containerColor = Gt.Surface,
    )
}
