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

import com.fasterxml.jackson.annotation.JsonFilter
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.dto.annotations.filtering.ActiveField
import org.taktik.icure.dto.annotations.filtering.FilterBeforeSdkVersion
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.embed.MedicationDto")
data class MedicationDto(
	@ActiveField val compoundPrescription: String? = null,
	@ActiveField val substanceProduct: SubstanceproductDto? = null,
	@ActiveField val medicinalProduct: MedicinalproductDto? = null,
	@ActiveField val numberOfPackages: Int? = null,
	@ActiveField val batch: String? = null,
	/**
	 * The expiration date of the medication. Format: yyyyMMdd
	 */
	@ActiveField val expirationDate: Long? = null,
	@ActiveField val instructionForPatient: String? = null,
	@ActiveField val instructionForReimbursement: String? = null,
	@ActiveField val commentForDelivery: String? = null,
	@ActiveField val drugRoute: String? = null, // CD-DRUG-ROUTE
	@ActiveField val temporality: String? = null, // CD-TEMPORALITY : chronic, acute, oneshot
	@ActiveField val frequency: CodeStubDto? = null, // CD-PERIODICITY
	@ActiveField val reimbursementReason: CodeStubDto? = null,
	@ActiveField val substitutionAllowed: Boolean? = null,
	@ActiveField val beginMoment: Long? = null,
	@ActiveField val endMoment: Long? = null,
	@ActiveField val deliveryMoment: Long? = null,
	@ActiveField val endExecutionMoment: Long? = null,
	@ActiveField val duration: DurationDto? = null,
	@ActiveField val renewal: RenewalDto? = null,
	@ActiveField val knownUsage: Boolean? = null,
	@ActiveField val regimen: List<RegimenItemDto>? = null,
	@ActiveField val posology: String? = null, // replace structured posology by text
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@FilterBeforeSdkVersion("2.6.0")
	val options: Map<String, ContentDto>? = null,
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@ActiveField val agreements: Map<String, ParagraphAgreementDto>? = null,
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@ActiveField val medicationSchemeIdOnSafe: String? = null,
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@ActiveField val medicationSchemeSafeVersion: Int? = null,
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@ActiveField val medicationSchemeTimeStampOnSafe: Long? = null,
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@ActiveField val medicationSchemeDocumentId: String? = null,
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@ActiveField val safeIdName: String? = null, // can be: vitalinkuri, RSWID, RSBID
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@ActiveField val idOnSafes: String? = null, // medicationschemeelement : value of vitalinkuri, RSBID, RSWID
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@ActiveField val timestampOnSafe: Long? = null, // transaction date+time
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@ActiveField val changeValidated: Boolean? = null, // accept change on safe
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@ActiveField val newSafeMedication: Boolean? = null, // new medication on safe
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@ActiveField val medicationUse: String? = null, // free text
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@ActiveField val beginCondition: String? = null, // free text
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@ActiveField val endCondition: String? = null, // free text
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@ActiveField val origin: String? = null, // regularprocess, recorded
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@ActiveField val medicationChanged: Boolean? = null,
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@ActiveField val posologyChanged: Boolean? = null,
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@ActiveField val suspension: List<SuspensionDto>? = null,
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@ActiveField val prescriptionRID: String? = null,
	@Deprecated("This field is deprecated for the use with Cardinal SDK")
	@ActiveField val status: Int? = null,
	@ActiveField val stockLocation: AddressDto? = null,
) : Serializable {
	companion object {
		const val REIMBURSED = "REIMBURSED"
		const val STATUS_NOT_SENT = 1 shl 0 // not send by recip-e
		const val STATUS_SENT = 1 shl 1 // sent by recip-e
		const val STATUS_PENDING = 1 shl 2 // not delivered to patient
		const val STATUS_DELIVERED = 1 shl 3 // delivered to patient
		const val STATUS_REVOKED = 1 shl 4 // revoked by physician
	}
}
