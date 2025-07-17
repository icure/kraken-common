package org.taktik.icure.services.external.rest.v2.mapper.requests

import org.mapstruct.Mapping
import org.mapstruct.Named
import org.springframework.stereotype.Service
import org.taktik.icure.entities.SecureDelegationKeyMap
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.services.external.rest.v2.dto.SecureDelegationKeyMapDto
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto
import org.taktik.icure.services.external.rest.v2.mapper.SecureDelegationKeyMapV2Mapper

//TODO tmp no support yet for generics

//@Mapper(
//    componentModel = "spring",
//    uses = [RejectedShareRequestV2Mapper::class],
//    injectionStrategy = InjectionStrategy.CONSTRUCTOR
//)
interface SecureDelegationKeyMapBulkShareResultV2Mapper {

    @Mapping(source = "updatedEntity", target = "updatedEntity", qualifiedByName = ["dtoToMap"])
    fun map(bulkShareResultDto: EntityBulkShareResultDto<SecureDelegationKeyMapDto>): EntityBulkShareResult<SecureDelegationKeyMap>
    @Mapping(source = "updatedEntity", target = "updatedEntity", qualifiedByName = ["mapToDto"])
    fun map(bulkShareResult: EntityBulkShareResult<SecureDelegationKeyMap>): EntityBulkShareResultDto<SecureDelegationKeyMapDto>

    @Named("mapToDto")
    fun patientToDto(map: SecureDelegationKeyMap?): SecureDelegationKeyMapDto?

    @Named("dtoToMap")
    fun dtoToPatient(mapDto: SecureDelegationKeyMapDto?): SecureDelegationKeyMap?
}

@Service
class SecureDelegationKeyMapBulkShareResultV2MapperImpl(
    private val rejectedShareRequestV2Mapper: RejectedShareRequestV2Mapper,
    private val keyMapV2Mapper: SecureDelegationKeyMapV2Mapper
) : SecureDelegationKeyMapBulkShareResultV2Mapper {
    override fun map(bulkShareResultDto: EntityBulkShareResultDto<SecureDelegationKeyMapDto>):
            EntityBulkShareResult<SecureDelegationKeyMap> = EntityBulkShareResult(
        updatedEntity = bulkShareResultDto.updatedEntity?.let { keyMapV2Mapper.map(it) },
        entityId = bulkShareResultDto.entityId,
        entityRev = bulkShareResultDto.entityRev,
        rejectedRequests = bulkShareResultDto.rejectedRequests.map { (k, v) ->
            k to this.rejectedShareRequestV2Mapper.map(v)
        }.toMap(),
    )

    override fun map(bulkShareResult: EntityBulkShareResult<SecureDelegationKeyMap>):
            EntityBulkShareResultDto<SecureDelegationKeyMapDto> = EntityBulkShareResultDto(
        updatedEntity =
        bulkShareResult.updatedEntity?.let { keyMapV2Mapper.map(it) },
        entityId = bulkShareResult.entityId,
        entityRev = bulkShareResult.entityRev,
        rejectedRequests = bulkShareResult.rejectedRequests.map { (k, v) ->
            k to this.rejectedShareRequestV2Mapper.map(v)
        }.toMap(),
    )

    override fun patientToDto(map: SecureDelegationKeyMap?): SecureDelegationKeyMapDto? = map?.let { keyMapV2Mapper.map(it) }
    override fun dtoToPatient(mapDto: SecureDelegationKeyMapDto?): SecureDelegationKeyMap? = mapDto?.let { keyMapV2Mapper.map(it) }
}

