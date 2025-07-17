package org.taktik.icure.services.external.rest.v2.mapper.requests

import org.mapstruct.Mapping
import org.mapstruct.Named
import org.springframework.stereotype.Service
import org.taktik.icure.entities.MaintenanceTask
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.services.external.rest.v2.dto.MaintenanceTaskDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.MaintenanceTaskV2Mapper

//TODO tmp no support yet for generics

interface MaintenanceTaskBulkShareResultV2Mapper {

    @Mapping(source = "updatedEntity", target = "updatedEntity", qualifiedByName = ["maintenanceTaskToDto"])
    fun map(bulkShareResultDto: EntityBulkShareResultDto<MaintenanceTaskDto>): EntityBulkShareResult<MaintenanceTask>
    @Mapping(source = "updatedEntity", target = "updatedEntity", qualifiedByName = ["dtoToMaintenanceTask"])
    fun map(bulkShareResult: EntityBulkShareResult<MaintenanceTask>): EntityBulkShareResultDto<MaintenanceTaskDto>

    @Named("maintenanceTaskToDto")
    fun maintenanceTaskToDto(maintenanceTask: MaintenanceTask?): MaintenanceTaskDto?

    @Named("dtoToMaintenanceTask")
    fun dtoToMaintenanceTask(maintenanceTaskDto: MaintenanceTaskDto?): MaintenanceTask?
}

@Service
class MaintenanceTaskBulkShareResultV2MapperImpl(
    private val rejectedShareRequestV2Mapper: RejectedShareRequestV2Mapper,
    private val maintenanceTaskMapper: MaintenanceTaskV2Mapper
) : MaintenanceTaskBulkShareResultV2Mapper {
    override fun map(bulkShareResultDto: EntityBulkShareResultDto<MaintenanceTaskDto>):
            EntityBulkShareResult<MaintenanceTask> = EntityBulkShareResult(
        updatedEntity = bulkShareResultDto.updatedEntity?.let { maintenanceTaskMapper.map(it) },
        entityId = bulkShareResultDto.entityId,
        entityRev = bulkShareResultDto.entityRev,
        rejectedRequests = bulkShareResultDto.rejectedRequests.map { (k, v) ->
            k to this.rejectedShareRequestV2Mapper.map(v)
        }.toMap(),
    )

    override fun map(bulkShareResult: EntityBulkShareResult<MaintenanceTask>):
            EntityBulkShareResultDto<MaintenanceTaskDto> = EntityBulkShareResultDto(
        updatedEntity =
        bulkShareResult.updatedEntity?.let { maintenanceTaskMapper.map(it) },
        entityId = bulkShareResult.entityId,
        entityRev = bulkShareResult.entityRev,
        rejectedRequests = bulkShareResult.rejectedRequests.map { (k, v) ->
            k to this.rejectedShareRequestV2Mapper.map(v)
        }.toMap(),
    )

    override fun maintenanceTaskToDto(maintenanceTask: MaintenanceTask?): MaintenanceTaskDto? = maintenanceTask?.let { maintenanceTaskMapper.map(it) }
    override fun dtoToMaintenanceTask(maintenanceTaskDto: MaintenanceTaskDto?): MaintenanceTask? = maintenanceTaskDto?.let { maintenanceTaskMapper.map(it) }
}

