package fr.kchallenge.touchp.gesture

import fr.kchallenge.touchp.tracking.Hand
import fr.kchallenge.touchp.tracking.Lm
import kotlin.math.hypot

/**
 * Comptage de doigts et classification de gestes, image par image, à partir des 21 landmarks.
 * Tout est exprimé en fraction de la taille de la main (poignet → base du majeur), donc
 * indépendant de la distance à la caméra et de la rotation de la main dans l'image.
 */
object HandAnalyzer {

    /** Un doigt est levé si son bout dépasse l'articulation PIP de plus de cette fraction de la main. */
    const val FINGER_MARGIN = 0.1f

    /** Le pouce est levé s'il s'écarte de la base de l'index d'au moins cette fraction. */
    const val THUMB_SPREAD = 0.6f

    /** Pincement : bouts du pouce et de l'index plus proches que cette fraction. */
    const val PINCH_DISTANCE = 0.25f

    /** Un poing a aussi le pouce contre l'index : pour un pincement, le bout de l'index doit rester loin du poignet. */
    const val PINCH_INDEX_REACH = 1.1f

    private class Pt(val x: Float, val y: Float)

    fun analyze(hand: Hand, imageWidth: Int, imageHeight: Int): HandPose {
        // Les coordonnées sont normalisées par axe : on repasse en pixels pour ne pas déformer les distances.
        val p = hand.landmarks.map { Pt(it.x * imageWidth, it.y * imageHeight) }
        fun d(a: Int, b: Int) = hypot(p[a].x - p[b].x, p[a].y - p[b].y)
        val size = d(Lm.WRIST, Lm.MIDDLE_MCP).coerceAtLeast(1e-3f)

        fun curled(pip: Int, tip: Int) = d(tip, Lm.WRIST) <= d(pip, Lm.WRIST) + FINGER_MARGIN * size

        val extended = buildSet {
            if (d(Lm.THUMB_TIP, Lm.PINKY_MCP) > d(Lm.THUMB_IP, Lm.PINKY_MCP) &&
                d(Lm.THUMB_TIP, Lm.INDEX_MCP) > THUMB_SPREAD * size
            ) add(Finger.THUMB)
            if (!curled(Lm.INDEX_PIP, Lm.INDEX_TIP)) add(Finger.INDEX)
            if (!curled(Lm.MIDDLE_PIP, Lm.MIDDLE_TIP)) add(Finger.MIDDLE)
            if (!curled(Lm.RING_PIP, Lm.RING_TIP)) add(Finger.RING)
            if (!curled(Lm.PINKY_PIP, Lm.PINKY_TIP)) add(Finger.PINKY)
        }

        val others = extended - Finger.THUMB
        val gesture = when {
            d(Lm.THUMB_TIP, Lm.INDEX_TIP) < PINCH_DISTANCE * size &&
                d(Lm.INDEX_TIP, Lm.WRIST) > PINCH_INDEX_REACH * size -> Gesture.PINCH
            others.size == 4 -> Gesture.OPEN_PALM
            others == setOf(Finger.INDEX, Finger.MIDDLE) -> Gesture.VICTORY
            others == setOf(Finger.INDEX) && Finger.THUMB in extended -> Gesture.GUN
            others.isEmpty() && Finger.THUMB in extended ->
                // Pouce levé seulement si le bout du pouce est au-dessus du poignet à l'écran.
                if (p[Lm.THUMB_TIP].y < p[Lm.WRIST].y - size * 0.5f) Gesture.THUMBS_UP else Gesture.NONE
            others.isEmpty() -> Gesture.FIST
            else -> Gesture.NONE
        }
        return HandPose(extended, gesture)
    }
}
