package org.taktik.icure.utils

expect class LocalTimeMP

expect class LocalDateMP

expect class LocalDateTimeMP

/**
 * Implementation ordinal MUST GUARANTEE:
 * SECONDS < MINUTES < HOURS < DAYS < MONTHS < YEARS
 */
expect enum class ChronoUnitMP {
	SECONDS,
	MINUTES,
	HOURS,
	DAYS,
	MONTHS,
	YEARS
}

expect internal object TimeUtilsMP {
	fun nowLocalTime(zone: String?): LocalTimeMP

	fun nowLocalDate(zone: String?): LocalDateMP

	fun nowLocalDateTime(zone: String?): LocalDateTimeMP

	/**
	 * Create a local time with hours (0-23), minutes (0-59), and seconds (0-59)
	 * The values must already be in the correct range, throws a platform-dependent exception otherwise
	 */
	fun createLocalTime(
		hours: Int,
		minutes: Int,
		seconds: Int
	): LocalTimeMP

	/**
	 * Initialize a local date with year (gregorian), month (1-12) and day (1-31).
	 * If the date is not a valid date (e.g. 2025-02-29, since 2025 was not a leap year), null is returned.
	 */
	fun tryCreateLocalDate(
		year: Int,
		month: Int,
		day: Int
	): LocalDateMP?

	fun createLocalDateTime(
		date: LocalDateMP,
		time: LocalTimeMP
	): LocalDateTimeMP

	/**
	 * Get a local date time matching the time of the given epoch timestamp, in the UTC timezone.
	 */
	fun utcLocalDatetimeAtTimestamp(epochTimestamp: Long): LocalDateTimeMP

	val MIDNIGHT_TIME: LocalTimeMP

	/**
	 * Does not mutate input
	 */
	fun plusDays(date: LocalDateMP, amount: Long): LocalDateMP

	/**
	 * Does not mutate input
	 */
	fun minusDays(datetime: LocalDateTimeMP, amount: Long): LocalDateTimeMP

	/**
	 * Does not mutate input
	 */
	fun minusMinutes(datetime: LocalDateTimeMP, amount: Long): LocalDateTimeMP

	/**
	 * Does not mutate input
	 */
	fun minusHours(datetime: LocalDateTimeMP, amount: Long): LocalDateTimeMP

	fun hourOf(time: LocalTimeMP): Int

	fun minuteOf(time: LocalTimeMP): Int

	fun secondOf(time: LocalTimeMP): Int

	fun hourOf(time: LocalDateTimeMP): Int

	fun minuteOf(time: LocalDateTimeMP): Int

	fun secondOf(time: LocalDateTimeMP): Int

	fun dateOf(dateTime: LocalDateTimeMP): LocalDateMP

	fun yearOf(date: LocalDateMP): Int

	/**
	 * Range 1-12
	 */
	fun monthOf(date: LocalDateMP): Int

	/**
	 * Range 1-31
	 */
	fun dayOf(date: LocalDateMP): Int
}