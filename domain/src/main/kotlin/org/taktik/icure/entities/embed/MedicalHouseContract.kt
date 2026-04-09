/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

// NOTE: better classname would be MedicalHouseInscriptionPeriod
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class MedicalHouseContract(
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
	val changeType: ContractChangeType? = null, // inscription, coverageChange, suspension
	/** The identifier of the parent contract. */
	val parentContractId: String? = null,
	/** Who changed the contract (user or mcn). */
	val changedBy: String? = null, // user, mcn

	/** The contract signature date (yyyyMMdd). */
	// Coverage specific data (coverage = forfait-inscription)
	val startOfContract: Long? = null, // yyyyMMdd : signdate
	/** The start of coverage date (yyyyMMdd). */
	val startOfCoverage: Long? = null, // yyyyMMdd
	/** The contract end signature date (yyyyMMdd). */
	val endOfContract: Long? = null, // yyyyMMdd : signdate
	/** The end of coverage date (yyyyMMdd). */
	val endOfCoverage: Long? = null, // yyyyMMdd
	/** Whether kinesitherapy is covered. */
	val kine: Boolean = false,
	/** Whether general practitioner care is covered. */
	val gp: Boolean = false,
	/** Whether PTD is covered. */
	val ptd: Boolean = false,
	/** Whether nursing care is covered. */
	val nurse: Boolean = false,
	/** Whether kinesitherapy is excluded. */
	val noKine: Boolean = false,
	/** Whether general practitioner care is excluded. */
	val noGp: Boolean = false,
	/** Whether nursing care is excluded. */
	val noNurse: Boolean = false,
	/** The reason identifier for unsubscription. */
	val unsubscriptionReasonId: Int? = null,

	/** The start date of PTD coverage. */
	val ptdStart: Long? = null,
	/** The end date of PTD coverage. */
	val ptdEnd: Long? = null,
	/** The date PTD was last invoiced. */
	val ptdLastInvoiced: Long? = null,

	/** The start of suspension date (yyyyMMdd). */
	// Suspension specific data:
	val startOfSuspension: Long? = null, // yyyyMMdd
	/** The end of suspension date (yyyyMMdd). */
	val endOfSuspension: Long? = null, // yyyyMMdd
	/** The reason for suspension. */
	val suspensionReason: SuspensionReason? = null,
	/** The source of the suspension. */
	val suspensionSource: String? = null,
	/** Whether the suspension is forced (no automatic unsuspension). */
	val forcedSuspension: Boolean = false, // no automatic unSuspension = false

	/** The type of signature used for the contract. */
	val signatureType: MhcSignatureType? = null,
	/** The contract status. */
	val status: Int? = null,
	/** Additional options as key-value pairs. */
	val options: Map<String, String> = HashMap(),
	/** Receipt identifiers as key-value pairs. */
	val receipts: Map<String, String> = emptyMap(),

	/** The base64-encoded encrypted content. */
	override val encryptedSelf: String? = null,
) : Encryptable
