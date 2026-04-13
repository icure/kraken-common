package org.taktik.icure.utils

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.FixedOffsetTimeZone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.minus
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
		Instant.fromEpochMilliseconds(epochTimestamp).toLocalDateTime(TimeZone.UTC)

	actual val MIDNIGHT_TIME: LocalTimeMP = LocalTime(0, 0, 0)

	actual fun plusOneDay(
		date: LocalDateMP,
	): LocalDateMP =
		date.plus(1, DateTimeUnit.DAY)

	actual fun minusOneDay(
		datetime: LocalDateTimeMP,
	): LocalDateTimeMP = LocalDateTime(
		datetime.date.minus(1, DateTimeUnit.DAY),
		datetime.time
	)

	actual fun minusOneMinute(
		datetime: LocalDateTimeMP,
	): LocalDateTimeMP {
		val newMinute = datetime.minute - 1
		return if (newMinute >= 0) {
			LocalDateTime(datetime.date, LocalTime(datetime.hour, newMinute, datetime.second, datetime.nanosecond))
		} else {
			val newHour = datetime.hour - 1
			if (newHour >= 0) {
				LocalDateTime(datetime.date, LocalTime(newHour, 59, datetime.second, datetime.nanosecond))
			} else {
				LocalDateTime(datetime.date.minus(1, DateTimeUnit.DAY), LocalTime(23, 59, datetime.second, datetime.nanosecond))
			}
		}
	}

	actual fun minusOneHour(
		datetime: LocalDateTimeMP,
	): LocalDateTimeMP {
		val newHour = datetime.hour - 1
		return if (newHour >= 0) {
			LocalDateTime(datetime.date, LocalTime(newHour, datetime.minute, datetime.second, datetime.nanosecond))
		} else {
			LocalDateTime(datetime.date.minus(1, DateTimeUnit.DAY), LocalTime(23, datetime.minute, datetime.second, datetime.nanosecond))
		}
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

	actual fun plusOneMonth(date: LocalDateMP): LocalDateMP =
		date.plus(1, DateTimeUnit.MONTH)

	private val MAX_TIME_ZONE = FixedOffsetTimeZone(UtcOffset(14))
	@OptIn(ExperimentalTime::class)
	actual fun nowMaxLocalDateTime(): LocalDateTimeMP =
		Clock.System.now().toLocalDateTime(MAX_TIME_ZONE)
}