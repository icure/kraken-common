/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.utils

import org.apache.commons.lang3.math.NumberUtils
import java.math.BigInteger
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit

/**
 * This utility class provides methods to detect the type of value submitted to it (dates, ssin,...) and handle the
 * value consequently.<br></br>
 * <br></br>
 * Detected fully-formed dates are: <br></br>
 *
 *  * dd/MM/yyyy
 *  * dd-MM-yyyy
 *  * yyyyMMdd
 *
 * Detected partially-formed dates are: <br></br>
 *
 *  * MM/yyyy
 *  * MM-yyyy
 *  * MMyyyy
 *  * yyyy
 *
 */
@Suppress("unused")
object FuzzyValues {
    const val MAX_FUZZY_DATE = 99991231
    const val MAX_FUZZY_TIME = 23_59_59
    const val MAX_FUZZY_DATETIME = 9999_12_31_23_59_59
    private val fuzzyDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

    fun strictIsFuzzyTime(fuzzyTime: Int): Boolean {
        if (fuzzyTime < 0) return false
        if (fuzzyTime > MAX_FUZZY_TIME) return false
        // Correctness of hour already validated by previous checks
        // Second > 0 already validated by previous checks
        val seconds = fuzzyTime % 100
        if (seconds > 59) return false
        // Minutes > 0 already validated by previous checks
        val minutes = (fuzzyTime / 100) % 100
        return minutes <= 59
    }

    fun strictTryParseFuzzyTime(fuzzyTime: Int): LocalTime? {
        if (fuzzyTime < 0) return null
        if (fuzzyTime > MAX_FUZZY_TIME) return null
        val hour = fuzzyTime / 1_00_00
        val minute = fuzzyTime / 1_00 % 1_00
        val second = fuzzyTime % 1_00
        return try {
            LocalTime.of(hour, minute, second)
        } catch (_: DateTimeException) {
            null
        }
    }

    fun strictTryParseFuzzyDate(fuzzyDate: Int): LocalDate? {
        if (fuzzyDate < 0) return null
        if (fuzzyDate > MAX_FUZZY_DATE) return null
        val year = fuzzyDate / 10000
        val month = fuzzyDate / 100 % 100
        val day = fuzzyDate % 100
        return try {
            LocalDate.of(year, month, day)
        } catch (_: DateTimeException) {
            null
        }
    }

    fun strictTryParseFuzzyDateTime(fuzzyDateTime: Long): LocalDateTime? {
        if (fuzzyDateTime < 0) return null
        if (fuzzyDateTime > MAX_FUZZY_DATETIME) return null
        val year = fuzzyDateTime / 1_00_00_00_00_00
        val month = fuzzyDateTime / 1_00_00_00_00 % 1_00
        val day = fuzzyDateTime/ 1_00_00_00 % 1_00
        val hour = fuzzyDateTime / 1_00_00 % 1_00
        val minute = fuzzyDateTime / 1_00 % 1_00
        val second = fuzzyDateTime % 1_00
        return try {
            LocalDateTime.of(year.toInt(), month.toInt(), day.toInt(), hour.toInt(), minute.toInt(), second.toInt())
        } catch (_: DateTimeException) {
            null
        }
    }

    fun getMaxRangeOf(text: String): Int {
        val fullyFormedDate = toYYYYMMDDString(text)
        val year = fullyFormedDate.substring(0, 4)
        val month = fullyFormedDate.substring(4, 6)
        val day = fullyFormedDate.substring(6, 8)
        val sb = StringBuilder(year)
        if (month == "00") {
            sb.append("99")
        } else {
            sb.append(month)
        }
        if (day == "00") {
            sb.append("99")
        } else {
            sb.append(day)
        }
        return sb.toString().toInt()
    }

