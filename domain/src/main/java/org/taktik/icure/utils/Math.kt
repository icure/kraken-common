/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.utils

import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.RealMatrix
import java.lang.Math
import java.util.*

object Math {
	var obsXs = doubleArrayOf(28.0, 30.0, 32.0, 34.0, 36.0, 38.0, 40.0, 42.0)
	var obsXsW50 = doubleArrayOf(
		16.0,
		17.0,
		18.0,
		19.0,
		20.0,
		21.0,
		22.0,
		23.0,
		24.0,
		25.0,
		26.0,
		27.0,
		28.0,
		30.0,
		32.0,
		34.0,
		36.0,
		38.0,
		40.0,
		42.0,
	)
	var obsWeights10 = doubleArrayOf(920.0, 1210.0, 1510.0, 1930.0, 2290.0, 2590.0, 2840.0, 2980.0)
	var obsWeights25 = doubleArrayOf(1080.0, 1360.0, 1730.0, 2160.0, 2550.0, 2850.0, 3090.0, 3200.0)
	var obsWeights50 = doubleArrayOf(
		142.0,
		176.0,
		218.0,
		267.0,
		324.0,
		390.0,
		465.0,
		551.0,
		647.0,
		754.0,
		872.0,
		1001.0,
		1200.0,
		1610.0,
		2060.0,
		2480.0,
		2850.0,
		3130.0,
		3370.0,
		3490.0,
	)
	var obsWeights75 = doubleArrayOf(1470.0, 2180.0, 2640.0, 2970.0, 3240.0, 3440.0, 3690.0, 3780.0)
	var obsWeights90 = doubleArrayOf(1900.0, 2540.0, 2960.0, 3270.0, 3520.0, 3720.0, 3910.0, 4080.0)
	var obsHeights10 = doubleArrayOf(35.0, 37.6, 40.5, 43.0, 45.3, 46.6, 47.5, 47.7)
	var obsHeights25 = doubleArrayOf(36.8, 39.9, 40.5, 45.0, 46.5, 47.7, 48.7, 49.4)
	var obsHeights50 = doubleArrayOf(37.8, 40.9, 44.3, 46.4, 48.0, 49.2, 50.0, 50.5)
	var obsHeights75 = doubleArrayOf(40.5, 45.4, 47.0, 48.4, 49.6, 50.5, 51.2, 51.9)
	var obsHeights90 = doubleArrayOf(44.0, 47.8, 49.1, 49.9, 50.0, 51.4, 57.1, 52.8)
	var obsW10x = rlsInterpolation(obsXs, obsWeights10, 3)
	var obsW25x = rlsInterpolation(obsXs, obsWeights25, 3)
	var obsW50x = rlsInterpolation(obsXsW50, obsWeights50, 3)
	var obsW75x = rlsInterpolation(obsXs, obsWeights75, 3)
	var obsW90x = rlsInterpolation(obsXs, obsWeights90, 3)
	var obsH10x = rlsInterpolation(obsXs, obsHeights10, 3)
	var obsH25x = rlsInterpolation(obsXs, obsHeights25, 3)
	var obsH50x = rlsInterpolation(obsXs, obsHeights50, 3)
	var obsH75x = rlsInterpolation(obsXs, obsHeights75, 3)
	var obsH90x = rlsInterpolation(obsXs, obsHeights90, 3)
	fun polValue(x: Double, coeffs: DoubleArray?): Double {
		var `val` = 0.0
		var pow = 1.0
		for (i in coeffs!!.indices) {
			`val` += pow * coeffs[i]
			pow *= x
		}
		return `val`
	}

	fun mean(items: Collection<Double?>): Double {
		var result = 0.0
		var n = 0
		for (d in items) {
			if (d != null) {
				n++
				result += d
			}
		}
		return result / n
	}

	fun sum(items: Collection<Double?>): Double {
		var result = 0.0
		for (d in items) {
			if (d != null) {
				result += d
			}
		}
		return result
	}

