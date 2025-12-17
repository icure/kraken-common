package org.taktik.icure.utils

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.sanctionco.jmail.JMail
import java.time.ZoneId


object Validation {
	private val strictPhoneRegex = Regex("^\\+\\d+$")
	private val phoneUtil = PhoneNumberUtil.getInstance()

	fun validEmail(email: String): Boolean =
		JMail.isValid(email)

	/**
	 * Very strict phone validation.
	 * The number must be in international format, and except for the starting `+` symbol can't contain any non-digit
	 * characters.
	 */
	fun validPhone(number: String): Boolean {
		try {
			if (!strictPhoneRegex.matches(number)) return false
			val phoneNumber = phoneUtil.parse(number, null)
			return phoneUtil.isValidNumber(phoneNumber) && phoneNumber.hasCountryCode()
		} catch (_: NumberParseException) {
			return false
		}
	}

	fun validZoneId(zoneId: String): Boolean =
		try {
			ZoneId.of(zoneId)
			true
		} catch (_: Exception) {
			false
		}
}
