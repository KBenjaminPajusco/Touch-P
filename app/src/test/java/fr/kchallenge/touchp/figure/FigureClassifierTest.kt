package fr.kchallenge.touchp.figure

import fr.kchallenge.touchp.tracking.Landmark
import fr.kchallenge.touchp.tracking.PoseLm
import org.junit.Assert.assertEquals
import org.junit.Test

class FigureClassifierTest {

    /** Squelette synthétique (image carrée) : points posés un par un, visibilité 1 par défaut. */
    private class Body {
        val pts = MutableList(PoseLm.COUNT) { Landmark(0.5f, 0.5f, 0f) }
        fun set(i: Int, x: Double, y: Double, visibility: Float = 1f) {
            pts[i] = Landmark(x.toFloat(), y.toFloat(), 0f, visibility)
        }
        /** Chaîne épaule-coude-poignet d'un côté, alignée de (x0,y0) vers (x1,y1). */
        fun arm(left: Boolean, from: Pair<Double, Double>, to: Pair<Double, Double>, bend: Double = 0.0) {
            val (s, e, w) = if (left) Triple(PoseLm.L_SHOULDER, PoseLm.L_ELBOW, PoseLm.L_WRIST)
            else Triple(PoseLm.R_SHOULDER, PoseLm.R_ELBOW, PoseLm.R_WRIST)
            set(s, from.first, from.second)
            set(e, (from.first + to.first) / 2 + bend, (from.second + to.second) / 2)
            set(w, to.first, to.second)
        }
        /** Chaîne hanche-genou-cheville alignée. */
        fun leg(left: Boolean, from: Pair<Double, Double>, to: Pair<Double, Double>) {
            val (h, k, a) = if (left) Triple(PoseLm.L_HIP, PoseLm.L_KNEE, PoseLm.L_ANKLE)
            else Triple(PoseLm.R_HIP, PoseLm.R_KNEE, PoseLm.R_ANKLE)
            set(h, from.first, from.second)
            set(k, (from.first + to.first) / 2, (from.second + to.second) / 2)
            set(a, to.first, to.second)
        }
        fun classify() = FigureClassifier.classify(pts, 100, 100)
    }

    private fun xy(x: Double, y: Double) = x to y

    private fun standingBody() = Body().apply {
        arm(true, xy(0.45, 0.3), xy(0.45, 0.6)); arm(false, xy(0.55, 0.3), xy(0.55, 0.6))
        leg(true, xy(0.47, 0.55), xy(0.47, 0.9)); leg(false, xy(0.53, 0.55), xy(0.53, 0.9))
    }

    private fun handstandBody() = Body().apply {
        arm(true, xy(0.45, 0.6), xy(0.45, 0.9)); arm(false, xy(0.55, 0.6), xy(0.55, 0.9))
        leg(true, xy(0.47, 0.35), xy(0.47, 0.05)); leg(false, xy(0.53, 0.35), xy(0.53, 0.05))
    }

    /** Corps horizontal de profil : épaules à x=0.2, pieds à x=0.8. */
    private fun horizontalBody(wristY: Double) = Body().apply {
        arm(true, xy(0.2, 0.5), xy(0.2, wristY)); arm(false, xy(0.22, 0.5), xy(0.22, wristY))
        leg(true, xy(0.5, 0.5), xy(0.8, 0.5)); leg(false, xy(0.5, 0.5), xy(0.8, 0.5))
    }

    @Test fun standingIsNotAFigure() = assertEquals(Figure.NONE, standingBody().classify())

    @Test fun handstand() = assertEquals(Figure.HANDSTAND, handstandBody().classify())

    @Test fun planche() = assertEquals(Figure.PLANCHE, horizontalBody(wristY = 0.85).classify())

    @Test fun lever() = assertEquals(Figure.LEVER, horizontalBody(wristY = 0.15).classify())

    @Test fun humanFlag() {
        val body = Body().apply {
            arm(true, xy(0.2, 0.5), xy(0.2, 0.15)); arm(false, xy(0.22, 0.5), xy(0.22, 0.85))
            leg(true, xy(0.5, 0.5), xy(0.8, 0.5)); leg(false, xy(0.5, 0.5), xy(0.8, 0.5))
        }
        assertEquals(Figure.HUMAN_FLAG, body.classify())
    }

    @Test fun bentArmsAreNotAPlanche() {
        val body = horizontalBody(wristY = 0.85).apply {
            arm(true, xy(0.2, 0.5), xy(0.2, 0.85), bend = 0.12); arm(false, xy(0.22, 0.5), xy(0.22, 0.85), bend = 0.12)
        }
        assertEquals(Figure.NONE, body.classify())
    }

    @Test fun bentBodyIsNotAHandstand() {
        val body = handstandBody().apply { leg(true, xy(0.47, 0.35), xy(0.8, 0.3)); leg(false, xy(0.53, 0.35), xy(0.8, 0.3)) }
        assertEquals(Figure.NONE, body.classify())
    }

    @Test fun lowVisibilityGivesNothing() {
        val body = handstandBody().apply { set(PoseLm.L_ANKLE, 0.47, 0.05, visibility = 0.1f) }
        assertEquals(Figure.NONE, body.classify())
    }

    @Test fun flippedHandstandIsJustStanding() {
        // Un handstand retourné (tête en haut) est un corps debout : jamais un handstand.
        val flipped = handstandBody().pts.map { it.copy(y = 1f - it.y) }
        assertEquals(Figure.NONE, FigureClassifier.classify(flipped, 100, 100))
    }
}
