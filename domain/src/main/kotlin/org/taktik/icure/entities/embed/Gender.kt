/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import org.taktik.icure.entities.base.EnumVersion
import java.io.Serializable

/**
 * Created by aduchate on 21/01/13, 14:56
 */
@EnumVersion(1L)
enum class Gender(val code: String) : Serializable {
	male("M"),
	female("F"),
	indeterminate("I"),
	changed("C"),
	changedToMale("Y"),
	changedToFemale("X"),
	unknown("U"),
	;

	override fun toString(): String = code

	companion object {
		fun fromCode(code: String?): Gender? = code?.let { c -> entries.find { c == it.code } }
	}
}
