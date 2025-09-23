package org.taktik.icure.utils

import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * Updated version of [FuzzyValues], with improved logic and configurability.
 *
 * You can configure if the precision should be encoded when converting a local date/time to fuzzy, or when converting
 * from fuzzy to local date/time if encoded precision values are allowed
 */
object FuzzyDates {
	const val MAX_FUZZY_DATE = 99991231
	const val MIN_FUZZY_DATE = 10000000

	/**
	 * Does not allow precision markers
	 */
	fun getFullLocalTime(
		fuzzyTime: Int,
	): LocalTime? = doGetLocalTime(fuzzyTime, false, false)?.first

	fun getLocalTimeWithPrecision(
		fuzzyTime: Int,
	): Pair<LocalTime, ChronoUnit>? = doGetLocalTime(fuzzyTime, true, false)?.takeIf {
		// Days precision doesn't make sense for time
		it.second != ChronoUnit.DAYS
	}?.let { Pair(it.first, it.second) }

	fun getFullLocalDate(
		fuzzyDate: Int,
	): LocalDate? = doGetLocalDate(fuzzyDate, false, false)?.first

	fun getLocalDateWithPrecision(
		fuzzyDate: Int,
	): Pair<LocalDate, ChronoUnit>? = doGetLocalDate(fuzzyDate, true, false)?.let {
		Pair(it.first, it.second)
	}

	fun getFullLocalDateTime(
		dateTime: Long,
		lenient: Boolean,
	): LocalDateTime? = doGetLocalDateTime(dateTime, false, lenient)?.first

	fun getLocalDateTimeWithPrecision(
		dateTime: Long,
		lenient: Boolean,
	): Pair<LocalDateTime, ChronoUnit>? = doGetLocalDateTime(dateTime, true, lenient)

	private fun doGetLocalDate(
		fuzzyDate: Int,
		allowEncodedPrecisionMarkers: Boolean,
		lenient: Boolean,
	): Pair<LocalDate, ChronoUnit>? {
		if (fuzzyDate !in MIN_FUZZY_DATE..MAX_FUZZY_DATE) return null
		val y = (fuzzyDate / 10000L).toInt()
		var mm = (fuzzyDate / 100L % 100L).toInt()
		var d = (fuzzyDate % 100L).toInt()
		var precision: ChronoUnit? = null
		if (mm == 0) {
			if (!allowEncodedPrecisionMarkers) return null
			if (d != 0) {
				if (lenient) {
					d = 0
				} else {
					return null
				}
			}
			mm = 1
			precision = ChronoUnit.YEARS
		}
		if (d == 0) {
			if (!allowEncodedPrecisionMarkers) return null
			d = 1
			if (precision == null) precision = ChronoUnit.MONTHS
		}
		return try {
			Pair(LocalDate.of(y, mm, d), precision ?: ChronoUnit.DAYS)
		} catch (_: DateTimeException) {
			null
		}
	}

	private fun doGetLocalTime(
		fuzzyTime: Int,
		allowEncodedPrecisionMarkers: Boolean,
		lenient: Boolean,
	): Triple<LocalTime, ChronoUnit, Boolean>? {
		if (fuzzyTime < 0) return null
		var h = (fuzzyTime / 10000L).toInt()
		var m = (fuzzyTime / 100L % 100L).toInt()
		var s = (fuzzyTime % 100L).toInt()
		var detectedPrecision: ChronoUnit? = null
		// Need to keep plusOe separate from detected precision: 23_59_60 is precision minutes, but wants plusOne
		var plusOne = false
		if (h > 24) {
			return null
		}
		if (m > 60) {
			return null
		}
		if (s > 60) {
			return null
		}
		if (s == 60) {
			if (!allowEncodedPrecisionMarkers || m == 60) return null
			s = 0
			m++
			detectedPrecision = ChronoUnit.MINUTES
		}
		if (m == 60) {
			if (!allowEncodedPrecisionMarkers || h == 24) return null
			if (s != 0) {
				if (lenient) {
					s = 0
				} else {
					return null
				}
			}
			m = 0
			h++
			if (detectedPrecision == null) detectedPrecision = ChronoUnit.HOURS
		}
		if (h == 24) {
			if (!allowEncodedPrecisionMarkers) return null
			if (m != 0 || s != 0) {
				if (lenient) {
					m = 0
					s = 0
				} else {
					return null
				}
			}
			h = 0
			plusOne = true
			if (detectedPrecision == null) detectedPrecision = ChronoUnit.DAYS
		}
		return Triple(LocalTime.of(h, m, s), detectedPrecision ?: ChronoUnit.SECONDS, plusOne)
	}

