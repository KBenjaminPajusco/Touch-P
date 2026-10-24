package fr.kchallenge.touchp.camera

import android.graphics.Bitmap

/** Reçoit les images à analyser. Appelé depuis un thread d'arrière-plan. */
fun interface FrameSink {
    fun onFrame(bitmap: Bitmap)
}
