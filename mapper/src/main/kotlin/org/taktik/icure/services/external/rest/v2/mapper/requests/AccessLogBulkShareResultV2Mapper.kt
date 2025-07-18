package org.taktik.icure.services.external.rest.v2.mapper.requests

import org.mapstruct.Mapping
import org.mapstruct.Named
import org.springframework.stereotype.Service
import org.taktik.icure.entities.AccessLog
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.services.external.rest.v2.dto.AccessLogDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.AccessLogV2Mapper

// TODO tmp no support yet for generics

interface AccessLogBulkShareResultV2Mapper {

	@Mapping(source = "updatedEntity", target = "updatedEntity", qualifiedByName = ["accessLogToDto"])
	fun map(bulkShareResultDto: EntityBulkShareResultDto<AccessLogDto>): EntityBulkShareResult<AccessLog>

	@Mapping(source = "updatedEntity", target = "updatedEntity", qualifiedByName = ["dtoToAccessLog"])
	fun map(bulkShareResult: EntityBulkShareResult<AccessLog>): EntityBulkShareResultDto<AccessLogDto>

	@Named("accessLogToDto")
	fun accessLogToDto(accessLog: AccessLog?): AccessLogDto?

	@Named("dtoToAccessLog")
	fun dtoToAccessLog(accessLogDto: AccessLogDto?): AccessLog?
}

@Service
class AccessLogBulkShareResultV2MapperImpl(
	private val rejectedShareRequestV2Mapper: RejectedShareRequestV2Mapper,
	private val accessLogMapper: AccessLogV2Mapper,
) : AccessLogBulkShareResultV2Mapper {
	override fun map(bulkShareResultDto: EntityBulkShareResultDto<AccessLogDto>): EntityBulkShareResult<AccessLog> = EntityBulkShareResult(
		updatedEntity = bulkShareResultDto.updatedEntity?.let { accessLogMapper.map(it) },
		entityId = bulkShareResultDto.entityId,
		entityRev = bulkShareResultDto.entityRev,
		rejectedRequests = bulkShareResultDto.rejectedRequests.map { (k, v) ->
			k to this.rejectedShareRequestV2Mapper.map(v)
		}.toMap(),
	)

	override fun map(bulkShareResult: EntityBulkShareResult<AccessLog>): EntityBulkShareResultDto<AccessLogDto> = EntityBulkShareResultDto(
		updatedEntity =
		bulkShareResult.updatedEntity?.let { accessLogMapper.map(it) },
		entityId = bulkShareResult.entityId,
		entityRev = bulkShareResult.entityRev,
		rejectedRequests = bulkShareResult.rejectedRequests.map { (k, v) ->
			k to this.rejectedShareRequestV2Mapper.map(v)
		}.toMap(),
	)

	override fun accessLogToDto(accessLog: AccessLog?) = accessLog?.let { accessLogMapper.map(it) }

	override fun dtoToAccessLog(accessLogDto: AccessLogDto?) = accessLogDto?.let { accessLogMapper.map(it) }
}
