package fr.kchallenge.touchp.tracking

import android.content.Context
import android.graphics.Bitmap
import org.json.JSONObject

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

    override fun detect(bitmap: Bitmap, rotationDegrees: Int): HandFrame {
        val hands = try {
            val text = context.assets.open("debug/${scene.substringBeforeLast('.')}.json").bufferedReader().use { it.readText() }
            val arr = JSONObject(text).getJSONArray("hands")
            List(arr.length()) { i ->
                val h = arr.getJSONObject(i)
                val pts = h.getJSONArray("landmarks")
                Hand(
                    landmarks = List(pts.length()) { j ->
                        val p = pts.getJSONArray(j)
                        Landmark(p.getDouble(0).toFloat(), p.getDouble(1).toFloat(), p.getDouble(2).toFloat())
                    },
                    label = h.getString("label"),
                    score = h.getDouble("score").toFloat(),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
        return HandFrame(hands, bitmap.width, bitmap.height, inferenceMs = 0f)
    }

    override fun close() = Unit
}
