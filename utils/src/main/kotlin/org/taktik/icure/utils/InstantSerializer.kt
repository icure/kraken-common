/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.utils

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

class InstantSerializer : JsonSerializer<Instant?>() {
	@Throws(IOException::class)
	override fun serialize(value: Instant?, jgen: JsonGenerator, provider: SerializerProvider) {
		jgen.writeNumber(getBigDecimal(value))
	}

	protected fun getBigDecimal(value: Instant?): BigDecimal = BigDecimal.valueOf(1000L * value!!.epochSecond).add(
		BigDecimal.valueOf(value.nano.toLong()).divide(
			_1000000,
		).setScale(0, RoundingMode.HALF_UP),
	)

	override fun isEmpty(value: Instant?): Boolean = value == null

	companion object {
		private val _1000000 = BigDecimal.valueOf(1000000)
	}
}
