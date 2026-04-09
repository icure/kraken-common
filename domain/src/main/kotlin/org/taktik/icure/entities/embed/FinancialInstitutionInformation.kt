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
	/** The name of the financial institution. */
	val name: String? = null,
	/** The key identifying this financial institution information entry. */
	val key: String? = null,
	/** The bank account number (e.g., IBAN). */
	val bankAccount: String? = null,
	/** The BIC/SWIFT code of the bank. */
	val bic: String? = null,
	/** The proxy bank account number. */
	val proxyBankAccount: String? = null,
	/** The BIC/SWIFT code for the proxy bank. */
	val proxyBic: String? = null,
	/** Set of insurance or healthcare party identifiers that prefer this financial institution. */
	val preferredFiiForPartners: Set<String> = emptySet(), // Insurance Id, Hcp Id
	/** The base64-encoded encrypted content. */
	override val encryptedSelf: String? = null,
) : Encryptable,
	Serializable
