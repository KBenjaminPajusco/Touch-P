package fr.kchallenge.touchp.gesture

import fr.kchallenge.touchp.tracking.Side

enum class Finger { THUMB, INDEX, MIDDLE, RING, PINKY }

enum class Gesture(val label: String) {
    NONE("—"),
    FIST("Poing"),
    OPEN_PALM("Main ouverte"),
    THUMBS_UP("Pouce levé"),
    VICTORY("Victoire"),
    GUN("Pistolet"),
    PINCH("Pincement"),
}

/** Analyse d'une seule image (non lissée). */
data class HandPose(val extended: Set<Finger>, val gesture: Gesture) {
    val fingerCount: Int get() = extended.size
}

/** Résultat lissé pour une main, prêt à afficher. */
data class HandReading(val side: Side, val gesture: Gesture, val fingerCount: Int)
