package com.cappielloantonio.tempo.ui.equalizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cappielloantonio.tempo.R
import com.cappielloantonio.tempo.ui.theme.TempusTheme
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    uiState: EqualizerUiState,
    onEnabledChange: (Boolean) -> Unit,
    onBandLevelChange: (Short, Int) -> Unit,
    onBandLevelChangeFinished: (Short, Int) -> Unit,
    onResetClick: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.equalizer_fragment_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (!uiState.isSupported) {
            EqualizerUnsupportedView(modifier = Modifier.padding(padding))
        } else {
            EqualizerContent(
                uiState = uiState,
                onEnabledChange = onEnabledChange,
                onBandLevelChange = onBandLevelChange,
                onBandLevelChangeFinished = onBandLevelChangeFinished,
                onResetClick = onResetClick,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
fun EqualizerContent(
    uiState: EqualizerUiState,
    onEnabledChange: (Boolean) -> Unit,
    onBandLevelChange: (Short, Int) -> Unit,
    onBandLevelChangeFinished: (Short, Int) -> Unit,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = TempusTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.equalizer_enable),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = uiState.isEnabled,
                onCheckedChange = onEnabledChange
            )
        }

        EqualizerSpectrumPreview(
            bands = uiState.bands,
            minLevel = uiState.minLevel,
            maxLevel = uiState.maxLevel,
            isEnabled = uiState.isEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(bottom = spacing.md)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .alpha(if (uiState.isEnabled) 1f else 0.5f)
        ) {
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 16.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                uiState.bands.forEach { band ->
                    EqualizerBandSlider(
                        band = band,
                        minLevel = uiState.minLevel,
                        maxLevel = uiState.maxLevel,
                        isEnabled = uiState.isEnabled,
                        onLevelChange = { onBandLevelChange(band.id, it) },
                        onLevelChangeFinished = { onBandLevelChangeFinished(band.id, it) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.lg))

        TextButton(
            onClick = onResetClick,
            enabled = uiState.isEnabled
        ) {
            Text(text = stringResource(id = R.string.equalizer_reset))
        }

        Spacer(modifier = Modifier.height(128.dp))
    }
}

@Composable
fun EqualizerSpectrumPreview(
    bands: List<EqualizerBandUiModel>,
    minLevel: Int,
    maxLevel: Int,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val lineColor = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val fillColor = if (isEnabled) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        tonalElevation = 1.dp
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            if (bands.isEmpty()) return@Canvas

            val width = size.width
            val height = size.height
            val zeroY = height - ((0 - minLevel).toFloat() / (maxLevel - minLevel)) * height

            drawLine(
                color = outlineColor,
                start = Offset(0f, zeroY),
                end = Offset(width, zeroY),
                strokeWidth = 1.dp.toPx()
            )

            val points = buildSpectrumSamples(
                bands = bands,
                minLevel = minLevel,
                maxLevel = maxLevel,
                width = width,
                height = height
            )
            val anchors = buildSpectrumAnchors(
                bands = bands,
                minLevel = minLevel,
                maxLevel = maxLevel,
                width = width,
                height = height
            )

            val linePath = buildSmoothSpectrumPath(points)
            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(points.last().x, zeroY)
                lineTo(points.first().x, zeroY)
                close()
            }

            drawPath(
                path = fillPath,
                color = fillColor
            )

            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            anchors.forEach { point ->
                drawCircle(
                    color = lineColor,
                    radius = 3.dp.toPx(),
                    center = point
                )
            }
        }
    }
}

@Composable
fun EqualizerBandSlider(
    band: EqualizerBandUiModel,
    minLevel: Int,
    maxLevel: Int,
    isEnabled: Boolean,
    onLevelChange: (Int) -> Unit,
    onLevelChangeFinished: (Int) -> Unit
) {
    val spacing = TempusTheme.spacing

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = formatDb(band.level / 100),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            modifier = Modifier.height(24.dp),
            color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            VerticalSlider(
                value = band.level,
                min = minLevel,
                max = maxLevel,
                isEnabled = isEnabled,
                onValueChange = onLevelChange,
                onValueChangeFinished = onLevelChangeFinished,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(24.dp)
            )
        }

        Text(
            text = formatFreq(band.frequency),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.xs)
        )
    }
}

