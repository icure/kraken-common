/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.utils

/**
 * Created with IntelliJ IDEA.
 * User: aduchate
 * Date: 13/07/14
 * Time: 19:08
 * To change this template use File | Settings | File Templates.
 */
object CheckDigitVerhoeff {
	private fun inv(iPos: Int): Int {
		val invTable = intArrayOf(0, 4, 3, 2, 1, 5, 6, 7, 8, 9)
		return invTable[iPos]
	}

	private fun d(j: Int, k: Int): Int {
		val dTable = arrayOf(
			intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
			intArrayOf(1, 2, 3, 4, 0, 6, 7, 8, 9, 5),
			intArrayOf(2, 3, 4, 0, 1, 7, 8, 9, 5, 6),
			intArrayOf(3, 4, 0, 1, 2, 8, 9, 5, 6, 7),
			intArrayOf(4, 0, 1, 2, 3, 9, 5, 6, 7, 8),
			intArrayOf(5, 9, 8, 7, 6, 0, 4, 3, 2, 1),
			intArrayOf(6, 5, 9, 8, 7, 1, 0, 4, 3, 2),
			intArrayOf(7, 6, 5, 9, 8, 2, 1, 0, 4, 3),
			intArrayOf(8, 7, 6, 5, 9, 3, 2, 1, 0, 4),
			intArrayOf(9, 8, 7, 6, 5, 4, 3, 2, 1, 0),
		)
		return dTable[j][k]
	}

	private fun p(i: Int, Ni: Int): Int {
		val pTable = arrayOf(
			intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
			intArrayOf(1, 5, 7, 6, 2, 8, 3, 0, 9, 4),
			intArrayOf(5, 8, 0, 3, 7, 9, 6, 1, 4, 2),
			intArrayOf(8, 9, 1, 6, 0, 4, 3, 5, 2, 7),
			intArrayOf(9, 4, 5, 3, 1, 2, 6, 8, 7, 0),
			intArrayOf(4, 2, 8, 6, 5, 7, 3, 9, 0, 1),
			intArrayOf(2, 7, 9, 3, 8, 0, 6, 4, 1, 5),
			intArrayOf(7, 0, 4, 6, 9, 1, 3, 2, 5, 8),
		)
		return pTable[i % 8][Ni]
	}

	/**
	 * Computes the checksum C as
	 * C = inv(F_n (a_n)?F_(n-1) (a_(n-1) )?... ?F_1 (a_1 ) )
	 * (with ? being the multiplication in D_5)
	 * @param iNumber String charset to compute Verhoeff check digit
	 * @return the check digit
	 */
	fun computeCheckDigit(iNumber: String): Int {
		var checkSum = 0
		for (pos in 0 until iNumber.length) {
			checkSum = d(checkSum, p(pos + 1, iNumber[iNumber.length - pos - 1].digitToIntOrNull() ?: -1))
		}
		return inv(checkSum)
	}

	/**
	 * Verify the number in parameter (11 DIGITS + Verhoeff check digit = 12 DIGITS)
	 * The verification computes and verified the following equation
	 * (F_n (a_n )?F_(n-1) (a_(n-1) )?...?F_1 (a_1 )?C) = 0
	 * @param iNumber
	 * @return true if checked
	 */
	fun checkDigit(iNumber: String): Boolean {
		var checkSum = 0
		for (pos in 0 until iNumber.length) {
			checkSum = d(checkSum, p(pos, iNumber[iNumber.length - pos - 1].digitToIntOrNull() ?: -1))
		}
		return if (checkSum == 0) {
			true
		} else {
			false
		}
	}
}
