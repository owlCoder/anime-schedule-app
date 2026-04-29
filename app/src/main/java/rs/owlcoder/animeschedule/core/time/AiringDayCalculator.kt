package rs.owlcoder.animeschedule.core.time

import java.time.LocalDate
import java.time.ZoneId

enum class AiringDay { TODAY, TOMORROW, THIS_WEEK, OTHER }

fun classifyAiringDay(airingAtEpochSeconds: Long, zoneId: ZoneId = ZoneId.systemDefault()): AiringDay {
    val airingDate = epochSecondsToLocalDate(airingAtEpochSeconds, zoneId)
    val today = LocalDate.now(zoneId)
    return when (airingDate) {
        today -> AiringDay.TODAY
        today.plusDays(1) -> AiringDay.TOMORROW
        else -> if (!airingDate.isBefore(today) && airingDate.isBefore(today.plusDays(7)))
            AiringDay.THIS_WEEK else AiringDay.OTHER
    }
}