	fun rlsInterpolation(x: DoubleArray, y: DoubleArray, pow: Int): DoubleArray? {
		if (pow < 1) {
			return null
		}
		val coeffs = DoubleArray(pow + 1)
		val d = 1000.0
		for (i in 0 until pow + 1) {
			coeffs[i] = 0.0
		}
		val pMtx = Array(pow + 1) { DoubleArray(pow + 1) }
		for (i in 0 until pow + 1) {
			for (j in 0 until pow + 1) {
				pMtx[i][j] = if (i == j) d else 0.0
			}
		}
		var wV: RealMatrix = Array2DRowRealMatrix(coeffs)
		var pM: RealMatrix = Array2DRowRealMatrix(pMtx)
		for (k in x.indices) {
			val xx = x[k]
			val yy = y[k]
			val xV: RealMatrix = Array2DRowRealMatrix(pow + 1, 1)
			var aPow = 1.0
			for (i in 0 until pow + 1) {
				xV.setEntry(i, 0, aPow)
				aPow *= xx
			}
			val alpha: Double = yy - wV.transpose().multiply(xV).getEntry(0, 0)
			val gV: RealMatrix =
				pM.multiply(xV).scalarMultiply(1 / (1.0 + xV.transpose().multiply(pM).multiply(xV).getEntry(0, 0)))
			pM = pM.subtract(gV.multiply(xV.transpose()).multiply(pM))
			wV = wV.add(gV.scalarMultiply(alpha))
		}
		return wV.getColumn(0)
	}

	fun obsPercWeight(weeks: Double, weight: Double): Double {
		val p10 = polValue(weeks, obsW10x)
		val p25 = polValue(weeks, obsW25x)
		val p50 = polValue(weeks, obsW50x)
		val p75 = polValue(weeks, obsW75x)
		val p90 = polValue(weeks, obsW90x)
		val cs =
			rlsInterpolation(doubleArrayOf(p10, p25, p50, p75, p90), doubleArrayOf(10.0, 25.0, 50.0, 75.0, 90.0), 3)
		return polValue(weight, cs) / 100.0
	}

	fun obsWeightPerc(weeks: Double, perc: Double): Double {
		val p10 = polValue(weeks, obsW10x)
		val p25 = polValue(weeks, obsW25x)
		val p50 = polValue(weeks, obsW50x)
		val p75 = polValue(weeks, obsW75x)
		val p90 = polValue(weeks, obsW90x)
		val cs =
			rlsInterpolation(doubleArrayOf(10.0, 25.0, 50.0, 75.0, 90.0), doubleArrayOf(p10, p25, p50, p75, p90), 3)
		return polValue(perc * 100.0, cs)
	}