@Composable
fun VerticalSlider(
    value: Int,
    min: Int,
    max: Int,
    isEnabled: Boolean,
    onValueChange: (Int) -> Unit,
    onValueChangeFinished: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val thumbColor = MaterialTheme.colorScheme.primary
    val disabledColor = MaterialTheme.colorScheme.outline
    val activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
    val zeroTickColor = MaterialTheme.colorScheme.outline
    val regularTickColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.9f)
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {
        val height = constraints.maxHeight.toFloat()
        val thumbRadiusPx = with(density) { 10.dp.toPx() }
        val trackWidthPx = with(density) { 4.dp.toPx() }
        val tickWidthPx = with(density) { 1.5.dp.toPx() }
        val trackTop = thumbRadiusPx
        val trackBottom = (height - thumbRadiusPx).coerceAtLeast(trackTop)
        val trackHeight = (trackBottom - trackTop).coerceAtLeast(1f)
        val tickLevels = remember(min, max) { buildTickLevels(min, max) }
        var displayValue by remember { mutableIntStateOf(value) }
        var isDragging by remember { mutableStateOf(false) }
        var lastHapticTick by remember { mutableStateOf<Int?>(null) }

        LaunchedEffect(value, isDragging) {
            if (!isDragging) {
                displayValue = value.coerceIn(min, max)
            }
        }

        fun valueFromPosition(positionY: Float): Int {
            val clamped = positionY.coerceIn(trackTop, trackBottom)
            val percentage = 1f - ((clamped - trackTop) / trackHeight)
            return (min + percentage * (max - min)).roundToInt().coerceIn(min, max)
        }

        fun maybePerformHaptic(newValue: Int) {
            val currentTick = nearestTick(newValue, tickLevels)
            if (currentTick != null && currentTick != lastHapticTick && abs(newValue - currentTick) <= 40) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                lastHapticTick = currentTick
            } else if (currentTick == null) {
                lastHapticTick = null
            }
        }

        fun updateValue(positionY: Float, fromUser: Boolean) {
            val newValue = valueFromPosition(positionY)
            displayValue = newValue
            if (fromUser) {
                maybePerformHaptic(newValue)
                onValueChange(newValue)
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isEnabled, min, max) {
                    if (!isEnabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            updateValue(offset.y, fromUser = true)
                        },
                        onDragEnd = {
                            isDragging = false
                            lastHapticTick = null
                            onValueChangeFinished(displayValue)
                        },
                        onDragCancel = {
                            isDragging = false
                            lastHapticTick = null
                            displayValue = value.coerceIn(min, max)
                        }
                    ) { change, _ ->
                        change.consume()
                        updateValue(change.position.y, fromUser = true)
                    }
                }
        ) {
            val centerX = size.width / 2
            val zeroY = trackBottom - ((0 - min).toFloat() / (max - min)) * trackHeight
            val thumbY = trackBottom - ((displayValue - min).toFloat() / (max - min)) * trackHeight
            val tickHalfWidth = size.width * 0.18f

            drawLine(
                color = if (isEnabled) trackColor else disabledColor.copy(alpha = 0.3f),
                start = Offset(centerX, trackTop),
                end = Offset(centerX, trackBottom),
                strokeWidth = trackWidthPx,
                cap = StrokeCap.Round
            )

            tickLevels.forEach { tickLevel ->
                val tickY = trackBottom - ((tickLevel - min).toFloat() / (max - min)) * trackHeight
                val tickColor = when {
                    !isEnabled -> disabledColor.copy(alpha = 0.25f)
                    tickLevel == 0 -> zeroTickColor
                    else -> regularTickColor
                }
                val tickLengthMultiplier = if (tickLevel == 0) 1.6f else 1f
                drawLine(
                    color = tickColor,
                    start = Offset(centerX - tickHalfWidth * tickLengthMultiplier, tickY),
                    end = Offset(centerX + tickHalfWidth * tickLengthMultiplier, tickY),
                    strokeWidth = tickWidthPx,
                    cap = StrokeCap.Round
                )
            }

            if (abs(displayValue) > 0) {
                drawLine(
                    color = if (isEnabled) activeTrackColor else disabledColor.copy(alpha = 0.45f),
                    start = Offset(centerX, zeroY),
                    end = Offset(centerX, thumbY),
                    strokeWidth = trackWidthPx,
                    cap = StrokeCap.Round
                )
            }

            drawCircle(
                color = if (isEnabled) thumbColor else disabledColor,
                radius = thumbRadiusPx,
                center = Offset(centerX, thumbY)
            )
        }
    }
}

