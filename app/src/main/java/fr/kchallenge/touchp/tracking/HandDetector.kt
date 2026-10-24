package fr.kchallenge.touchp.tracking

import android.graphics.Bitmap

/** Source de landmarks : MediaPipe en vrai ([HandTracker]) ou rejeu de données pré-calculées. */
interface HandDetector : AutoCloseable {
    val delegateName: String

    /** @param rotationDegrees rotation horaire pour remettre [bitmap] à l'endroit. */
    fun detect(bitmap: Bitmap, rotationDegrees: Int = 0): HandFrame

    /** Oublie l'état de suivi (changement de scène). */
    fun reset()

    /** Indique le nom de l'image de test affichée (utile au rejeu uniquement). */
    fun onScene(name: String) {}
}
