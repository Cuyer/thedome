package pl.cuyer.thedome.domain.server

import kotlinx.serialization.Serializable
import pl.cuyer.thedome.domain.rust.Wipe

@Serializable
enum class WipeSchedule {
    WEEKLY,
    BIWEEKLY,
    MONTHLY;

    companion object {
        /**
         * Picks a schedule based on the “weeks” flags:
         *  - 1 flagged week → MONTHLY
         *  - 5 flagged weeks → WEEKLY
         *  - 10 flagged weeks → BIWEEKLY
         *  - anything else → null
         */
        fun from(wipes: List<Wipe>): WipeSchedule? {
            // no data → can’t pick a schedule
            val spec = wipes.firstOrNull() ?: return null

            // sum of your [1,0,1,1,0]-style flags
            val flags = spec.weeks.sum()

            return when (flags) {
                1 -> MONTHLY
                5 -> WEEKLY
                10 -> BIWEEKLY
                else -> null
            }
        }
    }
}
