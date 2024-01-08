/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

//NOTE: better classname would be MedicalHouseInscriptionPeriod
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class MedicalHouseContract(
	val contractId: String? = null,
	val validFrom: Long? = null, //yyyyMMdd : start of contract period
	val validTo: Long? = null, //yyyyMMdd : end of contract period
	val mmNihii: String? = null,
	val hcpId: String? = null,
	val changeType: ContractChangeType? = null, //inscription, coverageChange, suspension
	val parentContractId: String? = null,
	val changedBy: String? = null, //user, mcn

	//Coverage specific data (coverage = forfait-inscription)
	val startOfContract: Long? = null, //yyyyMMdd : signdate
	val startOfCoverage: Long? = null, //yyyyMMdd
	val endOfContract: Long? = null, //yyyyMMdd : signdate
	val endOfCoverage: Long? = null, //yyyyMMdd
	val kine: Boolean = false,
	val gp: Boolean = false,
	val ptd: Boolean = false,
	val nurse: Boolean = false,
	val noKine: Boolean = false,
	val noGp: Boolean = false,
	val noNurse: Boolean = false,
	val unsubscriptionReasonId: Int? = null,

	val ptdStart: Long? = null,
	val ptdEnd: Long? = null,
	val ptdLastInvoiced: Long? = null,

	//Suspension specific data:
	val startOfSuspension: Long? = null, //yyyyMMdd
	val endOfSuspension: Long? = null, //yyyyMMdd
	val suspensionReason: SuspensionReason? = null,
	val suspensionSource: String? = null,
	val forcedSuspension: Boolean = false, //no automatic unSuspension = false

	val signatureType: MhcSignatureType? = null,
	val status: Int? = null,
	val options: Map<String, String> = HashMap(),
	val receipts: Map<String, String> = emptyMap(),

	override val encryptedSelf: String? = null
) : Encrypted
