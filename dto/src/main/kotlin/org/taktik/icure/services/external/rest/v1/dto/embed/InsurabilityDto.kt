/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.embed

/**
 * Created by aduchate on 21/01/13, 15:37
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "This class represents a coverage of a patient by an insurance during a period or time.")
data class InsurabilityDto(
	@Schema(description = "Insurance extra parameters.") val parameters: Map<String, String> = emptyMap(),
	@Schema(description = "Is hospitalization covered.") val hospitalisation: Boolean? = null,
	@Schema(description = "Is outpatient care covered.") val ambulatory: Boolean? = null,
	@Schema(description = "Is dental care covered.") val dental: Boolean? = null,
	@Schema(description = "Identification number of the patient at the insurance.") val identificationNumber: String? = null, // N° in form (number for the insurance's identification)
	@Schema(description = "Id of the Insurance.") val insuranceId: String? = null, // UUID to identify Partena, etc. (link to InsuranceDto object's document ID)
	@Schema(description = "Start date of the coverage (YYYYMMDD).") val startDate: Long? = null,
	@Schema(description = "End date of the coverage (YYYYMMDD).") val endDate: Long? = null,
	@Schema(
		description = "UUID of the contact person who is the policyholder of the insurance (when the patient is covered by the insurance of a third person).",
	) val titularyId: String? = null,
	override val encryptedSelf: String? = null,
) : EncryptableDto,
	Serializable
