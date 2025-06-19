package pl.cuyer.thedome.domain.server

import kotlinx.serialization.Serializable
import pl.cuyer.thedome.domain.rust.Wipe
import kotlin.math.abs

@Serializable
enum class WipeSchedule {
    WEEKLY,
    BIWEEKLY,
    MONTHLY;

    companion object {
        /**
         * Picks a schedule based on a series of month-by-month “weeks” flags:
         *  - average ≲ 1.5 flags/month → MONTHLY
         *  - average ≳ 3.5 flags/month → WEEKLY
         *  - otherwise → BIWEEKLY
         *
         * Also verifies that the “biweekly” case really alternates roughly every other week.
         */
        fun from(wipes: List<Wipe>): WipeSchedule? {
            if (wipes.isEmpty()) return null

            // total number of flagged weeks across all recorded months
            val totalFlags = wipes.sumOf { it.weeks.sum() }

            // compute average flags per month
            val avgPerMonth = totalFlags.toDouble() / wipes.size

            return when {
                avgPerMonth < 1.5 -> MONTHLY
                avgPerMonth >= 3.5 -> WEEKLY
                else -> {
                    // before declaring biweekly, double-check that flags alternate
                    if (wipes.all { it.weeks.isAlternatingBiweekly() }) BIWEEKLY
                    else null
                }
            }
        }

        /**
         * Heuristic: true if roughly every other week is flagged.
         * e.g. [1,0,1,0,1], [0,1,0,1] → true
         */
        private fun List<Int>.isAlternatingBiweekly(): Boolean {
            val idx = mapIndexedNotNull { i, v -> if (v == 1) i else null }
            if (idx.size < 2) return false
            val diffs = idx.zipWithNext { a, b -> b - a }
            // expect most diffs to be 2 (allow ±1 for 5-week months)
            return diffs.all { abs(it - 2) <= 1 }
        }
    }
}
