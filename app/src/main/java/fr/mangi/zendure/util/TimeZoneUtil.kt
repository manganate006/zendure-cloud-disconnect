package fr.mangi.zendure.util

import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

object TimeZoneUtil {

    private val FORMAT = Regex("""^GMT[+-]\d{2}:\d{2}$""")

    /**
     * Offset courant (DST inclus) au format attendu par le firmware : "GMT+02:00".
     */
    fun currentGmtOffset(
        timeZone: TimeZone = TimeZone.getDefault(),
        nowMillis: Long = System.currentTimeMillis(),
    ): String {
        val totalMinutes = timeZone.getOffset(nowMillis) / 60_000
        val sign = if (totalMinutes < 0) "-" else "+"
        val absMinutes = abs(totalMinutes)
        return String.format(Locale.US, "GMT%s%02d:%02d", sign, absMinutes / 60, absMinutes % 60)
    }

    fun isValid(value: String): Boolean = FORMAT.matches(value)
}