    fun getDateTime(dateTime: Long): LocalDateTime? {
        var date = dateTime
        var h = 0
        var m = 0
        var s = 0
        var plusOne = false
        if (dateTime > MAX_FUZZY_DATE) {
            if (dateTime < 18000101000000L) {
                return Instant.ofEpochMilli(dateTime).atZone(ZoneId.systemDefault()).toLocalDateTime()
            }
            //Full date time format
            val time = dateTime % 1000000L
            date = dateTime / 1000000L
            h = (time / 10000L).toInt()
            m = (time / 100L % 100L).toInt()
            s = (time % 100L).toInt()
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
                s = 0
                m++
            }
            if (m == 60) {
                m = 0
                h++
            }
            if (h == 24) {
                h = 0
                plusOne = true
            }
        }
        val y = (date / 10000L).toInt()
        var mm = (date / 100L % 100L).toInt()
        var d = (date % 100L).toInt()
        if (mm == 0) {
            mm = 1
        }
        if (d == 0) {
            d = 1
        }
        return LocalDateTime.of(y, mm, d, h, m, s).plus(Period.ofDays(if (plusOne) 1 else 0))
    }

    fun getCurrentFuzzyDateTime(precision: TemporalUnit): Long {
        return getFuzzyDateTime(LocalDateTime.now(), precision)
    }

    val currentFuzzyDate: Long
        get() = getFuzzyDate(LocalDateTime.now(), ChronoUnit.DAYS)
    val currentFuzzyDateTime: Long
        get() = getCurrentFuzzyDateTime(ChronoUnit.SECONDS)

    fun getFuzzyDateTime(dateTime: LocalDateTime, precision: TemporalUnit): Long {
        var returnDateTime = dateTime
        val seconds = returnDateTime.second
        /*if (seconds == 0 && precision==ChronoUnit.SECONDS) {
			seconds = 60;
			dateTime = dateTime.minusMinutes(1);
		}*/
        var minutes = returnDateTime.minute
        if (minutes == 0 && precision === ChronoUnit.MINUTES) {
            minutes = 60
            returnDateTime = returnDateTime.minusHours(1)
        }
        var hours = returnDateTime.hour
        if (hours == 0 && precision === ChronoUnit.HOURS) {
            hours = 24
            returnDateTime = returnDateTime.minusDays(1)
        }
        return getFuzzyDate(
            returnDateTime,
            precision
        ) * 1000000L + if (precision === ChronoUnit.DAYS) 0 else hours * 10000L + if (precision === ChronoUnit.HOURS) 0 else minutes * 100L + if (precision === ChronoUnit.MINUTES) 0 else seconds
    }

    /**
     * Converts an epoch timestamp in milliseconds to a fuzzy date time in YYYYMMDDHHMMSS format.
     *
     * @param timestamp an epoch timestamp in milliseconds.
     * @param zoneOffset a [ZoneOffset].
     * @return a fuzzy date.
     */
    fun getFuzzyDateTime(timestamp: Long, zoneOffset: ZoneOffset = ZoneOffset.UTC) = LocalDateTime.ofEpochSecond(timestamp / 1000, 0, zoneOffset).format(fuzzyDateFormatter).toLong()

    fun getFuzzyDate(dateTime: LocalDateTime, precision: TemporalUnit): Long {
        return dateTime.year * 10000L + if (precision === ChronoUnit.YEARS) 0 else dateTime.monthValue * 100L + if (precision === ChronoUnit.MONTHS) 0 else dateTime.dayOfMonth
    }

    /**
     * Indicates if the submitted text is a fully-formed or partially-formed date.
     */
    fun isDate(text: String): Boolean {
        return isPartiallyFormedYYYYMMDD(text) || isPartiallyFormedDashDate(text) || isPartiallyFormedSlashDate(text)
    }

    /**
     * Indicates if the submitted text is a fully-formed date.
     */
    fun isFullDate(text: String): Boolean {
        return isFullyFormedYYYYMMDDDate(text) || isFullyFormedDashDate(text) || isFullyFormedSlashDate(text)
    }

    /**
     * Indicates if the submitted text has the format of a SSIN. <br></br>
     * It does **NOT** check if the SSIN is valid!
     */
    fun isSsin(text: String): Boolean {
        if (!NumberUtils.isDigits(text) || text.length != 11) {
            return false
        }
        val checkDigits = BigInteger(text.substring(9))
        val big97 = BigInteger("97")
        val modBefore2000 = BigInteger(text.substring(0, 9)).mod(big97)
        val modAfter2000 = BigInteger("2" + text.substring(0, 9)).mod(big97)
        return big97.subtract(modBefore2000) == checkDigits || big97.subtract(modAfter2000) == checkDigits
    }

    /**
     * Converts a text value into a YYYYMMDD integer, where DD and MM are replaced by 00 characters if the submitted
     * value does not contain the information to extract the day or month of the date.<br></br>
     * For example, submitting *11/2008* will return *20081100*. <br></br>
     * All dates detected by [.isFullDate] and [.isDate] will be converted.
     */
    fun toYYYYMMDD(text: String): Int {
        return toYYYYMMDDString(text).toInt()
    }

    private fun toYYYYMMDDString(text: String): String {
        val result: String
        val fields: Array<String> = if (isPartiallyFormedDashDate(text)) {
            text.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        } else if (isPartiallyFormedSlashDate(text)) {
            text.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        } else {
            arrayOf(text)
        }
        var day = "00"
        var month = "00"
        val year: String
        if (fields.size == 3) {
            day = if (fields[0].isEmpty()) "00" else String.format("%1$02d", fields[0].toInt())
            month = if (fields[1].isEmpty()) "00" else String.format("%1$02d", fields[1].toInt())
            year = if (fields[2].isEmpty()) "0000" else String.format("%1$04d", fields[2].toInt())
            result = year + month + day
        } else if (fields.size == 2) {
            month = if (fields[0].isEmpty()) "00" else String.format("%1$02d", fields[0].toInt())
            year = if (fields[1].isEmpty()) "0000" else String.format("%1$04d", fields[1].toInt())
            result = year + month + day
        } else {
            if (isPartiallyFormedYYYYMMDD(text)) {
                if (text.length <= 4) {
                    year = String.format("%1$04d", text.substring(0, 4).toInt())
                } else if (text.length <= 6) {
                    month = String.format("%1$02d", text.substring(4).toInt())
                    year = String.format("%1$04d", text.substring(0, 4).toInt())
                } else {
                    day = String.format("%1$02d", text.substring(6, 8).toInt())
                    month = String.format("%1$02d", text.substring(4, 6).toInt())
                    year = String.format("%1$04d", text.substring(0, 4).toInt())
                }
                result = year + month + day
            } else {
                result = text
            }
        }
        return result
    }

    private fun isFullyFormedDashDate(text: String): Boolean {
        return text.matches("(0?[1-9]|[12][0-9]|3[01])-(0?[1-9]|1[012])-(\\d{4})".toRegex())
    }

    private fun isFullyFormedSlashDate(text: String): Boolean {
        return text.matches("(0?[1-9]|[12][0-9]|3[01])/(0?[1-9]|1[012])/(\\d{4})".toRegex())
    }

    private fun isFullyFormedYYYYMMDDDate(text: String): Boolean {
        return text.matches("(\\d{4})(0?[1-9]|1[012])(0?[1-9]|[12][0-9]|3[01])".toRegex())
    }

    private fun isPartiallyFormedDashDate(text: String): Boolean {
        return text.matches("(0?[1-9]|[12][0-9]|3[01])?(-)?(0?[1-9]|1[012])-(\\d{4})".toRegex())
    }

    private fun isPartiallyFormedSlashDate(text: String): Boolean {
        return text.matches("(0?[1-9]|[12][0-9]|3[01])?(/)?(0?[1-9]|1[012])/(\\d{4})".toRegex())
    }

    private fun isPartiallyFormedYYYYMMDD(text: String): Boolean {
        return NumberUtils.isDigits(text) && text.matches("(\\d{4})(0?[1-9]|1[012])?(0?[1-9]|[12][0-9]|3[01])?".toRegex())
    }

    fun compare(left: Long, right: Long): Int {
        return java.lang.Long.valueOf(if (left < 29991231) left * 1000000 else left)
            .compareTo(if (right < 29991231) right * 1000000 else right)
    }
}

