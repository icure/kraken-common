package org.taktik.icure.services.external.rest.v2.mapper.requests

import org.mapstruct.Mapping
import org.mapstruct.Named
import org.springframework.stereotype.Service
import org.taktik.icure.entities.Contact
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.services.external.rest.v2.dto.ContactDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.ContactV2Mapper

// TODO tmp no support yet for generics

// @Mapper(
//    componentModel = "spring",
//    uses = [RejectedShareRequestV2Mapper::class],
//    injectionStrategy = InjectionStrategy.CONSTRUCTOR
// )
interface ContactBulkShareResultV2Mapper {
	@Mapping(source = "updatedEntity", target = "updatedEntity", qualifiedByName = ["contactToDto"])
	suspend fun map(bulkShareResultDto: EntityBulkShareResultDto<ContactDto>): EntityBulkShareResult<Contact>

	@Mapping(source = "updatedEntity", target = "updatedEntity", qualifiedByName = ["dtoToContact"])
	suspend fun map(bulkShareResult: EntityBulkShareResult<Contact>): EntityBulkShareResultDto<ContactDto>

	@Named("contactToDto")
	suspend fun contactToDto(contact: Contact?): ContactDto?

	@Named("dtoToContact")
	suspend fun dtoToContact(contactDto: ContactDto?): Contact?
}

@Service
class ContactBulkShareResultV2MapperImpl(
	private val rejectedShareRequestV2Mapper: RejectedShareRequestV2Mapper,
	private val contactMapper: ContactV2Mapper,
) : ContactBulkShareResultV2Mapper {
	override suspend fun map(bulkShareResultDto: EntityBulkShareResultDto<ContactDto>): EntityBulkShareResult<Contact> = EntityBulkShareResult(
		updatedEntity = bulkShareResultDto.updatedEntity?.let { contactMapper.map(it) },
		entityId = bulkShareResultDto.entityId,
		entityRev = bulkShareResultDto.entityRev,
		rejectedRequests = bulkShareResultDto.rejectedRequests.map { (k, v) ->
			k to this.rejectedShareRequestV2Mapper.map(v)
		}.toMap(),
	)

	override suspend fun map(bulkShareResult: EntityBulkShareResult<Contact>): EntityBulkShareResultDto<ContactDto> = EntityBulkShareResultDto(
		updatedEntity =
		bulkShareResult.updatedEntity?.let { contactMapper.map(it) },
		entityId = bulkShareResult.entityId,
		entityRev = bulkShareResult.entityRev,
		rejectedRequests = bulkShareResult.rejectedRequests.map { (k, v) ->
			k to this.rejectedShareRequestV2Mapper.map(v)
		}.toMap(),
	)

	override suspend fun contactToDto(contact: Contact?): ContactDto? = contact?.let { contactMapper.map(it) }
	override suspend fun dtoToContact(contactDto: ContactDto?): Contact? = contactDto?.let { contactMapper.map(it) }
}
