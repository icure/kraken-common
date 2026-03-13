/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.mergers.annotations.Mergeable
import java.io.Serializable

/**
 * Created by aduchate on 21/01/13, 14:47
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Mergeable(["telecomType", "telecomNumber"])
data class Telecom(
	val telecomType: TelecomType? = null,
	val telecomNumber: String? = null,
	val telecomDescription: String? = null,
	override val encryptedSelf: String? = null,
) : Encryptable,
	Serializable,
	Comparable<Telecom> {

	override fun compareTo(other: Telecom): Int = telecomType?.compareTo(other.telecomType ?: TelecomType.other) ?: 0
}
