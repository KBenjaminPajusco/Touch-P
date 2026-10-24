package fr.kchallenge.touchp.tracking

/** Point normalisé [0..1] dans l'image analysée (x vers la droite, y vers le bas). */
data class Landmark(val x: Float, val y: Float, val z: Float)

data class Hand(
    val landmarks: List<Landmark>,
    /** "Left" / "Right" tel que renvoyé par MediaPipe (image non miroir). */
    val label: String,
    val score: Float,
)

/** Résultat d'une inférence, avec les dimensions de l'image sur laquelle elle a eu lieu. */
data class HandFrame(
    val hands: List<Hand> = emptyList(),
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val inferenceMs: Float = 0f,
)
