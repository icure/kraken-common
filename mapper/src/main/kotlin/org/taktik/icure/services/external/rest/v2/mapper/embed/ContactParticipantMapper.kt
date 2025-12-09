package org.taktik.icure.services.external.rest.v2.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.embed.ContactParticipant
import org.taktik.icure.services.external.rest.v2.dto.embed.ContactParticipantDto

@Mapper(componentModel = "spring", uses = [], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface ContactParticipantV2Mapper {
	fun map(participantDto: ContactParticipantDto): ContactParticipant

	fun map(participant: ContactParticipant): ContactParticipantDto
}