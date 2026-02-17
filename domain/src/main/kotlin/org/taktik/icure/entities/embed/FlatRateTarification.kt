/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.mergers.annotations.Mergeable
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Mergeable(["flatRateType"])
data class FlatRateTarification(
	val code: String? = null,
	val flatRateType: FlatRateType? = null,
	val label: Map<String, String>? = null,
	val valorisations: List<Valorisation> = emptyList(),
	override val encryptedSelf: String? = null,
) : Encryptable,
	Serializable
