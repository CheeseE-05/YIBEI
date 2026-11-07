package com.brewkin.beanbook.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brewkin.beanbook.data.Bean
import com.brewkin.beanbook.data.BeanStatus
import com.brewkin.beanbook.data.status
import com.brewkin.beanbook.ui.theme.BrewColors
import com.brewkin.beanbook.ui.theme.DMMono
import com.brewkin.beanbook.ui.theme.hexColor
import java.io.File

@Composable
fun Overline(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = BrewColors.Muted
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun CircleIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String,
    background: Color = BrewColors.Card,
    tint: Color = BrewColors.Ink,
    size: Dp = 44.dp,
    border: BorderStroke? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .then(if (border != null) Modifier.border(border, CircleShape) else Modifier)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = CircleShape,
        color = if (enabled) BrewColors.Lime else BrewColors.LineSoft,
        contentColor = if (enabled) BrewColors.InkSolid else BrewColors.Muted,
        shadowElevation = if (enabled) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                icon()
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = BrewColors.Ink,
        border = BorderStroke(1.dp, BrewColors.Line)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text,
                modifier = Modifier.padding(horizontal = 22.dp),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun TagPill(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = BrewColors.PaperSoft,
    foreground: Color = BrewColors.InkSoft,
    dotColor: Color? = null
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (dotColor != null) {
            Box(
                Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = foreground, maxLines = 1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectableChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        if (selected) BrewColors.InkSolid else BrewColors.Card,
        animationSpec = tween(180),
        label = "chipBg"
    )
    val fg by animateColorAsState(
        if (selected) Color.White else BrewColors.InkSoft,
        animationSpec = tween(180),
        label = "chipFg"
    )
    Surface(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = CircleShape,
        color = bg,
        contentColor = fg,
        border = if (selected) null else BorderStroke(1.dp, BrewColors.LineSoft)
    ) {
        Box(
            Modifier.padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmallChoice(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) BrewColors.LimeSoft else BrewColors.PaperSoft,
        border = if (selected) BorderStroke(1.dp, BrewColors.LimeDeep.copy(alpha = 0.45f)) else null
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) BrewColors.Forest else BrewColors.InkSoft
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ChoiceFlow(
    options: List<String>,
    selected: String?,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            SmallChoice(option, option == selected, { onSelect(option) })
        }
    }
}

@Composable
fun SectionHeading(
    title: String,
    modifier: Modifier = Modifier,
    overline: String? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(Modifier.weight(1f)) {
            if (overline != null) {
                Overline(overline, color = BrewColors.Muted)
                Spacer(Modifier.height(5.dp))
            }
            Text(title, style = MaterialTheme.typography.headlineSmall, color = BrewColors.Ink)
        }
        if (action != null && onAction != null) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onAction)
                    .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(action, style = MaterialTheme.typography.labelLarge, color = BrewColors.Muted)
                Spacer(Modifier.width(2.dp))
                ChevronRightIcon(Modifier.size(14.dp), BrewColors.Muted)
            }
        }
    }
}

@Composable
fun BrewTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    minHeight: Dp = 30.dp,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    maxLength: Int = 120,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(BrewColors.PaperSoft)
            .padding(horizontal = 15.dp, vertical = 9.dp)
    ) {
        Text(
            label,
            style = TextStyle(
                fontFamily = DMMono,
                fontSize = 9.sp,
                letterSpacing = 0.8.sp
            ),
            color = BrewColors.Muted
        )
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = { if (it.length <= maxLength) onValueChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight.coerceAtLeast(22.dp)),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = BrewColors.Ink),
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            cursorBrush = SolidColor(BrewColors.LimeDeep),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = BrewColors.MutedLight
                        )
                    }
                    inner()
                }
            }
        )
    }
}

