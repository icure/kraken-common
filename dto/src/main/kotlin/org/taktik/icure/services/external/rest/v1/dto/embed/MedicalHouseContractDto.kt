/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.dto.embed

// NOTE: better classname would be MedicalHouseInscriptionPeriod
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class MedicalHouseContractDto(
	val contractId: String? = null,
	val validFrom: Long? = null, // yyyyMMdd : start of contract period
	val validTo: Long? = null, // yyyyMMdd : end of contract period
	val mmNihii: String? = null,
	val hcpId: String? = null,
	val changeType: ContractChangeTypeDto? = null, // inscription, coverageChange, suspension
	val parentContractId: String? = null,
	val changedBy: String? = null, // user, mcn
	// Coverage specific data (coverage = forfait-inscription)
	val startOfContract: Long? = null, // yyyyMMdd : signdate
	val startOfCoverage: Long? = null, // yyyyMMdd
	val endOfContract: Long? = null, // yyyyMMdd : signdate
	val endOfCoverage: Long? = null, // yyyyMMdd
	@param:Schema(defaultValue = "false") val kine: Boolean = false,
	@param:Schema(defaultValue = "false") val gp: Boolean = false,
	@param:Schema(defaultValue = "false") val ptd: Boolean = false,
	@param:Schema(defaultValue = "false") val nurse: Boolean = false,
	@param:Schema(defaultValue = "false") val noKine: Boolean = false,
	@param:Schema(defaultValue = "false") val noGp: Boolean = false,
	@param:Schema(defaultValue = "false") val noNurse: Boolean = false,
	val unsubscriptionReasonId: Int? = null,
	val ptdStart: Long? = null,
	val ptdEnd: Long? = null,
	val ptdLastInvoiced: Long? = null,
	// SuspensionDto specific data:
	val startOfSuspension: Long? = null, // yyyyMMdd
	val endOfSuspension: Long? = null, // yyyyMMdd
	val suspensionReason: SuspensionReasonDto? = null,
	val suspensionSource: String? = null,
	@param:Schema(defaultValue = "false") val forcedSuspension: Boolean = false, // no automatic unSuspension = false
	val signatureType: MhcSignatureTypeDto? = null,
	val status: Int? = null,
	val options: Map<String, String> = HashMap(),
	val receipts: Map<String, String> = emptyMap(),
	override val encryptedSelf: String? = null,
) : EncryptableDto
