package fr.kchallenge.touchp.figure

import fr.kchallenge.touchp.tracking.Landmark
import fr.kchallenge.touchp.tracking.PoseLm
import fr.kchallenge.touchp.tracking.isVisible
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.PI

/**
 * Reconnaissance de figures statiques à partir des angles du corps, en 2D image (pas de 3D) :
 * une figure doit donc être filmée de profil (planche, lever) ou de face (human flag).
 * Le lever avant et arrière ont la même silhouette de profil : ils ne sont pas distingués.
 * "Haut" et "bas" sont ceux de l'image redressée : l'app est verrouillée en portrait, donc ils
 * suivent la gravité tant que le téléphone est tenu droit (tolérance ~25° de roulis).
 */
object FigureClassifier {

    /** Angle minimal (degrés) aux hanches et aux genoux pour un corps gainé. */
    private const val STRAIGHT_BODY = 155.0

    /** Angle minimal (degrés) aux coudes pour des bras tendus. */
    private const val STRAIGHT_ARM = 150.0

    /** Écart maximal à l'horizontale (degrés) du corps pour planche, lever et drapeau. */
    private const val HORIZONTAL_MAX = 25.0

    /** Écart minimal à l'horizontale (degrés) du corps pour un handstand. */
    private const val VERTICAL_MIN = 65.0

    /** Les poignets doivent être au-dessus/au-dessous des épaules d'au moins cette fraction de la longueur du bras. */
    private const val WRIST_OFFSET = 0.5

    private val REQUIRED = listOf(
        PoseLm.L_SHOULDER, PoseLm.R_SHOULDER, PoseLm.L_ELBOW, PoseLm.R_ELBOW, PoseLm.L_WRIST, PoseLm.R_WRIST,
        PoseLm.L_HIP, PoseLm.R_HIP, PoseLm.L_KNEE, PoseLm.R_KNEE, PoseLm.L_ANKLE, PoseLm.R_ANKLE,
    )

    private class P(val x: Double, val y: Double)

    fun classify(landmarks: List<Landmark>, imageWidth: Int, imageHeight: Int): Figure {
        if (landmarks.size < PoseLm.COUNT || REQUIRED.any { !landmarks[it].isVisible }) return Figure.NONE

        // Pixels, pour ne pas déformer angles et distances sur une image non carrée.
        val pts = Array(PoseLm.COUNT) { P(landmarks[it].x.toDouble() * imageWidth, landmarks[it].y.toDouble() * imageHeight) }
        fun p(i: Int) = pts[i]
        fun mid(a: Int, b: Int) = P((p(a).x + p(b).x) / 2, (p(a).y + p(b).y) / 2)
        fun dist(a: P, b: P) = hypot(a.x - b.x, a.y - b.y)
        fun angle(a: Int, b: Int, c: Int): Double {
            val ux = p(a).x - p(b).x; val uy = p(a).y - p(b).y
            val vx = p(c).x - p(b).x; val vy = p(c).y - p(b).y
            val norm = hypot(ux, uy) * hypot(vx, vy)
            return if (norm < 1e-6) 180.0 else acos(((ux * vx + uy * vy) / norm).coerceIn(-1.0, 1.0)) * 180 / PI
        }

        val shoulders = mid(PoseLm.L_SHOULDER, PoseLm.R_SHOULDER)
        val ankles = mid(PoseLm.L_ANKLE, PoseLm.R_ANKLE)
        val wrists = mid(PoseLm.L_WRIST, PoseLm.R_WRIST)
        if (dist(shoulders, ankles) < 1e-3) return Figure.NONE

        // 0° = corps horizontal, 90° = vertical.
        val tilt = atan2(abs(ankles.y - shoulders.y), abs(ankles.x - shoulders.x)) * 180 / PI

        val straightBody = listOf(
            angle(PoseLm.L_SHOULDER, PoseLm.L_HIP, PoseLm.L_KNEE), angle(PoseLm.R_SHOULDER, PoseLm.R_HIP, PoseLm.R_KNEE),
            angle(PoseLm.L_HIP, PoseLm.L_KNEE, PoseLm.L_ANKLE), angle(PoseLm.R_HIP, PoseLm.R_KNEE, PoseLm.R_ANKLE),
        ).all { it >= STRAIGHT_BODY }
        val armsStraight = listOf(
            angle(PoseLm.L_SHOULDER, PoseLm.L_ELBOW, PoseLm.L_WRIST), angle(PoseLm.R_SHOULDER, PoseLm.R_ELBOW, PoseLm.R_WRIST),
        ).all { it >= STRAIGHT_ARM }
        if (!straightBody || !armsStraight) return Figure.NONE

        val armLength = dist(p(PoseLm.L_SHOULDER), p(PoseLm.L_ELBOW)) + dist(p(PoseLm.L_ELBOW), p(PoseLm.L_WRIST))
        fun below(wrist: Int) = p(wrist).y - shoulders.y > WRIST_OFFSET * armLength
        fun above(wrist: Int) = shoulders.y - p(wrist).y > WRIST_OFFSET * armLength
        val leftBelow = below(PoseLm.L_WRIST)
        val leftAbove = above(PoseLm.L_WRIST)
        val rightBelow = below(PoseLm.R_WRIST)
        val rightAbove = above(PoseLm.R_WRIST)

        val horizontal = tilt <= HORIZONTAL_MAX
        val flag = (leftAbove && rightBelow) || (leftBelow && rightAbove)
        val wristsBelow = leftBelow && rightBelow
        val wristsAbove = leftAbove && rightAbove

        return when {
            horizontal && flag -> Figure.HUMAN_FLAG
            tilt >= VERTICAL_MIN && ankles.y < shoulders.y && wristsBelow -> Figure.HANDSTAND
            horizontal && wristsBelow && abs(wrists.x - shoulders.x) < WRIST_OFFSET * armLength -> Figure.PLANCHE
            horizontal && wristsAbove -> Figure.LEVER
            else -> Figure.NONE
        }
    }
}
