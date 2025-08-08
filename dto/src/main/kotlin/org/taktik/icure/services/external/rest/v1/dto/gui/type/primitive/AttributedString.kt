/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.gui.type.primitive

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v1.dto.gui.type.Data
import java.io.Serializable
import java.io.UnsupportedEncodingException

/**
 * Created by aduchate on 19/11/13, 10:41
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class AttributedString(
	val rtfString: String? = null,
	val rtfData: ByteArray? = null,
) : Data(),
	Primitive {
	fun length(): Int = if (rtfString!!.length > 0) rtfString!!.length else rtfData!!.size

	override fun getPrimitiveValue(): Serializable? = try {
		String(rtfData!!, Charsets.UTF_8)
	} catch (e: UnsupportedEncodingException) {
		throw IllegalStateException(e)
	}

	companion object {
		fun getRtfUnicodeEscapedString(s: String?): String {
			val sb = StringBuilder()
			for (i in 0 until s!!.length) {
				val c = s[i]
				if (c.code == 0x0a || c.code == 0x0d) {
					sb.append("\\line\n")
				} else if (c.code <= 0x7f) {
					sb.append(c)
				} else {
					sb
						.append(
							"\\u",
						).append(
							c.code,
						).append("?")
				}
			}
			return sb.toString()
		}
	}
}
