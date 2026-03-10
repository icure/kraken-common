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

// NOTE: better classname would be MedicalHouseInscriptionPeriod
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a medical house contract (inscription period) for a patient, including coverage details,
 * suspension periods, and signature information.
 */
data class MedicalHouseContractDto(
	/** The identifier of the contract. */
	val contractId: String? = null,
	/** The start of the contract period (yyyyMMdd). */
	val validFrom: Long? = null, // yyyyMMdd : start of contract period
	/** The end of the contract period (yyyyMMdd). */
	val validTo: Long? = null, // yyyyMMdd : end of contract period
	/** The NIHII number of the medical house. */
	val mmNihii: String? = null,
	/** The identifier of the healthcare party. */
	val hcpId: String? = null,
	/** The type of contract change (inscription, coverageChange, suspension). */
	val changeType: ContractChangeTypeDto? = null, // inscription, coverageChange, suspension
	/** The identifier of the parent contract. */
	val parentContractId: String? = null,
	/** Who changed the contract (user or mcn). */
	val changedBy: String? = null, // user, mcn
	// Coverage specific data (coverage = forfait-inscription)
	/** The contract signature date (yyyyMMdd). */
	val startOfContract: Long? = null, // yyyyMMdd : signdate
	/** The start of coverage date (yyyyMMdd). */
	val startOfCoverage: Long? = null, // yyyyMMdd
	/** The contract end signature date (yyyyMMdd). */
	val endOfContract: Long? = null, // yyyyMMdd : signdate
	/** The end of coverage date (yyyyMMdd). */
	val endOfCoverage: Long? = null, // yyyyMMdd
	/** Whether kinesitherapy is covered. */
	@param:Schema(defaultValue = "false") val kine: Boolean = false,
	/** Whether general practitioner care is covered. */
	@param:Schema(defaultValue = "false") val gp: Boolean = false,
	/** Whether PTD is covered. */
	@param:Schema(defaultValue = "false") val ptd: Boolean = false,
	/** Whether nursing care is covered. */
	@param:Schema(defaultValue = "false") val nurse: Boolean = false,
	/** Whether kinesitherapy is excluded. */
	@param:Schema(defaultValue = "false") val noKine: Boolean = false,
	/** Whether general practitioner care is excluded. */
	@param:Schema(defaultValue = "false") val noGp: Boolean = false,
	/** Whether nursing care is excluded. */
	@param:Schema(defaultValue = "false") val noNurse: Boolean = false,
	/** The reason identifier for unsubscription. */
	val unsubscriptionReasonId: Int? = null,
	/** The start date of PTD coverage. */
	val ptdStart: Long? = null,
	/** The end date of PTD coverage. */
	val ptdEnd: Long? = null,
	/** The date PTD was last invoiced. */
	val ptdLastInvoiced: Long? = null,
	// SuspensionDto specific data:
	/** The start of suspension date (yyyyMMdd). */
	val startOfSuspension: Long? = null, // yyyyMMdd
	/** The end of suspension date (yyyyMMdd). */
	val endOfSuspension: Long? = null, // yyyyMMdd
	/** The reason for suspension. */
	val suspensionReason: SuspensionReasonDto? = null,
	/** The source of the suspension. */
	val suspensionSource: String? = null,
	/** Whether the suspension is forced (no automatic unsuspension). */
	@param:Schema(defaultValue = "false") val forcedSuspension: Boolean = false, // no automatic unSuspension = false
	/** The type of signature used for the contract. */
	val signatureType: MhcSignatureTypeDto? = null,
	/** The contract status. */
	val status: Int? = null,
	/** Additional options as key-value pairs. */
	val options: Map<String, String> = HashMap(),
	/** Receipt identifiers as key-value pairs. */
	val receipts: Map<String, String> = emptyMap(),
	/** The base64-encoded encrypted content. */
	override val encryptedSelf: Base64StringDto? = null,
) : EncryptableDto
