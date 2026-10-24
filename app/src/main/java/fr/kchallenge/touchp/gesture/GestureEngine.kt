package fr.kchallenge.touchp.gesture

import fr.kchallenge.touchp.tracking.HandFrame
import fr.kchallenge.touchp.tracking.Side

/** Lisse les gestes et le nombre de doigts, main par main, sur plusieurs images. */
class GestureEngine(
    private val confirmFrames: Int = 4,
    /** Nombre d'images sans voir une main avant d'oublier son état. */
    private val dropFrames: Int = 8,
) {
    private class Slot(confirmFrames: Int) {
        val gesture = Debouncer(confirmFrames, Gesture.NONE)
        val fingers = Debouncer(confirmFrames, 0)
        var missing = 0
    }

    private val slots = HashMap<Side, Slot>()

    fun update(frame: HandFrame): List<HandReading> {
        // Si MediaPipe renvoie deux mains du même côté, on ne garde que la première.
        val readings = frame.hands.distinctBy { it.side }.map { hand ->
            val pose = HandAnalyzer.analyze(hand, frame.imageWidth, frame.imageHeight)
            val slot = slots.getOrPut(hand.side) { Slot(confirmFrames) }
            slot.missing = 0
            HandReading(hand.side, slot.gesture.update(pose.gesture), slot.fingers.update(pose.fingerCount))
        }
        val seen = readings.map { it.side }
        slots.entries.removeAll { (side, slot) -> side !in seen && ++slot.missing > dropFrames }
        return readings
    }

    fun reset() = slots.clear()
}
