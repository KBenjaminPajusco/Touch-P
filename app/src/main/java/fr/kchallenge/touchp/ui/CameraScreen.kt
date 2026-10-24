package fr.kchallenge.touchp.ui

import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.kchallenge.touchp.camera.LiveCamera

@Composable
fun CameraScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val frame by vm.frame.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val debug by vm.debug.collectAsStateWithLifecycle()
    val shot by vm.shot.collectAsStateWithLifecycle()
    val readings by vm.readings.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (debug) {
            shot?.let {
                Image(
                    bitmap = it.bitmap.asImageBitmap(),
                    contentDescription = it.label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else {
            val camera = remember { LiveCamera(context) { bmp, rot -> vm.onCameraFrame(bmp, rot) } }
            val previewView = remember {
                PreviewView(context).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
            }
            DisposableEffect(camera) {
                camera.bind(owner, previewView)
                onDispose { camera.shutdown() }
            }
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize(),
            )
        }

        HandOverlay(frame, mirror = !debug)

        Column(
            Modifier.statusBarsPadding().padding(12.dp).clip(RoundedCornerShape(10.dp))
                .background(Color(0x99000000)).padding(10.dp),
        ) {
            Text("FPS %.1f".format(stats.fps), color = Color.White, fontSize = 14.sp)
            Text("Inférence %.1f ms (%s)".format(stats.inferenceMs, stats.delegate), color = Color.White, fontSize = 14.sp)
            Text("Mains : ${frame.hands.size}", color = Color.White, fontSize = 14.sp)
            readings.forEach {
                Text(
                    "${it.side.label} : ${it.gesture.label} · ${it.fingerCount} doigt(s)",
                    color = Color(0xFF69F0AE), fontSize = 14.sp,
                )
            }
            if (debug) shot?.let { Text(it.label, color = Color(0xFFFFEB3B), fontSize = 12.sp) }
        }

        Row(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp)
                .clip(RoundedCornerShape(24.dp)).background(Color(0x99000000)).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Debug (médias)", color = Color.White)
            Switch(checked = debug, onCheckedChange = vm::setDebug)
        }
    }
}
