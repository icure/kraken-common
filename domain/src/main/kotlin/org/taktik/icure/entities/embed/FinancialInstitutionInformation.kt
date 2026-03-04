/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class FinancialInstitutionInformation(
	val name: String? = null,
	val key: String? = null,
	val bankAccount: String? = null,
	val bic: String? = null,
	val proxyBankAccount: String? = null,
	val proxyBic: String? = null,
	val preferredFiiForPartners: Set<String> = emptySet(), // Insurance Id, Hcp Id
	override val encryptedSelf: String? = null,
) : Encryptable,
	Serializable
