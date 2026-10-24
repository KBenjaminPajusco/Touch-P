package fr.kchallenge.touchp.tracking

import fr.kchallenge.touchp.gesture.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Test

class HandJsonTest {
    @Test fun mediaPipeHandednessIsInvertedForNonMirroredImages() {
        assertEquals(Side.RIGHT, Side.fromMediaPipe("Left"))
        assertEquals(Side.LEFT, Side.fromMediaPipe("Right"))
        assertEquals(Side.LEFT, Side.fromMediaPipe(null))
    }

    @Test fun parsesFixtureWithSizeAndHands() {
        val frame = Fixtures.frame("two_hands")
        assertEquals(2, frame.hands.size)
        assertEquals(21, frame.hands[0].landmarks.size)
        assertEquals(setOf(Side.LEFT, Side.RIGHT), frame.hands.map { it.side }.toSet())
        assertEquals(640 to 960, frame.imageWidth to frame.imageHeight)
    }
}
