package org.taktik.icure.utils

object FuzzyDates {
	const val MAX_FUZZY_DATE = 99991231
	const val MIN_FUZZY_DATE = 10000000

	/**
	 * Does not allow precision markers
	 */
	fun getFullLocalTime(
		fuzzyTime: Int,
	): LocalTimeMP? = doGetLocalTime(fuzzyTime, false, false)?.first

	fun getLocalTimeWithPrecision(
		fuzzyTime: Int,
	): Pair<LocalTimeMP, ChronoUnitMP>? = doGetLocalTime(fuzzyTime, true, false)?.takeIf {
		// Days precision doesn't make sense for time
		it.second != ChronoUnitMP.DAYS
	}?.let { Pair(it.first, it.second) }

	fun getFullLocalDate(
		fuzzyDate: Int,
	): LocalDateMP? = doGetLocalDate(fuzzyDate, false, false)?.first

	fun getLocalDateWithPrecision(
		fuzzyDate: Int,
	): Pair<LocalDateMP, ChronoUnitMP>? = doGetLocalDate(fuzzyDate, true, false)?.let {
		Pair(it.first, it.second)
	}

	fun getFullLocalDateTime(
		dateTime: Long,
		lenient: Boolean,
	): LocalDateTimeMP? = doGetLocalDateTime(dateTime, false, lenient)?.first

	fun getLocalDateTimeWithPrecision(
		dateTime: Long,
		lenient: Boolean,
	): Pair<LocalDateTimeMP, ChronoUnitMP>? = doGetLocalDateTime(dateTime, true, lenient)

