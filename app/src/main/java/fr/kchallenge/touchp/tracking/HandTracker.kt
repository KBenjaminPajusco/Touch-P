package fr.kchallenge.touchp.tracking

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker

/**
 * Enveloppe MediaPipe Hand Landmarker en mode VIDEO (synchrone) : le détecteur de paume n'est
 * relancé que si le suivi est perdu, ce qui est bien plus rapide que le mode IMAGE.
 * Non thread-safe : un seul thread à la fois (voir [detect]).
 */
class HandTracker(context: Context, private val maxHands: Int = 2) : AutoCloseable {

    private val appContext = context.applicationContext
    private var landmarker: HandLandmarker = createLandmarker()
    private var lastTimestampMs = 0L

    var delegateName: String = "?"
        private set

    private fun createLandmarker(): HandLandmarker {
        fun build(delegate: Delegate): HandLandmarker {
            val base = BaseOptions.builder()
                .setModelAssetPath(MODEL_ASSET)
                .setDelegate(delegate)
                .build()
            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(base)
                .setRunningMode(RunningMode.VIDEO)
                .setNumHands(maxHands)
                .setMinHandDetectionConfidence(0.5f)
                .setMinHandPresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .build()
            return HandLandmarker.createFromOptions(appContext, options)
        }
        // Le delegate déjà retenu est réutilisé après un reset (pas de ré-essai GPU inutile).
        if (delegateName == "CPU") return build(Delegate.CPU)
        return try {
            build(Delegate.GPU).also { delegateName = "GPU" }
        } catch (e: Exception) {
            build(Delegate.CPU).also { delegateName = "CPU" }
        }
    }

    /** Oublie l'état de suivi (changement de scène, par ex. d'image de test). */
    fun reset() {
        landmarker.close()
        landmarker = createLandmarker()
        lastTimestampMs = 0L
    }

    /**
     * @param rotationDegrees rotation horaire à appliquer pour remettre [bitmap] à l'endroit
     * (valeur CameraX `rotationDegrees`). Les landmarks sont renvoyés dans le repère redressé.
     */
    fun detect(bitmap: Bitmap, rotationDegrees: Int = 0): HandFrame {
        val start = SystemClock.elapsedRealtimeNanos()
        val timestampMs = maxOf(lastTimestampMs + 1, SystemClock.uptimeMillis())
        lastTimestampMs = timestampMs
        val options = ImageProcessingOptions.builder().setRotationDegrees(rotationDegrees).build()
        val result = landmarker.detectForVideo(BitmapImageBuilder(bitmap).build(), options, timestampMs)
        val ms = (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000f

        val hands = result.landmarks().mapIndexed { i, points ->
            val category = result.handedness().getOrNull(i)?.firstOrNull()
            Hand(
                landmarks = points.map { Landmark(it.x(), it.y(), it.z()) },
                label = category?.categoryName() ?: "?",
                score = category?.score() ?: 0f,
            )
        }
        val sideways = rotationDegrees % 180 != 0
        return HandFrame(
            hands,
            imageWidth = if (sideways) bitmap.height else bitmap.width,
            imageHeight = if (sideways) bitmap.width else bitmap.height,
            inferenceMs = ms,
        )
    }

    override fun close() = landmarker.close()

    companion object {
        const val MODEL_ASSET = "hand_landmarker.task"
    }
}
