package fr.kchallenge.touchp.gesture

import fr.kchallenge.touchp.tracking.HandFrame

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
        val seen = HashSet<Side>()
        val readings = frame.hands.mapNotNull { hand ->
            // MediaPipe suppose une image miroir (selfie) ; nos images ne le sont pas, donc on inverse.
            val side = if (hand.label == "Left") Side.RIGHT else Side.LEFT
            if (!seen.add(side)) return@mapNotNull null // deux mains avec le même côté : on garde la première
            val pose = HandAnalyzer.analyze(hand, frame.imageWidth, frame.imageHeight)
            val slot = slots.getOrPut(side) { Slot(confirmFrames) }
            slot.missing = 0
            HandReading(side, slot.gesture.update(pose.gesture), slot.fingers.update(pose.fingerCount))
        }
        val iterator = slots.entries.iterator()
        while (iterator.hasNext()) {
            val (side, slot) = iterator.next()
            if (side !in seen && ++slot.missing > dropFrames) iterator.remove()
        }
        return readings
    }

    fun reset() = slots.clear()
}
