package fr.mangi.zendure

import fr.mangi.zendure.util.TimeZoneUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class TimeZoneUtilTest {

    @Test
    fun `offset positif`() {
        assertEquals("GMT+02:00", TimeZoneUtil.currentGmtOffset(TimeZone.getTimeZone("GMT+02:00")))
    }

    @Test
    fun `offset negatif`() {
        assertEquals("GMT-05:00", TimeZoneUtil.currentGmtOffset(TimeZone.getTimeZone("GMT-05:00")))
    }

    @Test
    fun `offset avec demi-heure`() {
        assertEquals("GMT+05:30", TimeZoneUtil.currentGmtOffset(TimeZone.getTimeZone("GMT+05:30")))
    }

    @Test
    fun `UTC donne GMT plus zero`() {
        assertEquals("GMT+00:00", TimeZoneUtil.currentGmtOffset(TimeZone.getTimeZone("UTC")))
    }

    @Test
    fun `paris en ete vaut GMT plus deux`() {
        // 15 juillet 2026 : heure d'été (CEST)
        val paris = TimeZone.getTimeZone("Europe/Paris")
        assertEquals("GMT+02:00", TimeZoneUtil.currentGmtOffset(paris, 1784073600000L))
    }

    @Test
    fun `validation du format`() {
        assertTrue(TimeZoneUtil.isValid("GMT+02:00"))
        assertTrue(TimeZoneUtil.isValid("GMT-11:30"))
        assertFalse(TimeZoneUtil.isValid("GMT+2:00"))
        assertFalse(TimeZoneUtil.isValid("UTC+02:00"))
        assertFalse(TimeZoneUtil.isValid("GMT+02"))
        assertFalse(TimeZoneUtil.isValid(""))
    }
}
