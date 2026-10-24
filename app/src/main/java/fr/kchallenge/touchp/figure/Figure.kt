package fr.kchallenge.touchp.figure

/** Figures statiques de street workout reconnues à partir de la pose du corps. */
enum class Figure(val label: String) {
    NONE("—"),
    HANDSTAND("Handstand"),
    PLANCHE("Planche"),
    LEVER("Lever (front/back)"),
    HUMAN_FLAG("Human flag"),
}
