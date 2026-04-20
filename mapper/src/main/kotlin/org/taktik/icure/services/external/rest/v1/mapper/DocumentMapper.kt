/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.Document
import org.taktik.icure.services.external.rest.v1.dto.DocumentDto
import org.taktik.icure.services.external.rest.v1.mapper.base.CodeStubMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.DataAttachmentMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.DelegationMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.DeletedAttachmentMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.SecurityMetadataMapper

@Mapper(
	componentModel = "spring",
	uses = [
		CodeStubMapper::class,
		DelegationMapper::class,
		DataAttachmentMapper::class,
		DeletedAttachmentMapper::class,
		SecurityMetadataMapper::class,
	],
	injectionStrategy = InjectionStrategy.CONSTRUCTOR,
)
interface DocumentMapper {
	@Mappings(
		Mapping(target = "attachments", ignore = true),
		Mapping(target = "revHistory", ignore = true),
		Mapping(target = "conflicts", ignore = true),
		Mapping(target = "revisionsInfo", ignore = true),
		Mapping(target = "extraMainAttachmentInfo", ignore = true),
		Mapping(target = "mainAttachmentStoredDataSize", ignore = true),
		Mapping(target = "extensions", ignore = true),
		Mapping(target = "extensionsVersion", ignore = true),
	)
	fun map(documentDto: DocumentDto): Document

	fun map(document: Document): DocumentDto {
		require(document.extensions == null) { "Document has extensions and can't be used with v1 endpoints" }
		return doMap(document)
	}

	@Mappings(
		Mapping(target = "encryptedAttachment", ignore = true),
		Mapping(target = "decryptedAttachment", ignore = true),
	)
	fun doMap(document: Document): DocumentDto
}
