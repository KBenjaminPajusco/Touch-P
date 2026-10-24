package fr.kchallenge.touchp.tracking

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
