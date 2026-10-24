package fr.kchallenge.touchp.gesture

/**
 * Valeur stable : ne change que si la nouvelle valeur est vue [confirmFrames] fois de suite.
 * Évite le clignotement quand la classification oscille d'une image à l'autre.
 */
class Debouncer<T>(private val confirmFrames: Int, initial: T) {
    var stable: T = initial
        private set
    private var candidate: T = initial
    private var streak = 0

    fun reset(value: T) {
        stable = value
        candidate = value
        streak = 0
    }

    fun update(value: T): T {
        if (value == stable) {
            streak = 0
        } else {
            streak = if (value == candidate) streak + 1 else 1
            candidate = value
            if (streak >= confirmFrames) {
                stable = value
                streak = 0
            }
        }
        return stable
    }
}
