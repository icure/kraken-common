/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.db

import org.apache.commons.lang3.StringUtils
import org.jetbrains.annotations.Contract
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.text.Normalizer

@Contract("null -> null; !null -> !null")
fun sanitizeString(key: String?): String? {
	return if (key == null) {
		null
	} else removeDiacriticalMarks(key).replace("\\s".toRegex(), "").replace("\\W".toRegex(), "").lowercase()
}

private fun removeDiacriticalMarks(key: String): String {
	try {
		return Normalizer.normalize(key.replace("ø".toRegex(), "o").replace("æ".toRegex(), "ae").replace("Æ".toRegex(), "AE").replace("Œ".toRegex(), "oe").replace("œ".toRegex(), "oe"), Normalizer.Form.NFD)
			.replace("\\p{InCombiningDiacriticalMarks}".toRegex(), "")
	} catch (ignored: NoSuchMethodException) { }
	//Fallback
	return key.replace("[\u00E8\u00E9\u00EA\u00EB]".toRegex(), "e")
		.replace("[\u00FB\u00F9\u00FC]".toRegex(), "u")
		.replace("[\u00E7]".toRegex(), "c")
		.replace("[\u00EF\u00EE\u00EC]".toRegex(), "i")
		.replace("[\u00E0\u00E2\u00E4]".toRegex(), "a")
		.replace("[\u00F6\u00F2\u00F4]".toRegex(), "o")
		.replace("[\u00C8\u00C9\u00CA\u00CB]".toRegex(), "E")
		.replace("[\u00DB\u00D9\u00DC]".toRegex(), "U")
		.replace("[\u00CF\u00CE\u00CC]".toRegex(), "I")
		.replace("[\u00C0\u00C2\u00C4]".toRegex(), "A")
		.replace("[\u00D4\u00D6\u00D2]".toRegex(), "O")
		.replace("ø".toRegex(), "o")
		.replace("æ".toRegex(), "ae")
		.replace("Æ".toRegex(), "AE")
		.replace("Œ".toRegex(), "oe")
		.replace("œ".toRegex(), "oe")
}

fun safeConcat(vararg components: String?): String {
	val sb = StringBuffer()
	for (c in components) {
		if (c != null) {
			sb.append(c)
		}
	}
	return sb.toString()
}

fun detectFrenchCp850Cp1252(data: ByteArray?): String? {
	val br: BufferedReader
	var score = 0
	try {
		br = BufferedReader(InputStreamReader(ByteArrayInputStream(data), "cp850"))
		var c: Int
		while (br.read().also { c = it } != -1) {
			if (c == '\u00e8'.code) { // from cp850 (likely): LATIN SMALL LETTER E WITH GRAVE
// from cp1252 (unlikely): LATIN CAPITAL LETTER S WITH CARON
				score++
			} else if (c == '\u00e9'.code) { // from cp850 (likely): LATIN SMALL LETTER E WITH ACUTE
// from cp1252 (unlikely): SINGLE LOW-9 QUOTATION MARK
				score++
			} else if (c == '\u00e0'.code) { // from cp850 (likely): LATIN SMALL LETTER A WITH GRAVE
// from cp1252 (unlikely): HORIZONTAL ELLIPSIS
				score++
			} else if (c == '\u00e7'.code) { // from cp850 (likely): LATIN SMALL LETTER C WITH CEDILLA
// from cp1252 (unlikely): DOUBLE DAGGER
				score++
			} else if (c == '\u00b3'.code) { // from cp850 (likely): SUPERSCRIPT THREE
// from cp1252 (unlikely): LATIN SMALL LETTER U WITH DIAERESIS
				score++
			} else if (c == '\u00b5'.code) { // from cp850 (likely): MICRO SIGN
// from cp1252 (unlikely): LATIN SMALL LETTER AE
				score++
			} else if (c == '\u00d3'.code) { // from cp850 (unlikely): LATIN CAPITAL LETTER O WITH ACUTE
// from cp1252 (likely): LATIN SMALL LETTER A WITH GRAVE
				score--
			} else if (c == '\u00fe'.code) { // from cp850 (unlikely): LATIN SMALL LETTER THORN
// from cp1252 (likely): LATIN SMALL LETTER C WITH CEDILLA
				score--
			} else if (c == '\u00de'.code) { // from cp850 (unlikely): LATIN CAPITAL LETTER THORN
// from cp1252 (likely): LATIN SMALL LETTER E WITH GRAVE
				score--
			} else if (c == '\u00da'.code) { // from cp850 (unlikely): LATIN CAPITAL LETTER U WITH ACUTE
// from cp1252 (likely): LATIN SMALL LETTER E WITH ACUTE
				score--
			} else if (c == '\u00c1'.code) { // from cp850 (unlikely): LATIN CAPITAL LETTER A WITH ACUTE
// from cp1252 (likely): MICRO SIGN
				score--
			} else if (c == '\u2502'.code) { // from cp850 (unlikely): BOX DRAWINGS LIGHT VERTICAL
// from cp1252 (likely): SUPERSCRIPT THREE
				score--
			}
		}
		return if (score > 0) "cp850" else "cp1252"
	} catch (e: Exception) {
		e.printStackTrace()
	}
	return null
}

fun equals(s1: String?, s2: String?): Boolean {
	return s1 != null && s2 != null && (StringUtils.equals(s1, s2) || StringUtils.equals(sanitizeString(s1), sanitizeString(s2)))
}

@Contract("null -> null; !null -> !null")
fun String?.sanitize() = sanitizeString(this)