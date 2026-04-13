package org.taktik.icure.utils

import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

actual typealias ChronoUnitMP = ChronoUnit

actual typealias LocalDateTimeMP = LocalDateTime

actual typealias LocalTimeMP = LocalTime

actual typealias LocalDateMP = LocalDate

internal actual object TimeUtilsMP {
	actual fun createLocalTime(
		hours: Int,
		minutes: Int,
		seconds: Int
	): LocalTimeMP =
		LocalTime.of(hours, minutes, seconds)

	actual fun tryCreateLocalDate(
		year: Int,
		month: Int,
		day: Int
	): LocalDateMP? =
		try {
			LocalDate.of(year, month, day)
		} catch (_: DateTimeException) {
			null
		}

	actual fun createLocalDateTime(
		date: LocalDateMP,
		time: LocalTimeMP
	): LocalDateTimeMP =
		LocalDateTime.of(date, time)

	actual fun utcLocalDatetimeAtTimestamp(epochTimestamp: Long): LocalDateTimeMP =
		Instant.ofEpochMilli(epochTimestamp).atZone(ZoneOffset.UTC).toLocalDateTime()

	actual val MIDNIGHT_TIME: LocalTimeMP =
		LocalTime.MIDNIGHT

	actual fun plusOneDay(date: LocalDateMP): LocalDateMP =
		date.plusDays(1)

	actual fun minusOneDay(datetime: LocalDateTimeMP): LocalDateTimeMP =
		datetime.minusDays(1)

	actual fun minusOneMinute(datetime: LocalDateTimeMP): LocalDateTimeMP =
		datetime.minusMinutes(1)

	actual fun minusOneHour(datetime: LocalDateTimeMP): LocalDateTimeMP =
		datetime.minusHours(1)

	actual fun hourOf(time: LocalTimeMP): Int = time.hour

	actual fun minuteOf(time: LocalTimeMP): Int = time.minute

	actual fun secondOf(time: LocalTimeMP): Int = time.second

	actual fun hourOf(time: LocalDateTimeMP): Int = time.hour

	actual fun minuteOf(time: LocalDateTimeMP): Int = time.minute

	actual fun secondOf(time: LocalDateTimeMP): Int = time.second

	actual fun dateOf(dateTime: LocalDateTimeMP): LocalDateMP = dateTime.toLocalDate()

	actual fun yearOf(date: LocalDateMP): Int = date.year

	actual fun monthOf(date: LocalDateMP): Int = date.monthValue

	actual fun dayOf(date: LocalDateMP): Int = date.dayOfMonth

	actual fun nowLocalTime(zone: String?): LocalTimeMP =
		LocalTime.now(zone?.let { ZoneId.of(it) } ?: ZoneOffset.UTC)

	actual fun nowLocalDate(zone: String?): LocalDateMP =
		LocalDate.now(zone?.let { ZoneId.of(it) } ?: ZoneOffset.UTC)

	actual fun nowLocalDateTime(zone: String?): LocalDateTimeMP =
		LocalDateTime.now(zone?.let { ZoneId.of(it) } ?: ZoneOffset.UTC)

	actual fun plusOneMonth(date: LocalDateMP): LocalDateMP =
		date.plusMonths(1)

	private val MAX_TIME_ZONE = ZoneId.ofOffset("UTC", ZoneOffset.ofHours(14))
	actual fun nowMaxLocalDateTime(): LocalDateTimeMP =
		LocalDateTime.now(MAX_TIME_ZONE)

}