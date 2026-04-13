package org.taktik.icure.utils

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class FuzzyDatesTest : StringSpec({
	fun createLocalDateTime(
		year: Int,
		month: Int,
		day: Int,
		hour: Int,
		minute: Int,
		second: Int
	) = TimeUtilsMP.createLocalDateTime(
		TimeUtilsMP.tryCreateLocalDate(year, month, day)!!,
		TimeUtilsMP.createLocalTime(hour, minute, second)
	)

	"getFullLocalDate should return full date if precision markers are not used" {
		FuzzyDates.getFullLocalDate(2024_07_03) shouldBe TimeUtilsMP.tryCreateLocalDate(2024, 7, 3)
	}

	"getFullLocalDate should return null if precision markers are used" {
		FuzzyDates.getFullLocalDate(2024_00_00) shouldBe null
		FuzzyDates.getFullLocalDate(2024_01_00) shouldBe null
	}

	"getLocalDateWithPrecision should decode precision correctly" {
		FuzzyDates.getLocalDateWithPrecision(2024_00_00) shouldBe (TimeUtilsMP.tryCreateLocalDate(2024, 1, 1) to ChronoUnitMP.YEARS)
		FuzzyDates.getLocalDateWithPrecision(2024_01_00) shouldBe (TimeUtilsMP.tryCreateLocalDate(2024, 1, 1) to ChronoUnitMP.MONTHS)
		FuzzyDates.getLocalDateWithPrecision(2024_07_03) shouldBe (TimeUtilsMP.tryCreateLocalDate(2024, 7, 3) to ChronoUnitMP.DAYS)
	}

	"getFullLocalTime should return full time if precision markers are not used" {
		FuzzyDates.getFullLocalTime(15_30_45) shouldBe TimeUtilsMP.createLocalTime(15, 30, 45)
	}

	"getFullLocalTime should return null on precision markers" {
		FuzzyDates.getFullLocalTime(23_59_60) shouldBe null
		FuzzyDates.getFullLocalTime(23_60_00) shouldBe null
		FuzzyDates.getFullLocalTime(24_00_00) shouldBe null
		FuzzyDates.getFullLocalTime(19_60_00) shouldBe null
		FuzzyDates.getFullLocalTime(19_59_60) shouldBe null
		FuzzyDates.getFullLocalTime(19_29_60) shouldBe null
	}

	"getLocalTimeWithPrecision should decode precision correctly" {
		FuzzyDates.getLocalTimeWithPrecision(23_59_60) shouldBe (TimeUtilsMP.createLocalTime(0, 0, 0) to ChronoUnitMP.MINUTES)
		FuzzyDates.getLocalTimeWithPrecision(23_60_00) shouldBe (TimeUtilsMP.createLocalTime(0, 0, 0) to ChronoUnitMP.HOURS)
		FuzzyDates.getLocalTimeWithPrecision(19_60_00) shouldBe (TimeUtilsMP.createLocalTime(20, 0, 0) to ChronoUnitMP.HOURS)
		FuzzyDates.getLocalTimeWithPrecision(19_59_60) shouldBe (TimeUtilsMP.createLocalTime(20, 0, 0) to ChronoUnitMP.MINUTES)
		FuzzyDates.getLocalTimeWithPrecision(19_29_60) shouldBe (TimeUtilsMP.createLocalTime(19, 30, 0) to ChronoUnitMP.MINUTES)
		FuzzyDates.getLocalTimeWithPrecision(19_29_59) shouldBe (TimeUtilsMP.createLocalTime(19, 29, 59) to ChronoUnitMP.SECONDS)
		FuzzyDates.getLocalTimeWithPrecision(24_00_00) shouldBe null // Days precision makes no sense for local time
	}

	"getFullLocalDateTime should decode exact datetime without precision markers" {
		FuzzyDates.getFullLocalDateTime(2024_07_03_14_20_00L, lenient = false) shouldBe createLocalDateTime(2024, 7, 3, 14, 20, 0)
		FuzzyDates.getFullLocalDateTime(2024_07_01_14_20_13L, lenient = false) shouldBe createLocalDateTime(2024, 7, 1, 14, 20, 13)
		FuzzyDates.getFullLocalDateTime(2024_07_01_00_00_00L, lenient = false) shouldBe createLocalDateTime(2024, 7, 1, 0, 0, 0)
	}

	"getFullLocalDateTime should fail when fuzzy date has precision markers or is invalid" {
		FuzzyDates.getFullLocalDateTime(2023_12_31_23_59_60L, lenient = false) shouldBe null
		FuzzyDates.getFullLocalDateTime(2023_12_31_22_59_60L, lenient = false) shouldBe null
		FuzzyDates.getFullLocalDateTime(2023_12_31_22_30_60L, lenient = false) shouldBe null
		FuzzyDates.getFullLocalDateTime(2023_12_31_23_60_00L, lenient = false) shouldBe null
		FuzzyDates.getFullLocalDateTime(2023_12_31_22_60_00L, lenient = false) shouldBe null
		FuzzyDates.getFullLocalDateTime(2023_12_31_24_00_00L, lenient = false) shouldBe null
		FuzzyDates.getFullLocalDateTime(2024_03_04_24_00_00L, lenient = false) shouldBe null
		FuzzyDates.getFullLocalDateTime(2023_12_00_00_00_00L, lenient = false) shouldBe null
		FuzzyDates.getFullLocalDateTime(2023_00_00_00_00_00L, lenient = false) shouldBe null
		FuzzyDates.getFullLocalDateTime(2024_02_30_14_20_00L, lenient = false) shouldBe null
		FuzzyDates.getFullLocalDateTime(2024_07_01_14_60_13L, lenient = false) shouldBe null
		FuzzyDates.getFullLocalDateTime(2024_07_01_25_00_00L, lenient = false) shouldBe null
		FuzzyDates.getFullLocalDateTime(2024_01_01_00_70_00L, lenient = false) shouldBe null
		FuzzyDates.getFullLocalDateTime(2023_12_31_23_60_60L, lenient = false) shouldBe null
		FuzzyDates.getFullLocalDateTime(2023_12_31_24_60_60L, lenient = false) shouldBe null
		FuzzyDates.getFullLocalDateTime(2023_12_31_24_60_00L, lenient = false) shouldBe null
		FuzzyDates.getFullLocalDateTime(2023_12_31_20_60_60L, lenient = false) shouldBe null
		FuzzyDates.getFullLocalDateTime(2023_12_00_01_01_01L, lenient = false) shouldBe null
		FuzzyDates.getFullLocalDateTime(2023_00_00_01_01_01L, lenient = false) shouldBe null
	}

	"getLocalDateTimeWithPrecision should decode datetime with precision markers" {
		FuzzyDates.getLocalDateTimeWithPrecision(2024_07_03_14_20_00L, lenient = false) shouldBe (createLocalDateTime(2024, 7, 3, 14, 20, 0) to ChronoUnitMP.SECONDS)
		FuzzyDates.getLocalDateTimeWithPrecision(2024_07_01_14_20_13L, lenient = false) shouldBe (createLocalDateTime(2024, 7, 1, 14, 20, 13) to ChronoUnitMP.SECONDS)
		FuzzyDates.getLocalDateTimeWithPrecision(2024_07_01_00_00_00L, lenient = false) shouldBe (createLocalDateTime(2024, 7, 1, 0, 0, 0) to ChronoUnitMP.SECONDS)
		FuzzyDates.getLocalDateTimeWithPrecision(2024_01_01_00_00_00L, lenient = false) shouldBe (createLocalDateTime(2024, 1, 1, 0, 0, 0) to ChronoUnitMP.SECONDS)
		FuzzyDates.getLocalDateTimeWithPrecision(2023_12_31_23_59_60L, lenient = false) shouldBe (createLocalDateTime(2024, 1, 1, 0, 0, 0) to ChronoUnitMP.MINUTES)
		FuzzyDates.getLocalDateTimeWithPrecision(2023_12_31_22_59_60L, lenient = false) shouldBe (createLocalDateTime(2023, 12, 31, 23, 0, 0) to ChronoUnitMP.MINUTES)
		FuzzyDates.getLocalDateTimeWithPrecision(2023_12_31_22_30_60L, lenient = false) shouldBe (createLocalDateTime(2023, 12, 31, 22, 31, 0) to ChronoUnitMP.MINUTES)
		FuzzyDates.getLocalDateTimeWithPrecision(2023_12_31_23_60_00L, lenient = false) shouldBe (createLocalDateTime(2024, 1, 1, 0, 0, 0) to ChronoUnitMP.HOURS)
		FuzzyDates.getLocalDateTimeWithPrecision(2023_12_31_22_60_00L, lenient = false) shouldBe (createLocalDateTime(2023, 12, 31, 23, 0, 0) to ChronoUnitMP.HOURS)
		FuzzyDates.getLocalDateTimeWithPrecision(2023_12_31_24_00_00L, lenient = false) shouldBe (createLocalDateTime(2024, 1, 1, 0, 0, 0) to ChronoUnitMP.DAYS)
		FuzzyDates.getLocalDateTimeWithPrecision(2024_03_04_24_00_00L, lenient = false) shouldBe (createLocalDateTime(2024, 3, 5, 0, 0, 0) to ChronoUnitMP.DAYS)
		FuzzyDates.getLocalDateTimeWithPrecision(2023_12_00_00_00_00L, lenient = false) shouldBe (createLocalDateTime(2023, 12, 1, 0, 0, 0) to ChronoUnitMP.MONTHS)
		FuzzyDates.getLocalDateTimeWithPrecision(2023_00_00_00_00_00L, lenient = false) shouldBe (createLocalDateTime(2023, 1, 1, 0, 0, 0) to ChronoUnitMP.YEARS)
	}

	"getLocalDateTimeWithPrecision should fail on invalid datetimes and lenient = false" {
		FuzzyDates.getLocalDateTimeWithPrecision(2024_02_30_14_20_00L, lenient = false) shouldBe null
		FuzzyDates.getLocalDateTimeWithPrecision(2024_07_01_14_60_13L, lenient = false) shouldBe null
		FuzzyDates.getLocalDateTimeWithPrecision(2024_07_01_25_00_00L, lenient = false) shouldBe null
		FuzzyDates.getLocalDateTimeWithPrecision(2024_01_01_00_70_00L, lenient = false) shouldBe null
		FuzzyDates.getLocalDateTimeWithPrecision(2023_12_31_23_60_60L, lenient = false) shouldBe null
		FuzzyDates.getLocalDateTimeWithPrecision(2023_12_31_24_60_60L, lenient = false) shouldBe null
		FuzzyDates.getLocalDateTimeWithPrecision(2023_12_31_24_60_00L, lenient = false) shouldBe null
		FuzzyDates.getLocalDateTimeWithPrecision(2023_12_31_20_60_60L, lenient = false) shouldBe null
		FuzzyDates.getLocalDateTimeWithPrecision(2023_12_00_01_01_01L, lenient = false) shouldBe null
		FuzzyDates.getLocalDateTimeWithPrecision(2023_00_00_01_01_01L, lenient = false) shouldBe null
		FuzzyDates.getLocalDateTimeWithPrecision(2024_01_00_23_59_60L, lenient = false) shouldBe null
	}

	"getFuzzyDate should encode year, month, and day precision correctly" {
		val dt = TimeUtilsMP.tryCreateLocalDate(2024, 1, 1)!!
		FuzzyDates.getFuzzyDate(dt, ChronoUnitMP.YEARS, true) shouldBe 20240000
		FuzzyDates.getFuzzyDate(dt, ChronoUnitMP.MONTHS, true) shouldBe 20240100
		FuzzyDates.getFuzzyDate(dt, ChronoUnitMP.DAYS, true) shouldBe 20240101
	}

	"getFuzzyDateTime should truncate to the provided precision and encode when requested" {
		val dt1 = createLocalDateTime(2024, 1, 1, 0, 0, 0)

		FuzzyDates.getFuzzyDateTime(dt1, ChronoUnitMP.SECONDS, false) shouldBe 20240101000000L
		FuzzyDates.getFuzzyDateTime(dt1, ChronoUnitMP.MINUTES, false) shouldBe 20240101000000L
		FuzzyDates.getFuzzyDateTime(dt1, ChronoUnitMP.HOURS, false) shouldBe 20240101000000L
		FuzzyDates.getFuzzyDateTime(dt1, ChronoUnitMP.DAYS, false) shouldBe 20240101000000L
		FuzzyDates.getFuzzyDateTime(dt1, ChronoUnitMP.MONTHS, false) shouldBe 20240101000000L
		FuzzyDates.getFuzzyDateTime(dt1, ChronoUnitMP.YEARS, false) shouldBe 20240101000000L

		FuzzyDates.getFuzzyDateTime(dt1, ChronoUnitMP.SECONDS, true) shouldBe 20240101000000L
		FuzzyDates.getFuzzyDateTime(dt1, ChronoUnitMP.MINUTES, true) shouldBe 20231231235960L
		FuzzyDates.getFuzzyDateTime(dt1, ChronoUnitMP.HOURS, true) shouldBe 20231231236000L
		FuzzyDates.getFuzzyDateTime(dt1, ChronoUnitMP.DAYS, true) shouldBe 20231231240000L
		FuzzyDates.getFuzzyDateTime(dt1, ChronoUnitMP.MONTHS, true) shouldBe 20240100000000L
		FuzzyDates.getFuzzyDateTime(dt1, ChronoUnitMP.YEARS, true) shouldBe 20240000000000L

		val dt2 = createLocalDateTime(2024, 3, 4, 5, 6, 7)

		FuzzyDates.getFuzzyDateTime(dt2, ChronoUnitMP.SECONDS, false) shouldBe 2024_03_04_05_06_07L
		FuzzyDates.getFuzzyDateTime(dt2, ChronoUnitMP.MINUTES, false) shouldBe 2024_03_04_05_06_00L
		FuzzyDates.getFuzzyDateTime(dt2, ChronoUnitMP.HOURS, false)   shouldBe 2024_03_04_05_00_00L
		FuzzyDates.getFuzzyDateTime(dt2, ChronoUnitMP.DAYS, false)    shouldBe 2024_03_04_00_00_00L
		FuzzyDates.getFuzzyDateTime(dt2, ChronoUnitMP.MONTHS, false)  shouldBe 2024_03_01_00_00_00L
		FuzzyDates.getFuzzyDateTime(dt2, ChronoUnitMP.YEARS, false)   shouldBe 2024_01_01_00_00_00L

		FuzzyDates.getFuzzyDateTime(dt2, ChronoUnitMP.SECONDS, true)  shouldBe 2024_03_04_05_06_07L
		FuzzyDates.getFuzzyDateTime(dt2, ChronoUnitMP.MINUTES, true)  shouldBe 2024_03_04_05_05_60L
		FuzzyDates.getFuzzyDateTime(dt2, ChronoUnitMP.HOURS, true)    shouldBe 2024_03_04_04_60_00L
		FuzzyDates.getFuzzyDateTime(dt2, ChronoUnitMP.DAYS, true)     shouldBe 2024_03_03_24_00_00L
		FuzzyDates.getFuzzyDateTime(dt2, ChronoUnitMP.MONTHS, true)   shouldBe 2024_03_00_00_00_00L
		FuzzyDates.getFuzzyDateTime(dt2, ChronoUnitMP.YEARS, true)    shouldBe 2024_00_00_00_00_00L
	}

	"getFullLocalDateTime should decode epoch millis when lenient is true" {
		listOf(
			Pair(
				createLocalDateTime(2024, 7, 3, 4, 5, 6),
				1719979506000
			),
			Pair(
				createLocalDateTime(2026, 3, 29, 0, 30, 0),
				1774744200000
			),
			Pair(
				createLocalDateTime(2026, 3, 29, 1, 30, 0),
				1774747800000
			),
			Pair(
				createLocalDateTime(2026, 3, 29, 2, 30, 0),
				1774751400000
			),
		).forEach { (expected, epochMillis) ->
			FuzzyDates.getFullLocalDateTime(epochMillis, lenient = true) shouldBe expected
			FuzzyDates.getFullLocalDateTime(epochMillis, lenient = false) shouldBe null
		}
	}

	"getMonthRange should return single month when start and end are in the same month" {
		FuzzyDates.getMonthRange(20240301L, 20240315L, 12) shouldBe listOf(2024 to 3)
		FuzzyDates.getMonthRange(20240101L, 20240131L, 12) shouldBe listOf(2024 to 1)
	}

	"getMonthRange should return multiple months within the same year" {
		FuzzyDates.getMonthRange(20240101L, 20240301L, 12) shouldBe listOf(
			2024 to 1, 2024 to 2, 2024 to 3
		)
		FuzzyDates.getMonthRange(20240601L, 20241201L, 12) shouldBe listOf(
			2024 to 6, 2024 to 7, 2024 to 8, 2024 to 9, 2024 to 10, 2024 to 11, 2024 to 12
		)
	}

	"getMonthRange should handle cross-year boundaries" {
		FuzzyDates.getMonthRange(20231101L, 20240201L, 12) shouldBe listOf(
			2023 to 11, 2023 to 12, 2024 to 1, 2024 to 2
		)
	}

	"getMonthRange should accept full datetime format (14 digits)" {
		FuzzyDates.getMonthRange(20240301143000L, 20240501120000L, 12) shouldBe listOf(
			2024 to 3, 2024 to 4, 2024 to 5
		)
	}

	"getMonthRange should return null when range exceeds maxMonths" {
		FuzzyDates.getMonthRange(20240101L, 20240301L, 2) shouldBe null
		FuzzyDates.getMonthRange(20240101L, 20241201L, 11) shouldBe null
	}

	"getMonthRange should return list of exactly maxMonths when range equals maxMonths" {
		FuzzyDates.getMonthRange(20240101L, 20240301L, 3) shouldBe listOf(
			2024 to 1, 2024 to 2, 2024 to 3
		)
	}

	"getMonthRange should return null when end is before start" {
		FuzzyDates.getMonthRange(20240301L, 20240101L, 12) shouldBe null
	}

	"getMonthRange should return null for invalid dates" {
		FuzzyDates.getMonthRange(20240230L, 20240301L, 12) shouldBe null
		FuzzyDates.getMonthRange(20240101L, 20240230L, 12) shouldBe null
	}

	"getMonthRange should handle epoch millis input via lenient parsing" {
		// 2024-07-03 04:05:06 UTC = 1719979506000 ms
		// 2024-07-03 is in July, and August 1 2024 at some point
		FuzzyDates.getMonthRange(1719979506000, 1722470400000, 12) shouldBe listOf(
			2024 to 7, 2024 to 8
		)
	}

	"getMonthRange should handle end-of-month start dates correctly" {
		// Starting on Jan 31 - iterative plusOneMonth causes day drift, but year/month pairs must still be correct
		FuzzyDates.getMonthRange(20240131L, 20240401L, 12) shouldBe listOf(
			2024 to 1, 2024 to 2, 2024 to 3, 2024 to 4
		)
	}

	"getMonthRange should handle a 12-month full-year range" {
		FuzzyDates.getMonthRange(20240101L, 20241201L, 12) shouldBe (1..12).map { 2024 to it }
	}
})