/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.gui.type

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable

/**
 * Created by aduchate on 19/11/13, 10:33
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class Measure(
	val label: String = "",
	val unit: String = "",
	val value: Number? = null,
	val minRef: Number? = null,
	val maxRef: Number? = null,
	val severity: Number = 0,
) : Data(),
	Serializable {
	fun checkValue(): Int {
		if (severity != null && severity!!.toInt() > 0) {
			return TOO_HIGHT
		}
		return if (minRef == null) {
			if (maxRef == null) {
				OK
			} else {
				if (value == null) return OK
				if (value!!.toDouble() > maxRef!!.toDouble()) {
					TOO_HIGHT
				} else {
					OK
				}
			}
		} else {
			if (maxRef == null) {
				if (value == null) return OK
				if (value!!.toDouble() < minRef!!.toDouble()) {
					TOO_LOW
				} else {
					OK
				}
			} else {
				if (minRef == maxRef) {
					return OK
				}
				if (value == null) return OK
				if (maxRef!!.toDouble() > minRef!!.toDouble() && value!!.toDouble() > maxRef!!.toDouble()) {
					TOO_HIGHT
				} else if (value!!.toDouble() < minRef!!.toDouble()) {
					TOO_LOW
				} else {
					OK
				}
			}
		}
	}

	val restriction: String?
		get() =
			if (minRef == null) {
				if (maxRef == null) {
					null
				} else {
					"<" + maxRef
				}
			} else {
				if (maxRef == null) {
					">" + minRef
				} else {
					if (minRef == maxRef) {
						null
					} else {
						"" + minRef + "-" + maxRef
					}
				}
			}

	companion object {
		const val OK = 0
		const val TOO_LOW = 1
		const val TOO_HIGHT = 2
	}
}
