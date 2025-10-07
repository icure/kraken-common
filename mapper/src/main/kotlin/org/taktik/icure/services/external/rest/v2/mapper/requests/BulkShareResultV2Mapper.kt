package org.taktik.icure.services.external.rest.v2.mapper.requests

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.base.HasSecureDelegationsAccessControl
import org.taktik.icure.entities.requests.EntityBulkShareResult
import org.taktik.icure.services.external.rest.v2.dto.requests.EntityBulkShareResultDto

@Mapper(
	componentModel = "spring",
	uses = [
		RejectedShareRequestV2Mapper::class,
	],
	injectionStrategy = InjectionStrategy.CONSTRUCTOR,
)
interface BulkShareResultV2Mapper {
	@Mappings(
		Mapping(target = "updatedEntity", expression = "lambda(mapEntity)"),
	)
	fun <DTO : Any, OBJ : HasSecureDelegationsAccessControl> map(
		bulkShareResult: EntityBulkShareResult<OBJ>,
		mapEntity: (OBJ?) -> DTO?
	): EntityBulkShareResultDto<DTO>


	@Mappings(
		Mapping(target = "updatedEntity", expression = "kotlin(null)"),
	)
	fun mapMinimal(
		bulkShareResult: EntityBulkShareResult<*>,
	): EntityBulkShareResultDto<Nothing>
}