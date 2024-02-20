/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.embed

import java.io.Serializable
import java.util.SortedSet
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Created by aduchate on 02/07/13, 11:59
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "One or several periods of care by an hcp for this patient")
data class PatientHealthCarePartyDto(
	@Schema(description = "Type of care/relationship.") val type: PatientHealthCarePartyTypeDto? = null,
	@Schema(description = "UUID of the hcp.") val healthcarePartyId: String? = null,
	@Schema(description = "Preferred format of exchange for diverse means of communication") val sendFormats: Map<TelecomTypeDto, String> = emptyMap(), // String is in fact a UTI (uniform type identifier / a sort of super-MIME)
	@Schema(description = "Time periods") val referralPeriods: SortedSet<ReferralPeriodDto> = sortedSetOf(), // History of DMG ownerships
	@get:Deprecated("Use type") @Schema(defaultValue = "false") val referral: Boolean = false, // mark this phcp as THE active referral link (gmd)
	override val encryptedSelf: String? = null
) : EncryptedDto, Serializable
