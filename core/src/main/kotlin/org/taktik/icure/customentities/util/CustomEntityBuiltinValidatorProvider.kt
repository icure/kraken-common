package org.taktik.icure.customentities.util

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.taktik.icure.domain.customentities.config.ExtendableEntityName
import org.taktik.icure.domain.customentities.mapping.MapperExtensionsValidationContext
import org.taktik.icure.domain.customentities.util.CustomEntityBuiltinValidator
import org.taktik.icure.domain.customentities.util.CustomEntityConfigValidationContext
import org.taktik.icure.entities.RawJson
import org.taktik.icure.entities.embed.Extendable
import org.taktik.icure.services.external.rest.v2.dto.embed.AddressDto
import org.taktik.icure.services.external.rest.v2.dto.embed.ExtendableDto
import org.taktik.icure.services.external.rest.v2.mapper.embed.AddressV2Mapper

// Note: for multiplatform we will need to have a different implementation, not based on mappers but only based on
// cardinal models + multiplatform serialization
@Component
class CustomEntityBuiltinValidatorProvider(
	objectMapper: ObjectMapper,
	addressMapper: AddressV2Mapper,
) {
	private final val configs = kotlin.run {
		fun <DTO : ExtendableDto, DOMAIN : Extendable> mapperForBuiltin(
			dtoClass: Class<DTO>,
			mapper: (DTO, MapperExtensionsValidationContext) -> DOMAIN
		): (RawJson.JsonObject, MapperExtensionsValidationContext) -> RawJson.JsonObject {
			return { value, context ->
				val dto = objectMapper.readValue(objectMapper.writeValueAsString(value), dtoClass)
				val domain = mapper(dto, context)
				val domainJson =
					objectMapper.readValue(objectMapper.writeValueAsString(domain), RawJson.JsonObject::class.java)
				domainJson
			}
		}

		mapOf(
			ExtendableEntityName.Address to mapperForBuiltin(
				dtoClass = AddressDto::class.java,
				mapper = addressMapper::map
			),
		)
	}

	fun getValidator(
		fullValidationContext: MapperExtensionsValidationContext
	) = object : CustomEntityBuiltinValidator {
		override fun validateAndMapExtendableBuiltinForStore(
			extendableEntityName: ExtendableEntityName,
			context: CustomEntityConfigValidationContext,
			value: RawJson.JsonObject
		): RawJson.JsonObject =
			// All validators for embeddable entities should be present, if entity is not embeddable the config should
			// have not been valid in the first place
			checkNotNull(configs[extendableEntityName]) {
				"Missing builtin validator for $extendableEntityName"
			}.invoke(value, fullValidationContext)
	}
}