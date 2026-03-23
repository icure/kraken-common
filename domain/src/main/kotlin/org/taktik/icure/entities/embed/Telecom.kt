/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.RawJson
import org.taktik.icure.utils.DynamicInitializer
import org.taktik.icure.utils.invoke
import java.io.Serializable

/**
 * Created by aduchate on 21/01/13, 14:47
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Telecom(
	val telecomType: TelecomType? = null,
	val telecomNumber: String? = null,
	val telecomDescription: String? = null,
	override val encryptedSelf: String? = null,
	override val extensions: RawJson.JsonObject? = null
) : Encryptable,
	Serializable,
	Comparable<Telecom>,
	Extendable {
	companion object : DynamicInitializer<Telecom>

	fun merge(other: Telecom) = Telecom(args = this.solveConflictsWith(other))
	fun solveConflictsWith(other: Telecom) = super<Encryptable>.solveConflictsWith(other) +
		mapOf(
			"telecomType" to (this.telecomType ?: other.telecomType),
			"telecomNumber" to (this.telecomNumber ?: other.telecomNumber),
			"telecomDescription" to (this.telecomDescription ?: other.telecomDescription),
		)

	override fun compareTo(other: Telecom): Int = telecomType?.compareTo(other.telecomType ?: TelecomType.other) ?: 0
}
