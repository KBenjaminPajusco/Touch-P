package fr.kchallenge.touchp.gesture

import fr.kchallenge.touchp.tracking.Hand
import fr.kchallenge.touchp.tracking.HandFrame
import fr.kchallenge.touchp.tracking.Side
import org.junit.Assert.assertEquals
import org.junit.Test

class GestureEngineTest {

    private fun frameOf(vararg hands: Hand) = HandFrame(hands.toList(), 100, 100)
    private val victory = SyntheticHand.build(index = true, middle = true)
    private val fist = SyntheticHand.build()

    @Test fun debouncerNeedsConsecutiveFrames() {
        val d = Debouncer(3, "a")
        assertEquals("a", d.update("b"))
        assertEquals("a", d.update("b"))
        assertEquals("b", d.update("b"))
    }

    @Test fun debouncerIgnoresOneFrameGlitch() {
        val d = Debouncer(3, "a")
        repeat(10) { assertEquals("a", d.update(if (it == 4) "b" else "a")) }
    }

    @Test fun debouncerRestartsStreakWhenCandidateChanges() {
        val d = Debouncer(3, "a")
        d.update("b"); d.update("c"); d.update("b")
        assertEquals("a", d.update("c"))
    }

    @Test fun debouncerResetForcesValueImmediately() {
        val d = Debouncer(3, "a")
        repeat(2) { d.update("b") }
        d.reset("c")
        assertEquals("c", d.stable)
        // La série en cours est oubliée : il faut à nouveau 3 images.
        assertEquals("c", d.update("b"))
    }

    @Test fun gestureAppearsAfterConfirmation() {
        val engine = GestureEngine(confirmFrames = 4)
        val results = (1..5).map { engine.update(frameOf(victory)).single().gesture }
        assertEquals(listOf(Gesture.NONE, Gesture.NONE, Gesture.NONE, Gesture.VICTORY, Gesture.VICTORY), results)
    }

    @Test fun flickerDoesNotChangeGesture() {
        val engine = GestureEngine(confirmFrames = 4)
        repeat(6) { engine.update(frameOf(victory)) }
        // Une image de poing isolée ne doit pas changer le geste.
        assertEquals(Gesture.VICTORY, engine.update(frameOf(fist)).single().gesture)
        assertEquals(Gesture.VICTORY, engine.update(frameOf(victory)).single().gesture)
    }

    @Test fun stateIsDroppedWhenHandDisappears() {
        val engine = GestureEngine(confirmFrames = 2, dropFrames = 3)
        repeat(3) { engine.update(frameOf(victory)) }
        repeat(5) { engine.update(frameOf()) }
        // Après l'oubli, on repart de zéro : il faut à nouveau confirmer.
        assertEquals(Gesture.NONE, engine.update(frameOf(victory)).single().gesture)
    }

    @Test fun twoHandsTrackedIndependently() {
        val engine = GestureEngine(confirmFrames = 1)
        val readings = engine.update(frameOf(victory.copy(side = Side.LEFT), fist.copy(side = Side.RIGHT)))
        assertEquals(2, readings.size)
        assertEquals(Gesture.VICTORY, readings.first { it.side == Side.LEFT }.gesture)
        assertEquals(Gesture.FIST, readings.first { it.side == Side.RIGHT }.gesture)
    }
}