	fun obsWeights(ac: Double?, hc: Double?, bipd: Double?, fl: Double?): Map<String, Double?> {
		var ac1 = ac
		var hc1 = hc
		var bipd1 = bipd
		var fl1 = fl
		if (ac1 != null && ac1 == 0.0) {
			ac1 = null
		}
		if (hc1 != null && hc1 == 0.0) {
			hc1 = null
		}
		if (bipd1 != null && bipd1 == 0.0) {
			bipd1 = null
		}
		if (fl1 != null && fl1 == 0.0) {
			fl1 = null
		}
		val results: MutableMap<String, Double?> = HashMap()
		val results1: MutableMap<String, Double?> = HashMap()
		val results2: MutableMap<String, Double?> = HashMap()
		val results3: MutableMap<String, Double?> = HashMap()
		val results4: MutableMap<String, Double?> = HashMap()
		if (ac1 != null) {
			results1["Jordaan"] = Math.pow(
				10.0,
				0.6328 + 0.01881 * ac1 - 0.000043 * ac1 * ac1 + 0.000000036239 * Math.pow(ac1, 3.0),
			)
			results1["Higginbottom"] = 0.0816 * Math.pow(ac1, 3.0) / 1000
			results1["Campbell"] = Math.pow(2.718281, -4.564 + 0.0282 * ac1 - 0.0000331 * ac1 * ac1) * 1000
			results1["Hadlock AC"] = Math.pow(2.718281, 2.695 + 0.0253 * ac1 - 0.0000275 * Math.pow(ac1, 2.0))
			results1["Warsof"] = Math.pow(10.0, -1.8367 + 0.092 * ac1 / 10 - 0.000019 * Math.pow(ac1, 3.0) / 1000) * 1000
		}
		if (fl1 != null) {
			results1["Warsof et al. 1986"] = Math.pow(
				2.718281,
				4.6914 + 0.00151 * Math.pow(fl1, 2.0) - 0.0000119 * Math.pow(fl1, 3.0),
			)
		}
		if (ac1 != null && fl1 != null) {
			results2["Woo et al. 1985"] = Math.pow(10.0, 0.59 + 0.008 * ac1 + 0.028 * fl1 - 0.0000716 * ac1 * fl1)
			results2["Hadlock et al. 1985"] =
				Math.pow(10.0, 1.304 + 0.005281 * ac1 + 0.01938 * fl1 - 0.00004 * ac1 * fl1)
		}
		if (ac1 != null && bipd1 != null) {
			results2["Hsieh et al. 1987"] = Math.pow(
				10.0,
				2.1315 +
					0.000056541 *
					ac1 *
					bipd1 -
					0.00000015515 *
					bipd1 *
					Math.pow(
						ac1,
						2.0,
					) +
					0.000000019782 *
					Math.pow(ac1, 3.0) +
					0.0052594 *
					bipd1,
			)
			results2["Vintzileos et al. 1987"] = Math.pow(10.0, 1.879 + 0.0084 * bipd1 + 0.0026 * ac1)
			results2["Woo et al. 1985"] = Math.pow(
				10.0,
				1.63 +
					0.016 *
					bipd1 +
					0.0000111 *
					Math.pow(ac1, 2.0) -
					0.0000000859 *
					bipd1 *
					Math.pow(
						ac1,
						2.0,
					),
			)
			results2["Hadlock et al. 1984"] = Math.pow(
				10.0,
				1.1134 +
					0.005845 *
					ac1 -
					0.00000604 *
					Math.pow(ac1, 2.0) -
					0.00007365 *
					Math.pow(
						bipd1,
						2.0,
					) +
					0.00000595 *
					bipd1 *
					ac1 +
					0.01694 *
					bipd1,
			)
			results2["Jordaan. 1983"] =
				Math.pow(10.0, -1.1683 + 0.00377 * ac1 + 0.0095 * bipd1 - 0.000015 * bipd1 * ac1) * 1000
			results2["Warsof et al. 1977"] = Math.pow(
				10.0,
				-1.599 + 0.0144 * bipd1 + 0.0032 * ac1 - 0.000000111 * Math.pow(bipd1, 2.0) * ac1,
			) *
				1000
			results2["Shepard et al. 1982"] =
				Math.pow(10.0, -1.7492 + 0.0166 * bipd1 + 0.0046 * ac1 - 0.00002546 * ac1 * bipd1) * 1000
		}
		if (ac1 != null && hc1 != null) {
			results2["Hadlock"] = Math.pow(
				10.0,
				1.182 + 0.0273 * hc1 / 10 + 0.07057 * ac1 / 10 - 0.00063 * ac1 * ac1 / 100 - 0.0002184 * ac1 * ac1 / 100,
			)
			results2["Jordan"] = Math.pow(10.0, 0.9119 + 0.00488 * hc1 + 0.00824 * ac1 - 0.00001599 * hc1 * ac1)
		}
		if (ac1 != null && bipd1 != null && fl1 != null) {
			results3["Hadlock et al. 1985"] =
				Math.pow(10.0, 1.335 - 0.000034 * ac1 * fl1 + 0.00316 * bipd1 + 0.00457 * ac1 + 0.01623 * fl1)
			results3["Hsieh et al. 1987"] = Math.pow(
				10.0,
				2.7193 +
					0.000094962 *
					ac1 *
					bipd1 -
					0.01432 *
					fl1 -
					0.00000076742 *
					ac1 *
					Math.pow(
						bipd1,
						2.0,
					) +
					0.000001745 *
					fl1 *
					Math.pow(bipd1, 2.0),
			)
			results3["Shinozuka et al. 1987"] = 0.00023966 * Math.pow(ac1, 2.0) * fl1 + 0.001623 * Math.pow(bipd1, 3.0)
			results3["Woo et al. 1985"] = Math.pow(
				10.0,
				1.54 +
					0.015 *
					bipd1 +
					0.0000111 *
					Math.pow(ac1, 2.0) -
					0.0000000764 *
					bipd1 *
					Math.pow(
						ac1,
						2.0,
					) +
					0.005 *
					fl1 -
					0.00000992 *
					fl1 *
					ac1,
			)
		}
		if (ac1 != null && hc1 != null && bipd1 != null) {
			results3["Jordaan. 1983"] = Math.pow(10.0, 2.3231 + 0.002904 * ac1 + 0.00079 * hc1 - 0.00058 * bipd1)
		}
		if (ac1 != null && hc1 != null && fl1 != null) {
			results3["Hadlock et al. 1985"] =
				Math.pow(10.0, 1.326 - 0.0000326 * ac1 * fl1 + 0.00107 * hc1 + 0.00438 * ac1 + 0.0158 * fl1)
			results3["Hadlock et al. 1985"] =
				Math.pow(10.0, 1.326 - 0.0000326 * ac1 * fl1 + 0.00107 * hc1 + 0.00438 * ac1 + 0.0158 * fl1)
			results3["Hadlock et al. 1985"] =
				Math.pow(10.0, 1.326 - 0.0000326 * ac1 * fl1 + 0.00107 * hc1 + 0.00438 * ac1 + 0.0158 * fl1)
			results3["Combs et al. 1993"] = 0.00023718 *
				Math.pow(ac1, 2.0) *
				fl1 +
				0.00003312 *
				Math.pow(
					hc1,
					3.0,
				)
			results3["Ott et al. 1986"] = Math.pow(
				10.0,
				-2.0661 + 0.004355 * hc1 + 0.005394 * ac1 - 0.000008582 * hc1 * ac1 + 1.2594 * (fl1 / ac1),
			) *
				1000
			results3["Hadlock et al. 1985"] =
				Math.pow(10.0, 1.326 - 0.0000326 * ac1 * fl1 + 0.00107 * hc1 + 0.00438 * ac1 + 0.0158 * fl1)
		}
		if (ac1 != null && hc1 != null && fl1 != null && bipd1 != null) {
			results4["Hadlock et al. 1985"] = Math.pow(
				10.0,
				1.3596 + 0.00064 * hc1 + 0.00424 * ac1 + 0.0174 * fl1 + 0.0000061 * bipd1 * ac1 - 0.0000386 * ac1 * fl1,
			)
		}

		// Compute weighted means
		var mean: Double? = null
		if (results4.size > 0) {
			mean = (mean(results4.values) * 4 + sum(results3.values)) / (4 + results3.size)
		} else if (results3.size > 0) {
			mean = (mean(results3.values) * 4 + sum(results2.values)) / (4 + results2.size)
		} else if (results2.size > 0) {
			mean = (mean(results2.values) * 4 + sum(results1.values)) / (4 + results1.size)
		} else if (results1.size > 0) {
			mean = mean(results1.values)
		}
		results.putAll(results1)
		results.putAll(results2)
		results.putAll(results3)
		results.putAll(results4)
		if (mean != null) {
			results["mean"] = mean
		}
		return results
	}

