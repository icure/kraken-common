/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.embed

import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class FlatRateTarification(
	val code: String? = null,
	val flatRateType: FlatRateType? = null,
	val label: Map<String, String>? = null,
	val valorisations: List<Valorisation> = emptyList(),
	override val encryptedSelf: String? = null
) : Encryptable, Serializable