@Composable
fun EqualizerUnsupportedView(modifier: Modifier = Modifier) {
    val spacing = TempusTheme.spacing
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ui_eq_managed_externally),
            contentDescription = null,
            modifier = Modifier
                .sizeIn(maxWidth = 240.dp, maxHeight = 240.dp)
                .padding(bottom = spacing.md),
            contentScale = ContentScale.Fit
        )
        Text(
            text = stringResource(id = R.string.equalizer_managed_externally),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

private fun formatDb(value: Int): String = if (value > 0) "+$value" else "$value"

private fun formatFreq(freq: Int): String {
    return if (freq >= 1000) {
        if (freq % 1000 == 0) {
            "${freq / 1000}k"
        } else {
            "%.1fk".format(freq / 1000f)
        }
    } else {
        "$freq"
    }
}

private fun buildTickLevels(min: Int, max: Int): List<Int> {
    val majorStep = 500
    val ticks = linkedSetOf<Int>()
    var level = 0
    while (level <= max) {
        ticks += level
        level += majorStep
    }
    level = -majorStep
    while (level >= min) {
        ticks += level
        level -= majorStep
    }
    return ticks
        .filter { it in min..max }
        .sortedDescending()
}

private fun nearestTick(value: Int, tickLevels: List<Int>): Int? {
    return tickLevels.minByOrNull { abs(it - value) }
}

private fun buildSpectrumAnchors(
    bands: List<EqualizerBandUiModel>,
    minLevel: Int,
    maxLevel: Int,
    width: Float,
    height: Float
): List<Offset> {
    if (bands.isEmpty()) return emptyList()
    if (bands.size == 1) {
        val singleBand = bands.first()
        return listOf(
            Offset(
                x = width / 2f,
                y = levelToPreviewY(singleBand.level, minLevel, maxLevel, height)
            )
        )
    }

    val minFreq = bands.first().frequency.coerceAtLeast(1)
    val maxFreq = bands.last().frequency.coerceAtLeast(minFreq + 1)

    return bands.map { band ->
        Offset(
            x = frequencyToPreviewX(
                frequency = band.frequency,
                minFrequency = minFreq,
                maxFrequency = maxFreq,
                width = width
            ),
            y = levelToPreviewY(band.level, minLevel, maxLevel, height)
        )
    }
}

private fun buildSpectrumSamples(
    bands: List<EqualizerBandUiModel>,
    minLevel: Int,
    maxLevel: Int,
    width: Float,
    height: Float
): List<Offset> {
    if (bands.isEmpty()) return emptyList()

    val anchors = buildSpectrumAnchors(
        bands = bands,
        minLevel = minLevel,
        maxLevel = maxLevel,
        width = width,
        height = height
    )

    if (anchors.size == 1) return anchors

    val samplesPerSegment = 18
    val sampledPoints = mutableListOf<Offset>()

    for (index in 0 until anchors.lastIndex) {
        val start = anchors[index]
        val end = anchors[index + 1]

        for (step in 0 until samplesPerSegment) {
            val t = step / samplesPerSegment.toFloat()
            val x = lerp(start.x, end.x, t)
            val easedT = smoothStep(t)
            val y = lerp(start.y, end.y, easedT)
            sampledPoints += Offset(x, y)
        }
    }

    sampledPoints += anchors.last()
    return sampledPoints
}

private fun buildSmoothSpectrumPath(points: List<Offset>): Path {
    return Path().apply {
        if (points.isEmpty()) return@apply
        moveTo(points.first().x, points.first().y)

        if (points.size == 1) {
            lineTo(points.first().x, points.first().y)
            return@apply
        }

        for (index in 0 until points.lastIndex) {
            val current = points[index]
            val next = points[index + 1]
            val midX = (current.x + next.x) / 2f
            quadraticTo(
                midX,
                current.y,
                next.x,
                next.y
            )
        }
    }
}

private fun frequencyToPreviewX(
    frequency: Int,
    minFrequency: Int,
    maxFrequency: Int,
    width: Float
): Float {
    if (maxFrequency <= minFrequency) return width / 2f
    val minLog = ln(minFrequency.toFloat())
    val maxLog = ln(maxFrequency.toFloat())
    val valueLog = ln(frequency.coerceAtLeast(1).toFloat())
    return ((valueLog - minLog) / (maxLog - minLog)) * width
}

private fun levelToPreviewY(
    level: Int,
    minLevel: Int,
    maxLevel: Int,
    height: Float
): Float {
    return (height - ((level - minLevel).toFloat() / (maxLevel - minLevel)) * height)
        .coerceIn(0f, height)
}

private fun smoothStep(value: Float): Float {
    val clamped = value.coerceIn(0f, 1f)
    return clamped * clamped * (3f - 2f * clamped)
}

private fun lerp(start: Float, end: Float, fraction: Float): Float {
    return start + (end - start) * fraction
}
