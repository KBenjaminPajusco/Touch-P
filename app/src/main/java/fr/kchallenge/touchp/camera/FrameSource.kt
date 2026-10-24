package fr.kchallenge.touchp.camera

import android.graphics.Bitmap

/**
 * Reçoit les images à analyser, depuis un thread d'arrière-plan.
 * Le bitmap peut être réutilisé par l'appelant : ne pas le conserver après le retour.
 */
fun interface FrameSink {
    /** @param rotationDegrees rotation horaire nécessaire pour remettre l'image à l'endroit. */
    fun onFrame(bitmap: Bitmap, rotationDegrees: Int)
}
