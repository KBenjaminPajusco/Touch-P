package fr.kchallenge.touchp.tracking

import org.json.JSONObject

/** Lit les `assets/debug/<image>.json` produits par scripts/gen_landmarks.py. */
object HandJson {
    fun parse(text: String): HandFrame {
        val json = JSONObject(text)
        val arr = json.getJSONArray("hands")
        val hands = List(arr.length()) { i ->
            val h = arr.getJSONObject(i)
            val pts = h.getJSONArray("landmarks")
            Hand(
                landmarks = List(pts.length()) { j ->
                    val p = pts.getJSONArray(j)
                    Landmark(p.getDouble(0).toFloat(), p.getDouble(1).toFloat(), p.getDouble(2).toFloat())
                },
                side = Side.fromMediaPipe(h.getString("label")),
                score = h.getDouble("score").toFloat(),
            )
        }
        return HandFrame(hands, json.getInt("width"), json.getInt("height"))
    }
}
