package org.taktik.icure.utils

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class JvmFuzzyDatesTest : StringSpec({
	"getFuzzyDate should throw exception on invalid chrono units for precision" {
		val dt = LocalDate.of(2024, 3, 4)

		shouldThrow<IllegalArgumentException> { FuzzyDates.getFuzzyDate(dt, ChronoUnit.NANOS, false) }
		shouldThrow<IllegalArgumentException> { FuzzyDates.getFuzzyDate(dt, ChronoUnit.MICROS, false) }
		shouldThrow<IllegalArgumentException> { FuzzyDates.getFuzzyDate(dt, ChronoUnit.MILLIS, false) }
		shouldThrow<IllegalArgumentException> { FuzzyDates.getFuzzyDate(dt, ChronoUnit.HALF_DAYS, false) }
		shouldThrow<IllegalArgumentException> { FuzzyDates.getFuzzyDate(dt, ChronoUnit.WEEKS, false) }
		shouldThrow<IllegalArgumentException> { FuzzyDates.getFuzzyDate(dt, ChronoUnit.DECADES, false) }
		shouldThrow<IllegalArgumentException> { FuzzyDates.getFuzzyDate(dt, ChronoUnit.CENTURIES, false) }
		shouldThrow<IllegalArgumentException> { FuzzyDates.getFuzzyDate(dt, ChronoUnit.MILLENNIA, false) }
		shouldThrow<IllegalArgumentException> { FuzzyDates.getFuzzyDate(dt, ChronoUnit.ERAS, false) }
		shouldThrow<IllegalArgumentException> { FuzzyDates.getFuzzyDate(dt, ChronoUnit.FOREVER, false) }

		shouldThrow<IllegalArgumentException> { FuzzyDates.getFuzzyDate(dt, ChronoUnit.HOURS, false) }
		shouldThrow<IllegalArgumentException> { FuzzyDates.getFuzzyDate(dt, ChronoUnit.MINUTES, false) }
		shouldThrow<IllegalArgumentException> { FuzzyDates.getFuzzyDate(dt, ChronoUnit.SECONDS, false) }
	}

	"getFuzzyDateTime should throw exception on invalid chrono units for precision" {
		val dt = LocalDateTime.of(2024, 3, 4, 5, 6, 7)
		shouldThrow<IllegalArgumentException> { FuzzyDates.getFuzzyDateTime(dt, ChronoUnit.NANOS, false) }
		shouldThrow<IllegalArgumentException> { FuzzyDates.getFuzzyDateTime(dt, ChronoUnit.MICROS, false) }
		shouldThrow<IllegalArgumentException> { FuzzyDates.getFuzzyDateTime(dt, ChronoUnit.MILLIS, false) }
		shouldThrow<IllegalArgumentException> { FuzzyDates.getFuzzyDateTime(dt, ChronoUnit.HALF_DAYS, false) }
		shouldThrow<IllegalArgumentException> { FuzzyDates.getFuzzyDateTime(dt, ChronoUnit.WEEKS, false) }
		shouldThrow<IllegalArgumentException> { FuzzyDates.getFuzzyDateTime(dt, ChronoUnit.DECADES, false) }
		shouldThrow<IllegalArgumentException> { FuzzyDates.getFuzzyDateTime(dt, ChronoUnit.CENTURIES, false) }
		shouldThrow<IllegalArgumentException> { FuzzyDates.getFuzzyDateTime(dt, ChronoUnit.MILLENNIA, false) }
		shouldThrow<IllegalArgumentException> { FuzzyDates.getFuzzyDateTime(dt, ChronoUnitMP.ERAS, false) }
		shouldThrow<IllegalArgumentException> { FuzzyDates.getFuzzyDateTime(dt, ChronoUnitMP.FOREVER, false) }
	}
})