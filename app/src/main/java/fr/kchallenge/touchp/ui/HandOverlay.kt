package fr.kchallenge.touchp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import fr.kchallenge.touchp.tracking.HandConnections
import fr.kchallenge.touchp.tracking.HandFrame

private val handColors = listOf(Color(0xFF00E5FF), Color(0xFFFF4081))

/**
 * Dessine squelette + points. Le mapping reproduit un affichage "centre-crop" (FILL_CENTER
 * de PreviewView / ContentScale.Crop), avec miroir horizontal pour la caméra frontale.
 */
@Composable
fun HandOverlay(frame: HandFrame, mirror: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        if (frame.imageWidth == 0 || frame.imageHeight == 0) return@Canvas
        val mapper = CropMapper(size, frame.imageWidth, frame.imageHeight, mirror)
        frame.hands.forEachIndexed { index, hand ->
            val color = handColors[index % handColors.size]
            val pts = hand.landmarks.map { mapper.map(it.x, it.y) }
            val stroke = 4.dp.toPx()
            HandConnections.pairs.forEach { (a, b) ->
                drawLine(color, pts[a], pts[b], strokeWidth = stroke, cap = StrokeCap.Round)
            }
            pts.forEach { p ->
                drawCircle(Color.White, radius = 5.dp.toPx(), center = p)
                drawCircle(color, radius = 3.dp.toPx(), center = p)
            }
        }
    }
}

private class CropMapper(canvas: Size, private val iw: Int, private val ih: Int, private val mirror: Boolean) {
    private val w = canvas.width
    private val scale = maxOf(canvas.width / iw, canvas.height / ih)
    private val dx = (canvas.width - iw * scale) / 2f
    private val dy = (canvas.height - ih * scale) / 2f

    fun map(nx: Float, ny: Float): Offset {
        val x = dx + nx * iw * scale
        val y = dy + ny * ih * scale
        return Offset(if (mirror) w - x else x, y)
    }
}

