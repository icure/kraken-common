package org.taktik.icure.utils

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

actual typealias LocalTimeMP = LocalTime

actual typealias LocalDateMP = LocalDate

actual typealias LocalDateTimeMP = LocalDateTime

// Order is important, refer to expect declaration documentation
actual enum class ChronoUnitMP {
	SECONDS,
	MINUTES,
	HOURS,
	DAYS,
	MONTHS,
	YEARS
}

internal actual object TimeUtilsMP {
	actual fun createLocalTime(
		hours: Int,
		minutes: Int,
		seconds: Int
	): LocalTimeMP =
		LocalTime(hours, minutes, seconds)

	actual fun tryCreateLocalDate(
		year: Int,
		month: Int,
		day: Int
	): LocalDateMP? = try {
		LocalDate(year, month, day)
	} catch (_: IllegalArgumentException) {
		null
	}

	actual fun createLocalDateTime(
		date: LocalDateMP,
		time: LocalTimeMP
	): LocalDateTimeMP =
		LocalDateTime(date, time)

	@OptIn(ExperimentalTime::class)
	actual fun utcLocalDatetimeAtTimestamp(epochTimestamp: Long): LocalDateTimeMP =
		Instant.fromEpochMilliseconds(epochTimestamp).toLocalDateTime(kotlinx.datetime.TimeZone.UTC)

	actual val MIDNIGHT_TIME: LocalTimeMP = LocalTime(0, 0, 0)

	actual fun plusDays(
		date: LocalDateMP,
		amount: Long
	): LocalDateMP =
		date.plus(amount, DateTimeUnit.DAY)

	actual fun minusDays(
		datetime: LocalDateTimeMP,
		amount: Long
	): LocalDateTimeMP = TODO()

	actual fun minusMinutes(
		datetime: LocalDateTimeMP,
		amount: Long
	): LocalDateTimeMP {
		TODO("Not yet implemented")
	}

	actual fun minusHours(
		datetime: LocalDateTimeMP,
		amount: Long
	): LocalDateTimeMP {
		TODO("Not yet implemented")
	}

	actual fun hourOf(time: LocalTimeMP): Int =
		time.hour

	actual fun hourOf(time: LocalDateTimeMP): Int =
		time.hour

	actual fun minuteOf(time: LocalTimeMP): Int =
		time.minute

	actual fun minuteOf(time: LocalDateTimeMP): Int =
		time.minute

	actual fun secondOf(time: LocalTimeMP): Int =
		time.second

	actual fun secondOf(time: LocalDateTimeMP): Int =
		time.second

	actual fun dateOf(dateTime: LocalDateTimeMP): LocalDateMP =
		dateTime.date

	actual fun yearOf(date: LocalDateMP): Int =
		date.year

	actual fun monthOf(date: LocalDateMP): Int =
		date.month.number

	actual fun dayOf(date: LocalDateMP): Int =
		date.day

	actual fun nowLocalTime(zone: String?): LocalTimeMP =
		nowLocalDateTime(zone).time

	actual fun nowLocalDate(zone: String?): LocalDateMP =
		nowLocalDateTime(zone).date

	@OptIn(ExperimentalTime::class)
	actual fun nowLocalDateTime(zone: String?): LocalDateTimeMP =
		Clock.System.now().toLocalDateTime(zone?.let { TimeZone.of(it) } ?: TimeZone.UTC)
}