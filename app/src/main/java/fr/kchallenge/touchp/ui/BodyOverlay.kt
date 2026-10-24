package fr.kchallenge.touchp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import fr.kchallenge.touchp.tracking.BodyConnections
import fr.kchallenge.touchp.tracking.BodyFrame

private val bodyColor = Color(0xFFFFD740)

/** Squelette du corps (33 points). Même mapping centre-crop que [HandOverlay]. */
@Composable
fun BodyOverlay(frame: BodyFrame, mirror: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        val landmarks = frame.landmarks ?: return@Canvas
        if (frame.imageWidth == 0 || frame.imageHeight == 0) return@Canvas
        val mapper = CropMapper(size, frame.imageWidth, frame.imageHeight, mirror)
        val pts = landmarks.map { mapper.map(it.x, it.y) }
        BodyConnections.pairs.forEach { (a, b) ->
            if (landmarks[a].visibility > 0.3f && landmarks[b].visibility > 0.3f) {
                drawLine(bodyColor, pts[a], pts[b], strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
            }
        }
        pts.forEachIndexed { i, p ->
            if (landmarks[i].visibility > 0.3f) {
                drawCircle(Color.White, radius = 5.dp.toPx(), center = p)
                drawCircle(bodyColor, radius = 3.dp.toPx(), center = p)
            }
        }
    }
}
