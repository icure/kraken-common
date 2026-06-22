/*
 *  iCure Data Stack. Copyright (c) 2020 Taktik SA
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public
 *     License along with this program.  If not, see
 *     <https://www.gnu.org/licenses/>.
 */
package org.taktik.icure.services.external.rest.v2.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto
import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents information about a financial institution, including bank account details and proxy account configuration.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.embed.FinancialInstitutionInformationDto")
data class FinancialInstitutionInformationDto(
	/** The name of the financial institution. */
	@ActiveField val name: String? = null,
	/** The key identifying this financial institution information entry. */
	@ActiveField val key: String? = null,
	/** The bank account number (e.g., IBAN). */
	@ActiveField val bankAccount: String? = null,
	/** The BIC/SWIFT code of the bank. */
	@ActiveField val bic: String? = null,
	/** The proxy bank account number. */
	@ActiveField val proxyBankAccount: String? = null,
	/** The BIC/SWIFT code for the proxy bank. */
	@ActiveField val proxyBic: String? = null,
	/** Set of insurance or healthcare party identifiers that prefer this financial institution. */
	@ActiveField val preferredFiiForPartners: Set<String> = emptySet(), // InsuranceDto Id, Hcp Id
	/** The base64-encoded encrypted content. */
	override val encryptedSelf: Base64StringDto? = null,
) : EncryptableDto,
	Serializable
