/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
//@Mergeable(["flatRateType"])
data class FlatRateTarification(
	/** The tarification code. */
	val code: String? = null,
	/** The type of flat rate (physician, physiotherapist, nurse, or ptd). */
	val flatRateType: FlatRateType? = null,
	/** Localized labels for this tarification, keyed by language code. */
	val label: Map<String, String>? = null,
	/** The list of valorisations associated with this tarification. */
	val valorisations: List<Valorisation> = emptyList(),
	/** The base64-encoded encrypted content. */
	override val encryptedSelf: String? = null,
) : Encryptable,
	Serializable
