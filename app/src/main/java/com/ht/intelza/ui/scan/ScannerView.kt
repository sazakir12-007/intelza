package com.ht.intelza.ui.scan

import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ht.intelza.scan.ScannedCard
import com.ht.intelza.scan.TagScanner

/** Creates a [TagScanner] tied to this composition; it is closed when the composable leaves. */
@Composable
fun rememberTagScanner(): TagScanner {
    val context = LocalContext.current.applicationContext
    val scanner = remember { TagScanner(context) }
    DisposableEffect(scanner) {
        onDispose { scanner.close() }
    }
    return scanner
}

/** How one card is drawn on top of the camera preview. */
data class CardMarker(val label: String?, val color: Color)

/**
 * Live camera preview with an outline and label drawn over every card the scanner sees.
 */
@Composable
fun ScannerView(
    scanner: TagScanner,
    marker: (ScannedCard) -> CardMarker,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(scanner, lifecycleOwner) {
        scanner.bind(lifecycleOwner)
        onDispose { scanner.unbind() }
    }
    val frame by scanner.frames.collectAsStateWithLifecycle()
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)

    Box(modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PreviewView(context).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    // TextureView-based preview composes correctly with the overlay above it.
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    controller = scanner.controller
                }
            },
        )
        Canvas(Modifier.fillMaxSize()) {
            val cards = frame?.cards ?: return@Canvas
            val stroke = Stroke(width = 4.dp.toPx())
            for (card in cards) {
                val style = marker(card)
                val path = Path().apply {
                    card.outline.forEachIndexed { i, p ->
                        if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                    }
                    close()
                }
                drawPath(path, style.color, style = stroke)
                val label = style.label ?: continue
                val layout = textMeasurer.measure(label, labelStyle)
                val padding = 6.dp.toPx()
                val boxSize = Size(
                    layout.size.width + 2 * padding,
                    layout.size.height + padding,
                )
                val topLeft = Offset(
                    card.center.x - boxSize.width / 2,
                    card.center.y - boxSize.height / 2,
                )
                drawRoundRect(
                    color = style.color,
                    topLeft = topLeft,
                    size = boxSize,
                    cornerRadius = CornerRadius(8.dp.toPx()),
                )
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(topLeft.x + padding, topLeft.y + padding / 2),
                )
            }
        }
    }
}
