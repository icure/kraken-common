package org.taktik.icure.services.external.rest.v2.mapper.requests

import org.mapstruct.Mapping
import org.mapstruct.Named
import org.springframework.stereotype.Service
import org.taktik.icure.entities.Message
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.services.external.rest.v2.dto.MessageDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.MessageV2Mapper

// TODO tmp no support yet for generics

interface MessageBulkShareResultV2Mapper {
	@Mapping(source = "updatedEntity", target = "updatedEntity", qualifiedByName = ["dtoToMessage"])
	fun map(bulkShareResult: EntityBulkShareResult<Message>): EntityBulkShareResultDto<MessageDto>

	@Named("messageToDto")
	fun messageToDto(message: Message?): MessageDto?
}

@Service
class MessageBulkShareResultV2MapperImpl(
	private val rejectedShareRequestV2Mapper: RejectedShareRequestV2Mapper,
	private val messageMapper: MessageV2Mapper,
) : MessageBulkShareResultV2Mapper {
	override fun map(bulkShareResult: EntityBulkShareResult<Message>): EntityBulkShareResultDto<MessageDto> = EntityBulkShareResultDto(
		updatedEntity =
		bulkShareResult.updatedEntity?.let { messageMapper.map(it) },
		entityId = bulkShareResult.entityId,
		entityRev = bulkShareResult.entityRev,
		rejectedRequests = bulkShareResult.rejectedRequests.map { (k, v) ->
			k to this.rejectedShareRequestV2Mapper.map(v)
		}.toMap(),
	)

	override fun messageToDto(message: Message?): MessageDto? = message?.let { messageMapper.map(it) }
}