	private fun doGetLocalDate(
		fuzzyDate: Int,
		allowEncodedPrecisionMarkers: Boolean,
		lenient: Boolean,
	): Pair<LocalDateMP, ChronoUnitMP>? {
		if (fuzzyDate !in MIN_FUZZY_DATE..MAX_FUZZY_DATE) return null
		val y = (fuzzyDate / 10000L).toInt()
		var mm = (fuzzyDate / 100L % 100L).toInt()
		var d = (fuzzyDate % 100L).toInt()
		var precision: ChronoUnitMP? = null
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
			precision = ChronoUnitMP.YEARS
		}
		if (d == 0) {
			if (!allowEncodedPrecisionMarkers) return null
			d = 1
			if (precision == null) precision = ChronoUnitMP.MONTHS
		}
		return TimeUtilsMP.tryCreateLocalDate(y, mm, d)?.let {
			it to (precision ?: ChronoUnitMP.DAYS)
		}
	}

	private fun doGetLocalTime(
		fuzzyTime: Int,
		allowEncodedPrecisionMarkers: Boolean,
		lenient: Boolean,
	): Triple<LocalTimeMP, ChronoUnitMP, Boolean>? {
		if (fuzzyTime < 0) return null
		var h = (fuzzyTime / 10000L).toInt()
		var m = (fuzzyTime / 100L % 100L).toInt()
		var s = (fuzzyTime % 100L).toInt()
		var detectedPrecision: ChronoUnitMP? = null
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
			detectedPrecision = ChronoUnitMP.MINUTES
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
			if (detectedPrecision == null) detectedPrecision = ChronoUnitMP.HOURS
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
			if (detectedPrecision == null) detectedPrecision = ChronoUnitMP.DAYS
		}
		return Triple(TimeUtilsMP.createLocalTime(h, m, s), detectedPrecision ?: ChronoUnitMP.SECONDS, plusOne)
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
	): Pair<LocalDateTimeMP, ChronoUnitMP>? {
		if (dateTime > MAX_FUZZY_DATE) {
			if (dateTime < 18000101000000L && lenient) {
				return Pair(TimeUtilsMP.utcLocalDatetimeAtTimestamp(dateTime), ChronoUnitMP.SECONDS)
			}
			// Full date time format
			val parsedDateInfo = doGetLocalDate((dateTime / 1000000L).toInt(), allowEncodedPrecisionMarkers || lenient, lenient)
			if (parsedDateInfo == null) return null
			val time = (dateTime % 1000000L).toInt()
			return if (parsedDateInfo.second != ChronoUnitMP.DAYS) {
				if (lenient || time == 0) {
					Pair(TimeUtilsMP.createLocalDateTime(parsedDateInfo.first, TimeUtilsMP.MIDNIGHT_TIME), parsedDateInfo.second)
				} else {
					null // month or year precision with set time and non-lenient is invalid
				}
			} else {
				val parsedTimeInfo = doGetLocalTime(time, allowEncodedPrecisionMarkers || lenient, lenient)
				if (parsedTimeInfo == null) return null
				Pair(
					TimeUtilsMP.createLocalDateTime(
						if (parsedTimeInfo.third) TimeUtilsMP.plusOneDay(parsedDateInfo.first) else parsedDateInfo.first,
						parsedTimeInfo.first,
					),
					parsedTimeInfo.second,
				)
			}
		} else if (lenient) {
			return doGetLocalDate(dateTime.toInt(), true, true)?.let {
				Pair(TimeUtilsMP.createLocalDateTime(it.first, TimeUtilsMP.MIDNIGHT_TIME), it.second)
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
	 * - For [ChronoUnitMP.YEARS] precision the values for the months, days, hours, minutes, and seconds parts will be set to 0
	 *   example: 2024 with years precision is encoded as 2024_00_00_00_00_00
	 * - For [ChronoUnitMP.MONTHS] precision the values for the days, hours, minutes, and seconds parts will be set to 0
	 *   example: January 2024 with months precision is encoded as 2024_01_00_00_00_00
	 * - For [ChronoUnitMP.DAYS] precision the values for the parts:
	 *   - minutes, and seconds will be set to 0
	 *   - hours will be set to 24
	 *   - days will be actually one less than the actual value
	 *   example: 1st January 2024 with days precision is encoded as 2023_12_31_24_00_00
	 * - For [ChronoUnitMP.HOURS] precision the value of the parts:
	 *   - seconds will be set to 0
	 *   - minutes will be set to 60
	 *   - hours will be set to one less than the actual value
	 *   example: 1st January 2024 at 00:00 with hours precision is encoded as 2023_12_31_23_60_00
	 * - For [ChronoUnitMP.SECONDS] precision the value of the parts:
	 *   - seconds will be set to 60
	 *   - minutes will be set to one less than the actual value
	 *   example: 1st January 2024 at 00:00 with minutes precision is encoded as 2023_12_31_23_59_60
	 */
	fun getFuzzyDateTime(dateTime: LocalDateTimeMP, precision: ChronoUnitMP, encodePrecision: Boolean): Long {
		require(
			precision == ChronoUnitMP.YEARS ||
				precision == ChronoUnitMP.MONTHS ||
				precision == ChronoUnitMP.DAYS ||
				precision == ChronoUnitMP.HOURS ||
				precision == ChronoUnitMP.MINUTES ||
				precision == ChronoUnitMP.SECONDS,
		) { "Unsupported precision for datetime: $precision" }
		var returnDateTime = dateTime
		val seconds = if (precision === ChronoUnitMP.SECONDS) {
			TimeUtilsMP.secondOf(returnDateTime)
		} else if (encodePrecision && precision == ChronoUnitMP.MINUTES) {
			returnDateTime = TimeUtilsMP.minusOneMinute(returnDateTime)
			60
		} else {
			0
		}
		val minutes = if (precision.ordinal <= ChronoUnitMP.MINUTES.ordinal) {
			TimeUtilsMP.minuteOf(returnDateTime)
		} else if (encodePrecision && precision == ChronoUnitMP.HOURS) {
			returnDateTime = TimeUtilsMP.minusOneHour(returnDateTime)
			60
		} else {
			0
		}
		val hours = if (precision.ordinal <= ChronoUnitMP.HOURS.ordinal) {
			TimeUtilsMP.hourOf(returnDateTime)
		} else if (encodePrecision && precision == ChronoUnitMP.DAYS) {
			returnDateTime = TimeUtilsMP.minusOneDay(returnDateTime)
			24
		} else {
			0
		}
		return getFuzzyDate(
			TimeUtilsMP.dateOf(returnDateTime),
			if (precision.ordinal < ChronoUnitMP.DAYS.ordinal) ChronoUnitMP.DAYS else precision,
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
		dateTime: LocalDateMP,
		precision: ChronoUnitMP,
		encodePrecision: Boolean,
	): Int {
		require(precision == ChronoUnitMP.YEARS || precision == ChronoUnitMP.MONTHS || precision == ChronoUnitMP.DAYS) {
			"Unsupported precision for fuzzy date: $precision"
		}
		val year = TimeUtilsMP.yearOf(dateTime)
		require(year in 1000..9999) {
			"Fuzzy date only allows years in the range 1000-9999"
		}
		return year *
			10000 +
			(
				if (precision.ordinal <= ChronoUnitMP.MONTHS.ordinal) {
					TimeUtilsMP.monthOf(dateTime) * 100
				} else if (encodePrecision) {
					0
				} else {
					100
				}
			) + (
				if (precision === ChronoUnitMP.DAYS) {
					TimeUtilsMP.dayOf(dateTime)
				} else if (encodePrecision) {
					0
				} else {
					1
				}
			)
	}

	// No precision encoding
	fun getFuzzyTime(time: LocalTimeMP) =
		TimeUtilsMP.hourOf(time) * 1_00_00 + TimeUtilsMP.minuteOf(time) * 1_00 + TimeUtilsMP.secondOf(time)

	fun getNowFuzzyDate(zoneId: String?): Int =
		getFuzzyDate(
			TimeUtilsMP.nowLocalDate(zoneId),
			ChronoUnitMP.DAYS,
			false
		)

	fun getNowFuzzyTime(zoneId: String?): Int =
		getFuzzyTime(TimeUtilsMP.nowLocalTime(zoneId))

	fun getNowFuzzyDateTime(zoneId: String?): Long =
		getFuzzyDateTime(
			TimeUtilsMP.nowLocalDateTime(zoneId),
			ChronoUnitMP.SECONDS,
			false
		)
}