class MultiDateTime private constructor (
    val local: LocalDateTime,
    val fuzzy: Long,
    val epoch: Long
) {
    enum class Rounding {
        CEIL, FLOOR // TODO more if needed
    }

    companion object {
        fun fromLocalDateTime(local: LocalDateTime, fuzzyPrecision: ChronoUnit): MultiDateTime = MultiDateTime(
            local,
            FuzzyValues.getFuzzyDateTime(local, fuzzyPrecision),
            local.toInstant(ZoneOffset.UTC).toEpochMilli()
        )

        fun fromLocalDateTimeMinutePrecision(local: LocalDateTime, rounding: Rounding): MultiDateTime {
            val noSecond = local.withSecond(0).withNano(0)
            val rounded = when (rounding) {
                Rounding.CEIL -> {
                    if (local.second > 0 || local.nano > 0) noSecond.plusMinutes(1) else noSecond
                }
                Rounding.FLOOR -> noSecond
            }
            return fromLocalDateTime(rounded, ChronoUnit.MINUTES)
        }

        fun fromFuzzy(fuzzy: Long): MultiDateTime {
            val localDateTime = requireNotNull(FuzzyValues.strictTryParseFuzzyDateTime(fuzzy)) { "Invalid fuzzy date time $fuzzy" }
            return MultiDateTime(
                localDateTime,
                fuzzy,
                localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
            )
        }
    }
}