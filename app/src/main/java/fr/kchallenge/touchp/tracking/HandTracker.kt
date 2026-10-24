package fr.kchallenge.touchp.tracking

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker

/**
 * Enveloppe MediaPipe Hand Landmarker en mode IMAGE (synchrone) : l'appelant choisit le thread.
 * Non thread-safe : un seul thread à la fois (voir [detect]).
 */
class HandTracker(context: Context, maxHands: Int = 2) : AutoCloseable {

    private val landmarker: HandLandmarker = createLandmarker(context.applicationContext, maxHands)

    var delegateName: String = "?"
        private set

    private fun createLandmarker(context: Context, maxHands: Int): HandLandmarker {
        fun build(delegate: Delegate): HandLandmarker {
            val base = BaseOptions.builder()
                .setModelAssetPath(MODEL_ASSET)
                .setDelegate(delegate)
                .build()
            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(base)
                .setRunningMode(RunningMode.IMAGE)
                .setNumHands(maxHands)
                .setMinHandDetectionConfidence(0.5f)
                .setMinHandPresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .build()
            return HandLandmarker.createFromOptions(context, options)
        }
        return try {
            build(Delegate.GPU).also { delegateName = "GPU" }
        } catch (e: Exception) {
            build(Delegate.CPU).also { delegateName = "CPU" }
        }
    }

    fun detect(bitmap: Bitmap): HandFrame {
        val start = SystemClock.elapsedRealtimeNanos()
        val result = landmarker.detect(BitmapImageBuilder(bitmap).build())
        val ms = (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000f

        val hands = result.landmarks().mapIndexed { i, points ->
            val category = result.handedness().getOrNull(i)?.firstOrNull()
            Hand(
                landmarks = points.map { Landmark(it.x(), it.y(), it.z()) },
                label = category?.categoryName() ?: "?",
                score = category?.score() ?: 0f,
            )
        }
        return HandFrame(hands, bitmap.width, bitmap.height, ms)
    }

    override fun close() = landmarker.close()

    companion object {
        const val MODEL_ASSET = "hand_landmarker.task"
    }
}
