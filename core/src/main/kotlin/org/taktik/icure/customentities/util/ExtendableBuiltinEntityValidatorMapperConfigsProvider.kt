package org.taktik.icure.customentities.util

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.taktik.icure.customentities.mapping.MapperExtensionsValidationContext
import org.taktik.icure.entities.RawJson
import org.taktik.icure.entities.embed.Extendable
import org.taktik.icure.services.external.rest.v1.mapper.base.CodeStubMapper
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.embed.AddressDto
import org.taktik.icure.services.external.rest.v2.dto.embed.ExtendableDto
import org.taktik.icure.services.external.rest.v2.mapper.base.CodeStubV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.AddressV2Mapper

interface ExtendableBuiltinEntityValidatorMapperConfigsProvider {
	val configs: Map<String, (RawJson.JsonObject, MapperExtensionsValidationContext?) -> RawJson.JsonObject>
}

// Note: for multiplatform we will need to have a different implementation, not based on mappers but only based on
// cardinal models + multiplatform serialization
@Component
class ExtendableBuiltinEntityValidatorMapperConfigsProviderImpl(
	objectMapper: ObjectMapper,
	addressMapper: AddressV2Mapper,
	codeStubMapper: CodeStubV2Mapper,
): ExtendableBuiltinEntityValidatorMapperConfigsProvider {
	override val configs = run {
		fun <DTO : ExtendableDto, DOMAIN : Extendable> mapperForBuiltin(
			entityName: String,
			dtoClass: Class<DTO>,
			mapper: (DTO, MapperExtensionsValidationContext) -> DOMAIN
		): Pair<String, (RawJson.JsonObject, MapperExtensionsValidationContext?) -> RawJson.JsonObject> {
			return entityName to { value, context ->
				val dto = objectMapper.readValue(objectMapper.writeValueAsString(value), dtoClass)
				val domain = mapper(dto, context ?: MapperExtensionsValidationContext.Empty)
				val domainJson =
					objectMapper.readValue(objectMapper.writeValueAsString(domain), RawJson.JsonObject::class.java)
				domainJson
			}
		}

		fun <DTO, DOMAIN> mapperForBuiltin(
			entityName: String,
			dtoClass: Class<DTO>,
			mapper: (DTO) -> DOMAIN
		): Pair<String, (RawJson.JsonObject, MapperExtensionsValidationContext?) -> RawJson.JsonObject> {
			return entityName to { value, context ->
				check (context == null) {
					"Validating a non-extendable builtin entity as if it was extendable (entity: $entityName)"
				}
				val dto = objectMapper.readValue(objectMapper.writeValueAsString(value), dtoClass)
				val domain = mapper(dto)
				val domainJson =
					objectMapper.readValue(objectMapper.writeValueAsString(domain), RawJson.JsonObject::class.java)
				domainJson
			}
		}

		mapOf(
			mapperForBuiltin(
				entityName = "Address",
				dtoClass = AddressDto::class.java,
				mapper = addressMapper::map
			),
			mapperForBuiltin(
				entityName = "CodeStub",
				dtoClass = CodeStubDto::class.java,
				mapper = codeStubMapper::mapNotNull
			)
		)
	}
}