	/**
	 * Convert a fuzzy date time into a LocalDateTime
	 * @param dateTime the fuzzy date time
	 * @param allowEncodedPrecisionMarkers true if a fuzzy [dateTime] with encoded precision markers should be accepted. If false, the encoded precision markers won't be accepted as valid and the method returns null
	 * @param lenient true if you want to use lenient decoding:
	 * - If [dateTime] looks like an epoch timestamp it will be interpreted as such
	 * - If [dateTime] is a fuzzy date instead of a fuzzy date time it will still be accepted
	 * - Ignores precision marker instead of failing if [allowEncodedPrecisionMarkers] is false
	 */
	private fun doGetLocalDateTime(
		dateTime: Long,
		allowEncodedPrecisionMarkers: Boolean,
		lenient: Boolean,
	): Pair<LocalDateTime, ChronoUnit>? {
		if (dateTime > MAX_FUZZY_DATE) {
			if (dateTime < 18000101000000L && lenient) {
				return Pair(Instant.ofEpochMilli(dateTime).atZone(ZoneOffset.UTC).toLocalDateTime(), ChronoUnit.SECONDS)
			}
			// Full date time format
			val parsedDateInfo = doGetLocalDate((dateTime / 1000000L).toInt(), allowEncodedPrecisionMarkers || lenient, lenient)
			if (parsedDateInfo == null) return null
			val time = (dateTime % 1000000L).toInt()
			return if (parsedDateInfo.second != ChronoUnit.DAYS) {
				if (lenient || time == 0) {
					Pair(LocalDateTime.of(parsedDateInfo.first, LocalTime.MIDNIGHT), parsedDateInfo.second)
				} else {
					null // month or year precision with set time and non-lenient is invalid
				}
			} else {
				val parsedTimeInfo = doGetLocalTime(time, allowEncodedPrecisionMarkers || lenient, lenient)
				if (parsedTimeInfo == null) return null
				Pair(
					LocalDateTime.of(
						if (parsedTimeInfo.third) parsedDateInfo.first.plusDays(1) else parsedDateInfo.first,
						parsedTimeInfo.first,
					),
					parsedTimeInfo.second,
				)
			}
		} else if (lenient) {
			return doGetLocalDate(dateTime.toInt(), true, true)?.let {
				Pair(LocalDateTime.of(it.first, LocalTime.MIDNIGHT), it.second)
			}
		} else {
			return null
		}
	}

	/**
	 * Convert a LocalDateTime to a fuzzy date time.
	 * @param dateTime the local date time
	 * @param precision the precision of the conversion. Truncates the result to the provided precision (rounding is
	 * done as flooring)
	 * @param encodePrecision if true will encode the precision of the conversion in the fuzzy date using special markers:
	 * - For [ChronoUnit.YEARS] precision the values for the months, days, hours, minutes, and seconds parts will be set to 0
	 *   example: 2024 with years precision is encoded as 2024_00_00_00_00_00
	 * - For [ChronoUnit.MONTHS] precision the values for the days, hours, minutes, and seconds parts will be set to 0
	 *   example: January 2024 with months precision is encoded as 2024_01_00_00_00_00
	 * - For [ChronoUnit.DAYS] precision the values for the parts:
	 *   - minutes, and seconds will be set to 0
	 *   - hours will be set to 24
	 *   - days will be actually one less than the actual value
	 *   example: 1st January 2024 with days precision is encoded as 2023_12_31_24_00_00
	 * - For [ChronoUnit.HOURS] precision the value of the parts:
	 *   - seconds will be set to 0
	 *   - minutes will be set to 60
	 *   - hours will be set to one less than the actual value
	 *   example: 1st January 2024 at 00:00 with hours precision is encoded as 2023_12_31_23_60_00
	 * - For [ChronoUnit.SECONDS] precision the value of the parts:
	 *   - seconds will be set to 60
	 *   - minutes will be set to one less than the actual value
	 *   example: 1st January 2024 at 00:00 with minutes precision is encoded as 2023_12_31_23_59_60
	 */
	fun getFuzzyDateTime(dateTime: LocalDateTime, precision: ChronoUnit, encodePrecision: Boolean): Long {
		require(
			precision == ChronoUnit.YEARS ||
				precision == ChronoUnit.MONTHS ||
				precision == ChronoUnit.DAYS ||
				precision == ChronoUnit.HOURS ||
				precision == ChronoUnit.MINUTES ||
				precision == ChronoUnit.SECONDS,
		) { "Unsupported precision for datetime: $precision" }
		var returnDateTime = dateTime
		val seconds = if (precision === ChronoUnit.SECONDS) {
			returnDateTime.second
		} else if (encodePrecision && precision == ChronoUnit.MINUTES) {
			returnDateTime = returnDateTime.minusMinutes(1)
			60
		} else {
			0
		}
		val minutes = if (precision.ordinal <= ChronoUnit.MINUTES.ordinal) {
			returnDateTime.minute
		} else if (encodePrecision && precision == ChronoUnit.HOURS) {
			returnDateTime = returnDateTime.minusHours(1)
			60
		} else {
			0
		}
		val hours = if (precision.ordinal <= ChronoUnit.HOURS.ordinal) {
			returnDateTime.hour
		} else if (encodePrecision && precision == ChronoUnit.DAYS) {
			returnDateTime = returnDateTime.minusDays(1)
			24
		} else {
			0
		}
		return getFuzzyDate(
			returnDateTime.toLocalDate(),
			if (precision.ordinal < ChronoUnit.DAYS.ordinal) ChronoUnit.DAYS else precision,
			encodePrecision,
		) *
			1000000L +
			hours *
			10000L +
			minutes *
			100L +
			seconds
	}

	fun getFuzzyDate(
		dateTime: LocalDate,
		precision: ChronoUnit,
		encodePrecision: Boolean,
	): Int {
		require(precision == ChronoUnit.YEARS || precision == ChronoUnit.MONTHS || precision == ChronoUnit.DAYS) {
			"Unsupported precision for fuzzy date: $precision"
		}
		require(dateTime.year >= 1000 && dateTime.year <= 9999) {
			"Fuzzy date only allows years in the range 1000-9999"
		}
		return dateTime.year *
			10000 +
			(
				if (precision.ordinal <= ChronoUnit.MONTHS.ordinal) {
					dateTime.monthValue * 100
				} else if (encodePrecision) {
					0
				} else {
					100
				}
				) +
			(
				if (precision === ChronoUnit.DAYS) {
					dateTime.dayOfMonth
				} else if (encodePrecision) {
					0
				} else {
					1
				}
				)
	}

	// No precision encoding
	fun getFuzzyTime(time: LocalTime) = time.hour * 1_00_00 + time.minute * 1_00 + time.second
}
