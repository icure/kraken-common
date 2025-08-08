/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.Message
import org.taktik.icure.services.external.rest.v1.dto.MessageDto
import org.taktik.icure.services.external.rest.v1.mapper.base.CodeStubMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.DelegationMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.MessageAttachmentMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.MessageReadStatusMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.SecurityMetadataMapper

@Mapper(componentModel = "spring", uses = [CodeStubMapper::class, DelegationMapper::class, MessageReadStatusMapper::class, SecurityMetadataMapper::class, MessageAttachmentMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface MessageMapper {
	@Mappings(
		Mapping(target = "attachments", ignore = true),
		Mapping(target = "revHistory", ignore = true),
		Mapping(target = "conflicts", ignore = true),
		Mapping(target = "revisionsInfo", ignore = true),
	)
	fun map(messageDto: MessageDto): Message
	fun map(message: Message): MessageDto
}
