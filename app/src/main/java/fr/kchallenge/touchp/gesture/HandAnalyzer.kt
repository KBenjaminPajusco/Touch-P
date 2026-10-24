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
    private const val FINGER_MARGIN = 0.1f

    /** Le pouce est levé s'il s'écarte de la base de l'index d'au moins cette fraction. */
    private const val THUMB_SPREAD = 0.6f

    /** Pincement : bouts du pouce et de l'index plus proches que cette fraction. */
    private const val PINCH_DISTANCE = 0.25f

    /** Un poing a aussi le pouce contre l'index : pour un pincement, le bout de l'index doit rester loin du poignet. */
    private const val PINCH_INDEX_REACH = 1.1f

    private val LONG_FINGERS = listOf(
        Triple(Finger.INDEX, Lm.INDEX_PIP, Lm.INDEX_TIP),
        Triple(Finger.MIDDLE, Lm.MIDDLE_PIP, Lm.MIDDLE_TIP),
        Triple(Finger.RING, Lm.RING_PIP, Lm.RING_TIP),
        Triple(Finger.PINKY, Lm.PINKY_PIP, Lm.PINKY_TIP),
    )

    fun analyze(hand: Hand, imageWidth: Int, imageHeight: Int): HandPose {
        val lm = hand.landmarks
        // Coordonnées normalisées par axe : on repasse en pixels pour ne pas déformer les distances.
        fun d(a: Int, b: Int) = hypot((lm[a].x - lm[b].x) * imageWidth, (lm[a].y - lm[b].y) * imageHeight)
        val size = d(Lm.WRIST, Lm.MIDDLE_MCP).coerceAtLeast(1e-3f)

        val extended = buildSet {
            if (d(Lm.THUMB_TIP, Lm.PINKY_MCP) > d(Lm.THUMB_IP, Lm.PINKY_MCP) &&
                d(Lm.THUMB_TIP, Lm.INDEX_MCP) > THUMB_SPREAD * size
            ) add(Finger.THUMB)
            for ((finger, pip, tip) in LONG_FINGERS) {
                if (d(tip, Lm.WRIST) > d(pip, Lm.WRIST) + FINGER_MARGIN * size) add(finger)
            }
        }

        val thumb = Finger.THUMB in extended
        val others = extended - Finger.THUMB
        val thumbAboveWrist = lm[Lm.THUMB_TIP].y * imageHeight < lm[Lm.WRIST].y * imageHeight - size * 0.5f
        val gesture = when {
            d(Lm.THUMB_TIP, Lm.INDEX_TIP) < PINCH_DISTANCE * size &&
                d(Lm.INDEX_TIP, Lm.WRIST) > PINCH_INDEX_REACH * size -> Gesture.PINCH
            others.size == 4 -> Gesture.OPEN_PALM
            others == setOf(Finger.INDEX, Finger.MIDDLE) -> Gesture.VICTORY
            others == setOf(Finger.INDEX) && thumb -> Gesture.GUN
            others.isEmpty() && thumb && thumbAboveWrist -> Gesture.THUMBS_UP
            others.isEmpty() && !thumb -> Gesture.FIST
            else -> Gesture.NONE
        }
        return HandPose(extended, gesture)
    }
}
