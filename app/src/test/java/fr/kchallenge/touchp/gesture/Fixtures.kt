package fr.kchallenge.touchp.gesture

import fr.kchallenge.touchp.tracking.Hand
import fr.kchallenge.touchp.tracking.HandFrame
import fr.kchallenge.touchp.tracking.Landmark
import org.json.JSONObject
import java.io.File

/** Vraies détections MediaPipe des images de `assets/debug` (générées par scripts/gen_landmarks.py). */
object Fixtures {
    private val dir = File("src/main/assets/debug")

    fun frame(name: String): HandFrame {
        val json = JSONObject(File(dir, "$name.json").readText())
        val arr = json.getJSONArray("hands")
        val hands = List(arr.length()) { i ->
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
        return HandFrame(hands, json.getInt("width"), json.getInt("height"))
    }

    fun analyze(name: String, handIndex: Int = 0): HandPose {
        val f = frame(name)
        return HandAnalyzer.analyze(f.hands[handIndex], f.imageWidth, f.imageHeight)
    }
}

/**
 * Main synthétique (image carrée), doigts vers le haut, pour les gestes sans photo de référence.
 * Un doigt replié a son bout et sa DIP ramenés vers la paume.
 */
object SyntheticHand {
    fun build(
        thumb: Boolean = false,
        index: Boolean = false,
        middle: Boolean = false,
        ring: Boolean = false,
        pinky: Boolean = false,
        pinch: Boolean = false,
    ): Hand {
        val pts = MutableList(21) { Landmark(0.5f, 0.9f, 0f) }
        fun set(i: Int, x: Float, y: Float) { pts[i] = Landmark(x, y, 0f) }
        set(0, 0.5f, 0.9f)
        // Doigts : (mcp, pip, dip, tip), base du majeur à 0.2 au-dessus du poignet.
        val fingers = listOf(
            Triple(5, 0.40f, index), Triple(9, 0.5f, middle), Triple(13, 0.60f, ring), Triple(17, 0.70f, pinky),
        )
        for ((mcp, x, up) in fingers) {
            val baseY = 0.7f
            set(mcp, x, baseY)
            if (up) {
                set(mcp + 1, x, baseY - 0.10f); set(mcp + 2, x, baseY - 0.18f); set(mcp + 3, x, baseY - 0.25f)
            } else {
                set(mcp + 1, x, baseY - 0.08f); set(mcp + 2, x, baseY - 0.02f); set(mcp + 3, x, baseY + 0.04f)
            }
        }
        // Pouce : base près du poignet côté index.
        set(1, 0.42f, 0.85f); set(2, 0.36f, 0.80f)
        if (thumb) { set(3, 0.28f, 0.74f); set(4, 0.22f, 0.68f) } else { set(3, 0.40f, 0.78f); set(4, 0.45f, 0.74f) }
        if (pinch) set(4, pts[8].x + 0.01f, pts[8].y + 0.01f)
        return Hand(pts, "Right", 0.9f)
    }
}
