package org.taktik.icure.utils

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

fun isXDayweekOfMonthInRange(dayOfWeek: DayOfWeek, weekNumber: Long, start: LocalDateTime, end: LocalDateTime): Boolean {
	val endM = end.minusDays(1)
	return if (start.month != endM.month) {
		getXDayweekOfMonth(dayOfWeek, weekNumber, start).let { it.isEqual(start) || it.isAfter(start) } &&
			getXDayweekOfMonth(dayOfWeek, weekNumber, start).let { it.isEqual(endM) || it.isBefore(endM) } ||
			getXDayweekOfMonth(dayOfWeek, weekNumber, endM).let { it.isEqual(start) || it.isAfter(start) } &&
			getXDayweekOfMonth(dayOfWeek, weekNumber, endM).let { it.isEqual(endM) || it.isBefore(endM) }
	} else {
		getXDayweekOfMonth(dayOfWeek, weekNumber, start).let { it.isEqual(start) || it.isAfter(start) } &&
			getXDayweekOfMonth(dayOfWeek, weekNumber, start).let { it.isEqual(endM) || it.isBefore(endM) }
	}
}

fun getXDayweekOfMonth(dayOfWeek: DayOfWeek, weekNumber: Long, date: LocalDateTime): LocalDateTime {
	var firstOfMonth = date.withDayOfMonth(1).withHour(0).withSecond(0).withNano(0)
	while (firstOfMonth.dayOfWeek != dayOfWeek) {
		firstOfMonth = firstOfMonth.plusDays(1)
	}
	return firstOfMonth.plusWeeks(weekNumber - 1)
}

@Deprecated("Is using deprecated fuzzy values")
fun Long.toEpochMillisecond() = FuzzyValues.getDateTime(this)!!.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000
@Deprecated("Is using deprecated fuzzy values")
fun Long.toLocalDateTime() = FuzzyValues.getDateTime(this)
@Deprecated("Is using deprecated fuzzy values")
fun LocalDateTime.toFuzzyLong() = FuzzyValues.getFuzzyDateTime(this, ChronoUnit.SECONDS)
