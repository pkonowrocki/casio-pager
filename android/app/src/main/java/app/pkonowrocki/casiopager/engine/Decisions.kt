package app.pkonowrocki.casiopager.engine

import app.pkonowrocki.casiopager.model.DecisionEntry

const val DECISIONS_KEPT = 30

/** Adds [entry] at the front and keeps the newest [DECISIONS_KEPT]. */
fun pushDecision(list: List<DecisionEntry>, entry: DecisionEntry): List<DecisionEntry> =
    (listOf(entry) + list).take(DECISIONS_KEPT)
