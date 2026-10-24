package fr.kchallenge.touchp.tracking

import android.content.Context
import android.graphics.Bitmap

/**
 * Rejoue les landmarks de `assets/debug/<image>.json` (générés par scripts/gen_landmarks.py).
 * Pour l'émulateur CI, où la lib native arm64 de MediaPipe plante sous traduction ARM :
 * on valide ainsi l'affichage et l'overlay, pas la détection elle-même.
 */
class ReplayDetector(private val context: Context) : HandDetector {
    override val delegateName = "REPLAY"
    private var scene: String = ""

    override fun onScene(name: String) {
        scene = name
    }

    override fun reset() = Unit

    override fun detect(bitmap: Bitmap, rotationDegrees: Int): HandFrame = try {
        val text = context.assets.open("debug/${scene.substringBeforeLast('.')}.json").bufferedReader().use { it.readText() }
        HandJson.parse(text)
    } catch (e: Exception) {
        HandFrame(emptyList(), bitmap.width, bitmap.height)
    }

    override fun close() = Unit
}
