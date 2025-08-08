package org.taktik.icure.utils.entities.embed

import org.dmfs.rfc5545.recur.RecurrenceRule
import org.taktik.icure.entities.embed.TimeTableHour
import org.taktik.icure.entities.embed.TimeTableItem
import org.taktik.icure.utils.FuzzyValues
import org.taktik.icure.utils.isXDayweekOfMonthInRange
import org.taktik.icure.utils.sortedMerge
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.TimeZone

fun TimeTableItem.iterator(
	startDateTime: Long,
	endDateTime: Long,
	duration: Duration,
) = object : Iterator<Long> {
	// Start time or now() - notBeforeInMinutes if specified, as fuzzy date
	val constrainedStartDateTime =
		startDateTime.coerceAtLeast(
			notBeforeInMinutes?.let {
				FuzzyValues.getFuzzyDateTime(
					LocalDateTime.ofInstant(Instant.now() - Duration.ofMinutes(it.toLong()), ZoneId.of(zoneId ?: "UTC")),
					ChronoUnit.SECONDS,
				)
			} ?: 0,
		)

	// End time or now() - notAfterInMinutes if specified, as fuzzy date
	val constrainedEndDateTime =
		endDateTime.coerceAtMost(
			notAfterInMinutes?.let {
				FuzzyValues.getFuzzyDateTime(
					LocalDateTime.ofInstant(Instant.now() - Duration.ofMinutes(it.toLong()), ZoneId.of(zoneId ?: "UTC")),
					ChronoUnit.SECONDS,
				)
			} ?: endDateTime,
		)

	// Start and end fuzzy dates with second component set to 0
	val startLdt = FuzzyValues.getDateTime(constrainedStartDateTime - (constrainedStartDateTime % 100))!!
	val endLdt = FuzzyValues.getDateTime(constrainedEndDateTime - (constrainedEndDateTime % 100))!!

	val daysIterator =
		object : Iterator<LocalDateTime> {
			var day =
				startLdt
					.withHour(0)
					.withMinute(0)
					.withSecond(0)
					.withNano(0)
			val rrit =
				rrule?.let {
					RecurrenceRule(it)
						.iterator(
							FuzzyValues
								.getDateTime(rruleStartDate ?: constrainedStartDateTime)!!
								.atOffset(ZoneOffset.UTC)
								.toInstant()
								.toEpochMilli(),
							TimeZone.getTimeZone("UTC"),
						).also { iterator ->
							// After instantiating the iterator, skips all the slots up to constrainedStartDateTime minus 1 day.
							// (Why not setting the start there?)
							iterator.fastForward(
								FuzzyValues
									.getDateTime(constrainedStartDateTime)!!
									.atOffset(ZoneOffset.UTC)
									.toInstant()
									.toEpochMilli() -
									24 *
									3600 *
									1000,
							)
						}
				}

			@Suppress("DEPRECATION")
			private fun getNextValidLegacyDay() = generateSequence(day) {
				// WHY?
				(it + Duration.ofDays(1)).takeIf { d -> d <= endLdt }
			}.firstOrNull { d ->
				(
					days.any { dd ->
						dd.toInt() == d.dayOfWeek.value
					} && // The day of week of the timestamp is listed in the days property
						recurrenceTypes.any { r ->
							// The day of the week of the slot matches a weekly recurrence condition
							r == "EVERY_WEEK" ||
								listOf("ONE" to 1, "TWO" to 2, "THREE" to 3, "FOUR" to 4, "FIVE" to 5).any { (rt, i) ->
									(r == rt && isXDayweekOfMonthInRange(d.dayOfWeek, i.toLong(), startLdt, endLdt))
								}
						}
					)
			}

			override fun hasNext(): Boolean = rrit?.let {
				try {
					it.peekMillis().let { n ->
						LocalDateTime.ofInstant(Instant.ofEpochMilli(n), ZoneOffset.UTC) <= endLdt
					}
				} catch (e: ArrayIndexOutOfBoundsException) {
					false
				}
			} ?: (getNextValidLegacyDay() != null)

			override fun next(): LocalDateTime = rrit?.nextMillis()?.let { nextMillis ->
				LocalDateTime.ofInstant(Instant.ofEpochMilli(nextMillis), ZoneOffset.UTC).also { day = it }
			} ?: run {
				getNextValidLegacyDay()?.also { day = it + Duration.ofDays(1) } ?: throw NoSuchElementException()
			}
		}

	var hoursIterator = hours.iterator(duration)

	// Used for look ahead when we want to be sure that hasNext respects all constraints
	var currentDay = if (daysIterator.hasNext()) daysIterator.next() else null

	// More of a "currentSlot" as the iterator may return periods of duration != 1 hour
	var currentHour = if (hoursIterator.hasNext()) hoursIterator.next() else null

	init {
		val startOfStartDayLdt =
			startLdt
				.withHour(0)
				.withMinute(0)
				.withSecond(0)
				.withNano(0)
		while (currentDay != null && currentDay!! < startOfStartDayLdt) {
			currentDay = if (daysIterator.hasNext()) daysIterator.next() else null
		}
	}

	override fun hasNext(): Boolean {
		val cd = currentDay
		val ch = currentHour // More of a "current slot"

		return cd != null &&
			when {
				// There is a slot available, but it's on the last day, and it is after the last hour available for the day -> no availability
				ch != null && cd.withHour((ch.toInt()) / 10000).withMinute(((ch.toInt()) / 100) % 100).withSecond(0) > endLdt -> false
				// There is a slot available, and the day is greater than the constrained start -> availability
				ch != null && cd > startLdt -> true
				// There is a slot and the current day is exactly the start day
				ch != null -> {
					// If it's after the start time (hhmmss) -> availability
					if (ch >= constrainedStartDateTime % 1000000) {
						true
					} else {
						// We need to skip the current hour and see if there is a later one that matches the constraints
						currentHour = if (hoursIterator.hasNext()) hoursIterator.next() else null
						hasNext()
					}
				}
				else -> {
					// We have exhausted the available hours for this day... We need to check what's possible on the next one
					hoursIterator = hours.iterator(duration)
					currentHour = if (hoursIterator.hasNext()) hoursIterator.next() else null
					currentDay = if (daysIterator.hasNext()) daysIterator.next() else null
					(
						currentDay != null &&
							currentHour != null &&
							currentDay!!.withHour((currentHour!!.toInt()) / 10000).withMinute(((currentHour!!.toInt()) / 100) % 100).withSecond(0) <= endLdt
						)
				}
			}
	}

	override fun next(): Long = (
		currentHour?.let {
			currentHour = if (hoursIterator.hasNext()) hoursIterator.next() else null // Prefetch the next hour for hasNext() and next()
			FuzzyValues.getFuzzyDateTime(currentDay!! + Duration.ofNanos(it.toLocalTime().toNanoOfDay()), ChronoUnit.SECONDS)
		} ?: run {
			hoursIterator = hours.iterator(duration)
			currentHour = if (hoursIterator.hasNext()) hoursIterator.next() else null
			currentDay = if (daysIterator.hasNext()) daysIterator.next() else null
			next()
		}
		).let {
		if (it < constrainedStartDateTime) {
			if (hasNext()) next() else throw NoSuchElementException() // This should never happen if hasNext() is called before next()
		} else {
			it
		}
	}
}

