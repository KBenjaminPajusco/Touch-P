package fr.kchallenge.touchp.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.LifecycleCameraController
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Caméra frontale CameraX : preview (via PreviewView) + analyse RGBA envoyée au [FrameSink].
 * Les bitmaps sont remis à l'endroit (rotation appliquée) mais NON miroités : le miroir est
 * géré à l'affichage, pour que MediaPipe voie la vraie main gauche/droite.
 */
class LiveCamera(context: Context, private val sink: FrameSink) {

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    val controller = LifecycleCameraController(context).apply {
        cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        // Le preview est géré par PreviewView (controller attaché) ; ici on active l'analyse.
        setEnabledUseCases(LifecycleCameraController.IMAGE_ANALYSIS)
        imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
        imageAnalysisOutputImageFormat = ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
        setImageAnalysisAnalyzer(executor) { proxy ->
            try {
                val raw = proxy.toBitmap()
                val rotation = proxy.imageInfo.rotationDegrees
                val upright = if (rotation == 0) raw else {
                    Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(rotation.toFloat()) }, true)
                }
                sink.onFrame(upright)
            } finally {
                proxy.close()
            }
        }
    }

    fun bind(owner: LifecycleOwner) {
        controller.bindToLifecycle(owner)
    }

    fun shutdown() {
        controller.unbind()
        executor.shutdown()
    }
}