@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    color: Color = BrewColors.CardPure,
    radius: Dp = 26.dp,
    border: BorderStroke? = BorderStroke(1.dp, BrewColors.LineSoft),
    content: @Composable ColumnScope.() -> Unit
) {
    // 澎湃 OS 卡片感：白底 + 1dp 细边 + 极轻阴影。
    Column(
        modifier = modifier
            .shadow(
                elevation = 1.dp,
                shape = RoundedCornerShape(radius),
                clip = false,
                ambientColor = BrewColors.Ink.copy(alpha = 0.10f),
                spotColor = BrewColors.Ink.copy(alpha = 0.10f)
            )
            .clip(RoundedCornerShape(radius))
            .background(color)
            .then(if (border != null) Modifier.border(border, RoundedCornerShape(radius)) else Modifier)
    ) {
        content()
    }
}

@Composable
fun StatusTag(status: BeanStatus, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(status.bgHex.hexColor())
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(status.inkHex.hexColor().copy(alpha = 0.75f))
        )
        Spacer(Modifier.width(5.dp))
        Text(
            status.label,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.2.sp),
            color = status.inkHex.hexColor()
        )
    }
}

@Composable
fun BeanArtwork(
    bean: Bean,
    imageFile: File?,
    modifier: Modifier = Modifier,
    radius: Dp = 26.dp,
    showStatus: Boolean = true
) {
    val status = bean.status()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radius))
            .background(status.bgHex.hexColor()),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.34f),
                radius = size.minDimension * 0.34f,
                center = Offset(size.width * 0.52f, size.height * 0.53f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.22f),
                radius = size.minDimension * 0.23f,
                center = Offset(size.width * 0.26f, size.height * 0.25f)
            )
        }
        if (imageFile != null) {
            androidx.compose.foundation.Image(
                painter = coil.compose.rememberAsyncImagePainter(imageFile),
                contentDescription = "${bean.name} 豆子照片",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        } else {
            BeanPlaceholder(bean, Modifier.fillMaxSize().padding(16.dp))
        }
        if (showStatus) {
            StatusTag(status, Modifier.align(Alignment.TopStart).padding(10.dp))
        }
    }
}

@Composable
fun BeanPlaceholder(bean: Bean, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val beanColor = bean.roast.toneHex.hexColor()
            rotate(-24f, pivot = center) {
                drawOval(
                    color = BrewColors.Ink.copy(alpha = 0.10f),
                    topLeft = Offset(w * 0.22f, h * 0.20f),
                    size = Size(w * 0.60f, h * 0.76f)
                )
                drawOval(
                    color = beanColor,
                    topLeft = Offset(w * 0.18f, h * 0.12f),
                    size = Size(w * 0.60f, h * 0.76f)
                )
                val crease = Path().apply {
                    moveTo(w * 0.46f, h * 0.19f)
                    cubicTo(w * 0.70f, h * 0.38f, w * 0.31f, h * 0.66f, w * 0.54f, h * 0.82f)
                }
                drawPath(
                    crease,
                    BrewColors.Ink.copy(alpha = 0.45f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            drawCircle(
                color = Color.White.copy(alpha = 0.55f),
                radius = w * 0.025f,
                center = Offset(w * 0.33f, h * 0.33f)
            )
        }
    }
}

@Composable
fun RatingBeans(
    rating: Int,
    modifier: Modifier = Modifier,
    filledColor: Color = BrewColors.Ink,
    onRating: ((Int) -> Unit)? = null
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in 1..5) {
            Box(
                modifier = if (onRating != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onRating(i) }
                } else Modifier
            ) {
                BeanRatingGlyph(
                    modifier = Modifier.size(17.dp),
                    color = filledColor,
                    filled = i <= rating
                )
            }
        }
    }
}

@Composable
fun DotSeparator(color: Color = BrewColors.MutedLight) {
    Box(
        Modifier
            .size(3.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun MetaRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) { content() }
}

fun Modifier.softClickable(onClick: () -> Unit): Modifier = clickable(
    interactionSource = MutableInteractionSource(),
    indication = null,
    onClick = onClick
)

@Composable
fun EmptyHint(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(74.dp)
                .clip(CircleShape)
                .background(BrewColors.PaperSoft),
            contentAlignment = Alignment.Center
        ) {
            BeanIcon(Modifier.size(32.dp), BrewColors.Muted)
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = BrewColors.Ink)
        Spacer(Modifier.height(6.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = BrewColors.Muted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (action != null) {
            Spacer(Modifier.height(18.dp))
            action()
        }
    }
}
