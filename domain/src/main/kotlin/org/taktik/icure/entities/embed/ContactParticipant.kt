package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.base.ParticipantType

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ContactParticipant(
	/** The type of participation in the contact. */
	val type: ParticipantType,
	/** The identifier of the participating healthcare party. */
	val hcpId: String,
)
