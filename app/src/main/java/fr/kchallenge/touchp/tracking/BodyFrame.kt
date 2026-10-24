package fr.kchallenge.touchp.tracking

/** En dessous, un landmark du corps est considéré comme non visible (hors champ ou masqué). */
const val MIN_VISIBILITY = 0.3f

val Landmark.isVisible: Boolean get() = visibility >= MIN_VISIBILITY

/** Résultat d'une inférence de pose : 33 landmarks pour une personne, ou null si personne. */
data class BodyFrame(
    val landmarks: List<Landmark>? = null,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val inferenceMs: Float = 0f,
)

/** Index des 33 landmarks MediaPipe Pose (ceux utilisés ici). */
object PoseLm {
    const val L_SHOULDER = 11
    const val R_SHOULDER = 12
    const val L_ELBOW = 13
    const val R_ELBOW = 14
    const val L_WRIST = 15
    const val R_WRIST = 16
    const val L_HIP = 23
    const val R_HIP = 24
    const val L_KNEE = 25
    const val R_KNEE = 26
    const val L_ANKLE = 27
    const val R_ANKLE = 28
    const val COUNT = 33
}

object BodyConnections {
    val pairs: List<Pair<Int, Int>> = listOf(
        // visage
        0 to 1, 1 to 2, 2 to 3, 3 to 7, 0 to 4, 4 to 5, 5 to 6, 6 to 8, 9 to 10,
        // torse
        11 to 12, 11 to 23, 12 to 24, 23 to 24,
        // bras
        11 to 13, 13 to 15, 15 to 17, 15 to 19, 15 to 21, 17 to 19,
        12 to 14, 14 to 16, 16 to 18, 16 to 20, 16 to 22, 18 to 20,
        // jambes
        23 to 25, 25 to 27, 27 to 29, 29 to 31, 27 to 31,
        24 to 26, 26 to 28, 28 to 30, 30 to 32, 28 to 32,
    )
}
