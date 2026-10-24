package fr.kchallenge.touchp.tracking

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker

/** MediaPipe Pose Landmarker (une personne, mode VIDEO). Même contrat que [HandTracker] : un thread à la fois. */
class BodyTracker(context: Context) : AutoCloseable {

    private val landmarker: PoseLandmarker
    private var lastTimestampMs = 0L

    val delegateName: String

    init {
        val appContext = context.applicationContext
        val (created, delegate) = createWithFallback(null) { delegate ->
            val base = BaseOptions.builder().setModelAssetPath(MODEL_ASSET).setDelegate(delegate).build()
            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(base)
                .setRunningMode(RunningMode.VIDEO)
                .setNumPoses(1)
                .build()
            PoseLandmarker.createFromOptions(appContext, options)
        }
        landmarker = created
        delegateName = delegate
    }

    /** @param rotationDegrees rotation horaire pour remettre [bitmap] à l'endroit (CameraX). */
    fun detect(bitmap: Bitmap, rotationDegrees: Int = 0): BodyFrame {
        val start = SystemClock.elapsedRealtimeNanos()
        val timestampMs = maxOf(lastTimestampMs + 1, SystemClock.uptimeMillis())
        lastTimestampMs = timestampMs
        val options = ImageProcessingOptions.builder().setRotationDegrees(rotationDegrees).build()
        val result = landmarker.detectForVideo(BitmapImageBuilder(bitmap).build(), options, timestampMs)
        val ms = (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000f

        val landmarks = result.landmarks().firstOrNull()?.map {
            Landmark(it.x(), it.y(), it.z(), it.visibility().orElse(1f))
        }
        val sideways = rotationDegrees % 180 != 0
        return BodyFrame(
            landmarks,
            imageWidth = if (sideways) bitmap.height else bitmap.width,
            imageHeight = if (sideways) bitmap.width else bitmap.height,
            inferenceMs = ms,
        )
    }

    override fun close() = landmarker.close()

    companion object {
        const val MODEL_ASSET = "pose_landmarker_lite.task"
    }
}
