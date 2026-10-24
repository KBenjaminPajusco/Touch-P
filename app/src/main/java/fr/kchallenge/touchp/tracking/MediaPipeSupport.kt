package fr.kchallenge.touchp.tracking

import android.graphics.Bitmap
import android.os.SystemClock
import com.google.mediapipe.tasks.core.Delegate

/**
 * Crée un détecteur MediaPipe en GPU, avec repli CPU si l'initialisation GPU échoue.
 * @param previous delegate déjà retenu (réutilisé tel quel après un reset, sans ré-essayer le GPU).
 * @return le détecteur et le nom du delegate retenu.
 */
internal fun <T> createWithFallback(previous: String?, build: (Delegate) -> T): Pair<T, String> {
    if (previous == "CPU") return build(Delegate.CPU) to "CPU"
    return try {
        build(Delegate.GPU) to "GPU"
    } catch (e: Exception) {
        build(Delegate.CPU) to "CPU"
    }
}

/** Horodatage strictement croissant exigé par le mode VIDEO de MediaPipe. */
internal class MonotonicClockMs {
    private var last = 0L

    fun next(): Long = maxOf(last + 1, SystemClock.uptimeMillis()).also { last = it }

    fun reset() {
        last = 0L
    }
}

/** (largeur, hauteur) du bitmap une fois redressé de [rotationDegrees]. */
internal fun Bitmap.uprightSize(rotationDegrees: Int): Pair<Int, Int> =
    if (rotationDegrees % 180 != 0) height to width else width to height
