package pl.cuyer.thedome.domain.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import pl.cuyer.thedome.domain.rust.Wipe

class WipeScheduleTest {
    @Test
    fun `empty list returns null`() {
        assertNull(WipeSchedule.from(emptyList()))
    }

    @Test
    fun `one flag returns MONTHLY`() {
        val wipes = listOf(Wipe(weeks = listOf(1)))
        assertEquals(WipeSchedule.MONTHLY, WipeSchedule.from(wipes))
    }

    @Test
    fun `five flags returns WEEKLY`() {
        val wipes = listOf(Wipe(weeks = listOf(1,1,1,1,1)))
        assertEquals(WipeSchedule.WEEKLY, WipeSchedule.from(wipes))
    }

    @Test
    fun `alternating flags return BIWEEKLY`() {
        val wipes = List(4) { Wipe(weeks = listOf(1,0,1,0)) }
        assertEquals(WipeSchedule.BIWEEKLY, WipeSchedule.from(wipes))
    }

    @Test
    fun `other flag counts return null`() {
        val wipes = listOf(Wipe(weeks = listOf(1,0,0,0,1)))
        assertNull(WipeSchedule.from(wipes))
    }
}
