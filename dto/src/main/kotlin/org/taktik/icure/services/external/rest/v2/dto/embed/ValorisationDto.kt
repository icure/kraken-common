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
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.services.external.rest.v2.dto.specializations.Base64StringDto
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents the financial valorisation of a flat rate tarification, including validity period and amount breakdown.
 */
data class ValorisationDto(
	/** The start of the validity period (yyyyMMdd). */
	val startOfValidity: Long? = null, // yyyyMMdd
	/** The end of the validity period (yyyyMMdd). */
	val endOfValidity: Long? = null, // yyyyMMdd
	/** A predicate expression for conditional valorisation. */
	val predicate: String? = null,
	/** A list of reference integers. */
	val reference: List<Int>? = null,
	/** The total amount (reimbursement + doctor supplement + intervention). */
	val totalAmount: Double? = null, // =reimbursement+doctorSupplement+intervention
	/** The reimbursement amount. */
	val reimbursement: Double? = null,
	/** The patient intervention amount. */
	val patientIntervention: Double? = null,
	/** The doctor supplement amount. */
	val doctorSupplement: Double? = null,
	/** The VAT amount. */
	val vat: Double? = null,
	/** Localized labels for this valorisation, keyed by language code. */
	@param:Schema(defaultValue = "emptyMap()") val label: Map<String, String>? = emptyMap(), // ex: {en: Rheumatic Aortic Stenosis, fr: Sténose rhumatoïde de l'Aorte}
	/** The base64-encoded encrypted content. */
	override val encryptedSelf: Base64StringDto? = null,
) : EncryptableDto,
	Serializable
