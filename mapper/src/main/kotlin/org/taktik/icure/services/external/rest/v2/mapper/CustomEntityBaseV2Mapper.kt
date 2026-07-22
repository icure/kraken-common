package org.taktik.icure.services.external.rest.v2.mapper

import com.icure.cardinal.customentities.mapping.MapperExtensionsValidationContext
import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.CustomEntityBase
import org.taktik.icure.services.external.rest.v2.dto.CustomEntityBaseDto
import org.taktik.icure.services.external.rest.v2.mapper.base.CodeStubV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.DataAttachmentV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.DelegationV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.DeletedAttachmentV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.SecurityMetadataV2Mapper


@Mapper(
	componentModel = "spring",
	uses = [
		CodeStubV2Mapper::class,
		DelegationV2Mapper::class,
		SecurityMetadataV2Mapper::class,
		DataAttachmentV2Mapper::class,
		DeletedAttachmentV2Mapper::class,
	],
	injectionStrategy = InjectionStrategy.CONSTRUCTOR,
)
abstract class CustomEntityBaseV2Mapper {
	@Mappings(
		Mapping(target = "attachments", ignore = true),
		Mapping(target = "conflicts", ignore = true),
		Mapping(target = "revisionsInfo", ignore = true),
		Mapping(target = "extensions", expression = "kotlin(mapperExtensionsValidationContext.validateAndMapCurrentExtension(customEntityBaseDto.extensions))"),
		Mapping(target = "entityTypeId", expression = "kotlin(entityTypeId)"),
	)
	abstract fun map(
		customEntityBaseDto: CustomEntityBaseDto,
		entityTypeId: String,
		mapperExtensionsValidationContext: MapperExtensionsValidationContext,
	): CustomEntityBase
	fun map(
		customEntityBase: CustomEntityBase,
		entityTypeId: String,
	): CustomEntityBaseDto {
		require(customEntityBase.entityTypeId == entityTypeId) {
			"Entity is not of expected type $entityTypeId"
		}
		return doMap(customEntityBase)
	}
	protected abstract fun doMap(customEntityBase: CustomEntityBase): CustomEntityBaseDto
}