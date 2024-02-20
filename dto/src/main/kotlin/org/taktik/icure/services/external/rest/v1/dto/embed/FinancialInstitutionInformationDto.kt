/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.embed

import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class FinancialInstitutionInformationDto(
	val name: String? = null,
	val key: String? = null,
	val bankAccount: String? = null,
	val bic: String? = null,
	val proxyBankAccount: String? = null,
	val proxyBic: String? = null,
	val preferredFiiForPartners: Set<String> = emptySet(), //InsuranceDto Id, Hcp Id
	override val encryptedSelf: String? = null
) : EncryptedDto, Serializable
