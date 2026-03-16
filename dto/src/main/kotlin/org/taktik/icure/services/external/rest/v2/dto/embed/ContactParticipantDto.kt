package org.taktik.icure.services.external.rest.v2.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v2.dto.base.ParticipantTypeDto

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a healthcare party participating in a contact, along with their participation type.
 */
data class ContactParticipantDto(
	/** The type of participation in the contact. */
	val type: ParticipantTypeDto,
	/** The identifier of the participating healthcare party. */
	val hcpId: String,
)