	fun percentile(desc: String, x: Double, y: Double): Double {
		var prevperc: Double? = null
		var prevval: Double? = null
		var result: Double? = null
		for (row in desc.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
			val perc = java.lang.Double.valueOf(
				row.split(">".toRegex()).dropLastWhile { it.isEmpty() }
					.toTypedArray()[0],
			)
			val vals = row.split(">".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
			val `val` = interpolate(vals, x)
			if (`val`!! >= y) {
				result = if (prevval != null) {
					((y - prevval) * perc + (`val` - y) * prevperc!!) / (`val` - prevval)
				} else {
					perc
				}
				break
			}
			prevperc = perc
			prevval = `val`
		}
		return result ?: prevperc!!
	}

	fun interpolate(vals: String, x: Double): Double? {
		var `val`: Double? = null
		var preva: Double? = null
		var prevb: Double? = null
		for (pair in vals.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
			val a = java.lang.Double.valueOf(pair.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0])
			val b = java.lang.Double.valueOf(pair.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
			if (a >= x) {
				`val` = if (preva != null) {
					((x - preva) * b + (a - x) * prevb!!) / (a - preva)
				} else {
					b
				}
				break
			}
			preva = a
			prevb = b
		}
		if (`val` == null) {
			`val` = prevb
		}
		return `val`
	}

	fun isNissValid(niss: String): Boolean {
		if (niss.length != 11 && niss.length != 13) {
			return false
		}
		return if (niss.length == 13) {
			CheckDigitLuhn.checkDigit(niss.substring(0, 12)) &&
				CheckDigitVerhoeff.checkDigit(
					niss.substring(
						0,
						11,
					) +
						niss.substring(12, 13),
				)
		} else {
			val luxYear = java.lang.Long.valueOf(niss.substring(0, 4))
			if (luxYear > 1900 && luxYear <= GregorianCalendar().get(Calendar.YEAR)) {
				val luxMonth = java.lang.Long.valueOf(niss.substring(4, 6))
				val luxDay = java.lang.Long.valueOf(niss.substring(6, 8))
				if (luxMonth < 13 && luxDay < 32) {
					val sumprod = java.lang.Long.valueOf(niss.substring(0, 1)) *
						5 +
						java.lang.Long.valueOf(
							niss.substring(
								1,
								2,
							),
						) *
						4 +
						java.lang.Long.valueOf(niss.substring(2, 3)) *
						3 +
						java.lang.Long.valueOf(
							niss.substring(
								3,
								4,
							),
						) *
						2 +
						java.lang.Long.valueOf(niss.substring(4, 5)) *
						7 +
						java.lang.Long.valueOf(
							niss.substring(
								5,
								6,
							),
						) *
						6 +
						java.lang.Long.valueOf(niss.substring(6, 7)) *
						5 +
						java.lang.Long.valueOf(
							niss.substring(
								7,
								8,
							),
						) *
						4 +
						java.lang.Long.valueOf(niss.substring(8, 9)) *
						3 +
						java.lang.Long.valueOf(
							niss.substring(
								9,
								10,
							),
						) *
						2
					var check = 11 - sumprod % 11
					if (check == 11L) {
						check = 0
					}
					if (check == 10L) {
						// Not a valid SNS number shouldn't have been attributed
					} else {
						if (check == java.lang.Long.valueOf(niss.substring(10, 11))) {
							return true
						} else {
							check = 12 - sumprod % 11
							if (check == 12L) {
								check = 1
							}
							if (check == 11L) {
								check = 0
							}
							if (check == java.lang.Long.valueOf(niss.substring(10, 11))) {
								return true
							}
						}
					}
				}
			}
			val number = java.lang.Long.valueOf(niss.substring(0, 9))
			val checkDigits = java.lang.Long.valueOf(niss.substring(9, 11))
			val year = Integer.valueOf(niss.substring(0, 2))
			val ck1 = 97L - number % 97L == checkDigits
			val ck2 = 97L - (2000000000L + number) % 97L == checkDigits
			if (year + 2000 > GregorianCalendar().get(Calendar.YEAR)) {
				ck1
			} else {
				ck1 || ck2
			}
		}
	}
}
