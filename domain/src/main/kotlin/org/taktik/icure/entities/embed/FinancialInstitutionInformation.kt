/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.annotations.entities.ContentValue
import org.taktik.icure.annotations.entities.ContentValues
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class FinancialInstitutionInformation(
	@param:ContentValue(ContentValues.ANY_STRING) val name: String? = null,
	@param:ContentValue(ContentValues.ANY_STRING) val key: String? = null,
	@param:ContentValue(ContentValues.ANY_STRING) val bankAccount: String? = null,
	@param:ContentValue(ContentValues.ANY_STRING) val bic: String? = null,
	@param:ContentValue(ContentValues.ANY_STRING) val proxyBankAccount: String? = null,
	@param:ContentValue(ContentValues.ANY_STRING) val proxyBic: String? = null,
	val preferredFiiForPartners: Set<String> = emptySet(), //Insurance Id, Hcp Id
	override val encryptedSelf: String? = null
) : Encryptable, Serializable
