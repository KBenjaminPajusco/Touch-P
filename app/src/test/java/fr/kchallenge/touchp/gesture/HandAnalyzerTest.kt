package fr.kchallenge.touchp.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HandAnalyzerTest {

    private fun synthetic(vararg args: Pair<String, Boolean>): HandPose {
        val m = args.toMap()
        val hand = SyntheticHand.build(
            thumb = m["thumb"] == true, index = m["index"] == true, middle = m["middle"] == true,
            ring = m["ring"] == true, pinky = m["pinky"] == true, pinch = m["pinch"] == true,
        )
        return HandAnalyzer.analyze(hand, 100, 100)
    }

    // --- Vraies photos ---

    @Test fun photoVictory() {
        val pose = Fixtures.analyze("victory")
        assertEquals(setOf(Finger.INDEX, Finger.MIDDLE), pose.extended)
        assertEquals(Gesture.VICTORY, pose.gesture)
    }

    @Test fun photoThumbUp() {
        val pose = Fixtures.analyze("thumb_up")
        assertEquals(setOf(Finger.THUMB), pose.extended)
        assertEquals(Gesture.THUMBS_UP, pose.gesture)
    }

    @Test fun photoPointingUp() {
        val pose = Fixtures.analyze("pointing_up")
        assertTrue(Finger.INDEX in pose.extended)
        assertEquals(1, pose.extended.size - (if (Finger.THUMB in pose.extended) 1 else 0))
    }

    @Test fun photoFist() {
        val pose = Fixtures.analyze("fist")
        assertEquals(emptySet<Finger>(), pose.extended - Finger.THUMB)
        assertEquals(Gesture.FIST, pose.gesture)
    }

    // --- Mains synthétiques ---

    @Test fun syntheticOpenPalm() {
        val pose = synthetic("thumb" to true, "index" to true, "middle" to true, "ring" to true, "pinky" to true)
        assertEquals(5, pose.fingerCount)
        assertEquals(Gesture.OPEN_PALM, pose.gesture)
    }

    @Test fun syntheticFist() {
        val pose = synthetic()
        assertEquals(0, pose.fingerCount)
        assertEquals(Gesture.FIST, pose.gesture)
    }

    @Test fun syntheticGun() {
        val pose = synthetic("thumb" to true, "index" to true)
        assertEquals(2, pose.fingerCount)
        assertEquals(Gesture.GUN, pose.gesture)
    }

    @Test fun syntheticVictory() {
        val pose = synthetic("index" to true, "middle" to true)
        assertEquals(Gesture.VICTORY, pose.gesture)
    }

    @Test fun syntheticPinch() {
        val pose = synthetic("thumb" to true, "index" to true, "pinch" to true)
        assertEquals(Gesture.PINCH, pose.gesture)
    }

    @Test fun rotationDoesNotChangeFingerCount() {
        // Même main tournée de 90° : comptage inchangé (les distances ne dépendent pas de l'orientation).
        val hand = SyntheticHand.build(index = true, middle = true)
        val rotated = hand.copy(landmarks = hand.landmarks.map { it.copy(x = it.y, y = 1f - it.x) })
        assertEquals(
            HandAnalyzer.analyze(hand, 100, 100).extended,
            HandAnalyzer.analyze(rotated, 100, 100).extended,
        )
    }
}
