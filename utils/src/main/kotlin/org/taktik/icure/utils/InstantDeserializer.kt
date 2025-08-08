/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.utils

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import java.io.IOException
import java.math.BigDecimal
import java.time.Instant

class InstantDeserializer : JsonDeserializer<Instant>() {
	@Throws(IOException::class)
	override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): Instant? = when {
		jp.currentToken.isNumeric -> jp.decimalValue
		jp.currentToken.isScalarValue -> BigDecimal(jp.valueAsString)
		else -> null
	}?.let { getInstant(it) }

	private fun getInstant(decVal: BigDecimal): Instant = Instant.ofEpochSecond(decVal.divide(_1000).toLong(), decVal.remainder(_1000).multiply(_1000000).toLong())

	companion object {
		private val _1000000 = BigDecimal.valueOf(1000000)
		private val _1000 = BigDecimal.valueOf(1000)
	}
}
