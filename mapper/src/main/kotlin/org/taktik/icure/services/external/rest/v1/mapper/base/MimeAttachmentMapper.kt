package org.taktik.icure.services.external.rest.v1.mapper.base

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.domain.result.MimeAttachment
import org.taktik.icure.services.external.rest.v1.dto.base.MimeAttachmentDto

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface MimeAttachmentMapper {
	fun map(mimeAttachmentDto: MimeAttachmentDto): MimeAttachment
	fun map(mimeAttachment: MimeAttachment): MimeAttachmentDto
}
