package fr.kchallenge.touchp.camera

import android.content.Context
import android.graphics.Bitmap
import android.util.Range
import android.util.Size
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.hardware.camera2.CaptureRequest

/**
 * Caméra frontale CameraX : preview + analyse RGBA envoyée au [FrameSink].
 * Les images sont envoyées telles quelles avec leur rotation (pas de copie/rotation de bitmap) et
 * NON miroitées : le miroir est géré à l'affichage, pour que MediaPipe voie la vraie main gauche/droite.
 */
class LiveCamera(private val context: Context, private val sink: FrameSink) {

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var provider: ProcessCameraProvider? = null
    private var reusable: Bitmap? = null

    @androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
    fun bind(owner: LifecycleOwner, previewView: PreviewView) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val cameraProvider = future.get().also { provider = it }

            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }

            val analysisBuilder = ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(Size(640, 480), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER),
                        ).build(),
                )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            // Sans ça, la caméra frontale retombe souvent à ~15 fps en lumière moyenne (auto-exposition).
            Camera2Interop.Extender(analysisBuilder)
                .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(30, 30))
            val analysis = analysisBuilder.build()
            analysis.setAnalyzer(executor) { proxy ->
                try {
                    val plane = proxy.planes[0]
                    val w = proxy.width
                    val h = proxy.height
                    val bmp = reusable?.takeIf { it.width == w && it.height == h }
                        ?: Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { reusable = it }
                    val buffer = plane.buffer.apply { rewind() }
                    if (plane.rowStride == w * 4) {
                        bmp.copyPixelsFromBuffer(buffer)
                    } else {
                        // Lignes avec padding : on copie ligne par ligne dans un bitmap compact.
                        val row = ByteArray(w * 4)
                        val packed = java.nio.ByteBuffer.allocateDirect(w * h * 4)
                        for (y in 0 until h) {
                            buffer.position(y * plane.rowStride)
                            buffer.get(row, 0, w * 4)
                            packed.put(row)
                        }
                        packed.rewind()
                        bmp.copyPixelsFromBuffer(packed)
                    }
                    sink.onFrame(bmp, proxy.imageInfo.rotationDegrees)
                } finally {
                    proxy.close()
                }
            }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(owner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis)
        }, ContextCompat.getMainExecutor(context))
    }

    fun shutdown() {
        provider?.unbindAll()
        executor.shutdown()
    }
}
