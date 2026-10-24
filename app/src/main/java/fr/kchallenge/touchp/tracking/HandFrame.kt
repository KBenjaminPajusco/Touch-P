package fr.kchallenge.touchp.tracking

/** Point normalisé [0..1] dans l'image analysée (x vers la droite, y vers le bas). */
data class Landmark(val x: Float, val y: Float, val z: Float)

/** Côté physique de la main, du point de vue de l'utilisateur. */
enum class Side(val label: String) {
    LEFT("Gauche"),
    RIGHT("Droite");

    companion object {
        /**
         * MediaPipe suppose une image miroir (selfie) alors que nos images ne le sont pas
         * (caméra frontale brute, photos) : son "Left" est donc la main droite de l'utilisateur.
         * Seul endroit où cette inversion est faite.
         */
        fun fromMediaPipe(label: String?): Side = if (label == "Left") RIGHT else LEFT
    }
}

data class Hand(
    val landmarks: List<Landmark>,
    val side: Side,
    val score: Float,
)

/** Résultat d'une inférence, avec les dimensions de l'image sur laquelle elle a eu lieu. */
data class HandFrame(
    val hands: List<Hand> = emptyList(),
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val inferenceMs: Float = 0f,
)
