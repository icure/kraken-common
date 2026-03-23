package org.taktik.icure.customentities.util

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.taktik.icure.customentities.mapping.MapperExtensionsValidationContext
import org.taktik.icure.entities.RawJson
import org.taktik.icure.entities.embed.Extendable
import org.taktik.icure.errorreporting.ScopedErrorCollector
import org.taktik.icure.errorreporting.addError
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.embed.AddressDto
import org.taktik.icure.services.external.rest.v2.dto.embed.ExtendableDto
import org.taktik.icure.services.external.rest.v2.dto.embed.TelecomDto
import org.taktik.icure.services.external.rest.v2.mapper.base.CodeStubV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.AddressV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.TelecomV2Mapper

interface ExtendableBuiltinEntityValidatorMapperConfigsProvider {
	val configs: Map<String, (RawJson.JsonObject, MapperExtensionsValidationContext?, ScopedErrorCollector) -> RawJson.JsonObject>
}

// Note: for multiplatform we will need to have a different implementation, not based on mappers but only based on
// cardinal models + multiplatform serialization
@Component
class ExtendableBuiltinEntityValidatorMapperConfigsProviderImpl(
	objectMapper: ObjectMapper,
	addressMapper: AddressV2Mapper,
	codeStubMapper: CodeStubV2Mapper,
	telecomMapper: TelecomV2Mapper,
): ExtendableBuiltinEntityValidatorMapperConfigsProvider {
	override val configs = run {

		fun <DTO : ExtendableDto, DOMAIN : Extendable> mapperForBuiltin(
			entityName: String,
			dtoClass: Class<DTO>,
			mapper: (DTO, MapperExtensionsValidationContext) -> DOMAIN
		): Pair<String, (RawJson.JsonObject, MapperExtensionsValidationContext?, ScopedErrorCollector) -> RawJson.JsonObject> =
			entityName to fun (value: RawJson.JsonObject, context: MapperExtensionsValidationContext?, collector: ScopedErrorCollector): RawJson.JsonObject {
				val dto = try {
					objectMapper.readValue(objectMapper.writeValueAsString(value), dtoClass)
				} catch (e: JsonMappingException) {
					// Can maybe add path of jackson problem, but then becomes too implementation-dependent
					collector.addError("GE-OBJECT-BUILTIN", "entityName" to entityName)
					return value
				}
				return objectMapper.readValue(
					objectMapper.writeValueAsString(mapper(dto, context ?: MapperExtensionsValidationContext.NotAllowed(collector))),
					RawJson.JsonObject::class.java
				)
			}

		fun <DTO, DOMAIN> mapperForBuiltin(
			entityName: String,
			dtoClass: Class<DTO>,
			mapper: (DTO) -> DOMAIN
		): Pair<String, (RawJson.JsonObject, MapperExtensionsValidationContext?, ScopedErrorCollector) -> RawJson.JsonObject> =
			entityName to fun (value: RawJson.JsonObject, context: MapperExtensionsValidationContext?, collector: ScopedErrorCollector): RawJson.JsonObject {
				check (context == null) {
					"Validating a non-extendable builtin entity as if it was extendable (entity: $entityName)"
				}
				val dto = try {
					objectMapper.readValue(objectMapper.writeValueAsString(value), dtoClass)
				} catch (e: JsonMappingException) {
					// Can maybe add path of jackson problem, but then becomes too implementation-dependent
					collector.addError("GE-OBJECT-BUILTIN", "entityName" to entityName)
					return value
				}
				return objectMapper.readValue(
					objectMapper.writeValueAsString(mapper(dto)),
					RawJson.JsonObject::class.java
				)
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
			),
			mapperForBuiltin(
				entityName = "Telecom",
				dtoClass = TelecomDto::class.java,
				mapper = telecomMapper::map
			)
		)
	}
}