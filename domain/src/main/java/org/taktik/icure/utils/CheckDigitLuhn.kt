/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.utils

/**
 * Apply Luhn algorithm to compute check digit
 * This algorithm is used to compute the first check digit
 * during extended unique id generation
 *
 * @author Ciedmdr
 */
object CheckDigitLuhn {
	/**
	 * Computes the checksum C according Luhn algorithm
	 * @param iNumber String charset to compute Luhn check digit
	 * @return the check digit
	 */
	fun computeCheckDigit(iNumber: String): Int {
		var checkSum = 0
		var weight: Int
		var weightedDigit: Int
		for (pos in iNumber.indices) {
			weight = if (pos % 2 == 0) 2 else 1
			weightedDigit = (iNumber[iNumber.length - pos - 1].digitToIntOrNull() ?: -1) * weight
			checkSum += if (weightedDigit > 9) weightedDigit - 9 else weightedDigit
		}
		return (10 - checkSum % 10) % 10
	}

	/**
	 * Verify the number in parameter (11 DIGITS + Luhn check digit = 12 DIGITS)
	 * @param iNumber
	 * @return true if checked
	 */
	fun checkDigit(iNumber: String): Boolean {
		var checkSum = 0
		var weight = 0
		var weightedDigit: Int
		for (pos in iNumber.indices) {
			weight = if (pos % 2 == 0) 1 else 2
			weightedDigit = (iNumber[iNumber.length - pos - 1].digitToIntOrNull() ?: -1) * weight
			checkSum += if (weightedDigit > 9) weightedDigit - 9 else weightedDigit
		}
		return if (checkSum % 10 == 0) true else false
	}
}
