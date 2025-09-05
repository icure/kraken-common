package org.taktik.icure.services.external.rest.v2.mapper.requests

import org.mapstruct.Mapping
import org.mapstruct.Named
import org.springframework.stereotype.Service
import org.taktik.icure.entities.ApplicationSettings
import org.taktik.icure.entities.Classification
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.services.external.rest.v2.dto.ApplicationSettingsDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.ApplicationSettingsV2Mapper

// TODO tmp no support yet for generics

interface ApplicationSettingsBulkShareResultV2Mapper {
	@Mapping(source = "updatedEntity", target = "updatedEntity", qualifiedByName = ["applicationSettingsToDto"])
	fun map(bulkShareResultDto: EntityBulkShareResultDto<ApplicationSettingsDto>): EntityBulkShareResult<ApplicationSettings>

	@Mapping(source = "updatedEntity", target = "updatedEntity", qualifiedByName = ["dtoToApplicationSettings"])
	fun map(bulkShareResult: EntityBulkShareResult<ApplicationSettings>): EntityBulkShareResultDto<ApplicationSettingsDto>

	@Named("applicationSettingsToDto")
	fun applicationSettingsToDto(applicationSettings: ApplicationSettings?): ApplicationSettingsDto?

	@Named("dtoToApplicationSettings")
	fun dtoToApplicationSettings(applicationSettingsDto: ApplicationSettingsDto?): ApplicationSettings?
}

@Service
class ApplicationSettingsBulkShareResultV2MapperImpl(
	private val rejectedShareRequestV2Mapper: RejectedShareRequestV2Mapper,
	private val applicationSettingsMapper: ApplicationSettingsV2Mapper,
) : ApplicationSettingsBulkShareResultV2Mapper {
	override fun map(bulkShareResultDto: EntityBulkShareResultDto<ApplicationSettingsDto>): EntityBulkShareResult<ApplicationSettings> = EntityBulkShareResult(
		updatedEntity = bulkShareResultDto.updatedEntity?.let { applicationSettingsMapper.map(it) },
		entityId = bulkShareResultDto.entityId,
		entityRev = bulkShareResultDto.entityRev,
		rejectedRequests = bulkShareResultDto.rejectedRequests.map { (k, v) ->
			k to this.rejectedShareRequestV2Mapper.map(v)
		}.toMap(),
	)

	override fun map(bulkShareResult: EntityBulkShareResult<ApplicationSettings>): EntityBulkShareResultDto<ApplicationSettingsDto> = EntityBulkShareResultDto(
		updatedEntity =
		bulkShareResult.updatedEntity?.let { applicationSettingsMapper.map(it) },
		entityId = bulkShareResult.entityId,
		entityRev = bulkShareResult.entityRev,
		rejectedRequests = bulkShareResult.rejectedRequests.map { (k, v) ->
			k to this.rejectedShareRequestV2Mapper.map(v)
		}.toMap(),
	)

	override fun applicationSettingsToDto(applicationSettings: ApplicationSettings?): ApplicationSettingsDto? = applicationSettings?.let { applicationSettingsMapper.map(it) }

	override fun dtoToApplicationSettings(applicationSettingsDto: ApplicationSettingsDto?): ApplicationSettings? = applicationSettingsDto?.let { applicationSettingsMapper.map(it) }
}