fun List<TimeTableHour>.iterator(duration: Duration): Iterator<Long> = this.map { it.iterator(duration) }.sortedMerge()

/**
 * Iterates all the slots of the specified [duration] in a [TimeTableHour] from [TimeTableHour.startHour] to
 * [TimeTableHour.endHour]
 */
fun TimeTableHour.iterator(duration: Duration) = object : Iterator<Long> {
	val normalisedEndLocalTime = (endHour?.takeIf { it != 240000L } ?: 235959L).toLocalTime()

	fun getAcceptableLocalTime(t: Long?) = (t ?: 0L).toLocalTime().takeIf {
		val end = it + duration - Duration.ofSeconds(60)
		end <= normalisedEndLocalTime && end > it
	}

	var current: LocalTime? = getAcceptableLocalTime(this@iterator.startHour)

	override fun hasNext() = current != null

	override fun next() = (current ?: throw NoSuchElementException()).let { c ->
		c.toLongHour().also {
			current = (c + duration).takeIf { it > c }?.let { getAcceptableLocalTime(it.toLongHour()) }
		}
	}
}

fun LocalTime.toLongHour() = (this.hour * 10000 + this.minute * 100 + this.second).toLong()

fun Long.toLocalTime(): LocalTime = LocalTime.of((this / 10000).toInt(), ((this % 10000) / 100).toInt(), (this % 100).toInt())
