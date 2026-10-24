package fr.kchallenge.touchp.gesture

import org.junit.Assert.assertEquals
import org.junit.Test

class HandAnalyzerTest {

    private fun analyze(hand: fr.kchallenge.touchp.tracking.Hand) = HandAnalyzer.analyze(hand, 100, 100)

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
        assertEquals(setOf(Finger.INDEX), pose.extended - Finger.THUMB)
    }

    @Test fun photoFist() {
        val pose = Fixtures.analyze("fist")
        assertEquals(emptySet<Finger>(), pose.extended - Finger.THUMB)
        assertEquals(Gesture.FIST, pose.gesture)
    }

    // --- Mains synthétiques ---

    @Test fun syntheticOpenPalm() {
        val pose = analyze(SyntheticHand.build(thumb = true, index = true, middle = true, ring = true, pinky = true))
        assertEquals(5, pose.fingerCount)
        assertEquals(Gesture.OPEN_PALM, pose.gesture)
    }

    @Test fun syntheticFist() {
        val pose = analyze(SyntheticHand.build())
        assertEquals(0, pose.fingerCount)
        assertEquals(Gesture.FIST, pose.gesture)
    }

    @Test fun syntheticGun() {
        val pose = analyze(SyntheticHand.build(thumb = true, index = true))
        assertEquals(2, pose.fingerCount)
        assertEquals(Gesture.GUN, pose.gesture)
    }

    @Test fun syntheticVictory() {
        val pose = analyze(SyntheticHand.build(index = true, middle = true))
        assertEquals(Gesture.VICTORY, pose.gesture)
    }

    @Test fun syntheticPinch() {
        val pose = analyze(SyntheticHand.build(thumb = true, index = true, pinch = true))
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
