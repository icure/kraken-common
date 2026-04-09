package org.taktik.icure.customentities.util

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.LongNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.TextNode
import org.springframework.stereotype.Component
import org.taktik.icure.customentities.mapping.MapperExtensionsValidationContext
import org.taktik.icure.entities.RawJson
import org.taktik.icure.entities.base.Extendable
import org.taktik.icure.errorreporting.ScopedErrorCollector
import org.taktik.icure.errorreporting.addError
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.embed.AddressDto
import org.taktik.icure.services.external.rest.v2.dto.base.ExtendableDto
import org.taktik.icure.services.external.rest.v2.dto.embed.TelecomDto
import org.taktik.icure.services.external.rest.v2.mapper.base.CodeStubV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.AddressV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.TelecomV2Mapper

interface ExtendableBuiltinEntityValidatorMapperConfigsProvider {
	val configs: Map<String, (RawJson.JsonObject, MapperExtensionsValidationContext?, ScopedErrorCollector) -> RawJson.JsonObject>
}

class ExtendableBuiltinEntityValidatorMapperConfigsProviderBuilder(
	private val objectMapper: ObjectMapper
) {
	private val configs = mutableMapOf<String, (RawJson.JsonObject, MapperExtensionsValidationContext?, ScopedErrorCollector) -> RawJson.JsonObject>()

	fun <DTO : ExtendableDto, DOMAIN : Extendable> addMapperForBuiltin(
		entityName: String,
		dtoClass: Class<DTO>,
		mapper: (DTO, MapperExtensionsValidationContext) -> DOMAIN
	) {
		check(!configs.containsKey(entityName)) {
			"Mapper for $entityName already configured"
		}
		configs[entityName] = fun(
			value: RawJson.JsonObject,
			context: MapperExtensionsValidationContext?,
			collector: ScopedErrorCollector
		): RawJson.JsonObject {
			val dto = try {
				objectMapper.treeToValue(value.toJackson(), dtoClass)
			} catch (e: JsonMappingException) {
				// Can maybe add path of jackson problem, but then becomes too implementation-dependent
				collector.addError("GE-OBJECT-BUILTIN", "entityName" to entityName)
				return value
			}
			return objectMapper.readValue(
				objectMapper.writeValueAsString(
					mapper(
						dto,
						context ?: MapperExtensionsValidationContext.NotAllowed(collector)
					)
				),
				RawJson.JsonObject::class.java
			)
		}
	}

	fun <DTO, DOMAIN> addMapperForBuiltin(
		entityName: String,
		dtoClass: Class<DTO>,
		mapper: (DTO) -> DOMAIN
	) {
		check(!configs.containsKey(entityName)) {
			"Mapper for $entityName already configured"
		}
		configs[entityName] = fun(
			value: RawJson.JsonObject,
			context: MapperExtensionsValidationContext?,
			collector: ScopedErrorCollector
		): RawJson.JsonObject {
			check(context == null) {
				"Validating a non-extendable builtin entity as if it was extendable (entity: $entityName)"
			}
			val dto = try {
				objectMapper.treeToValue(value.toJackson(), dtoClass)
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
	}

	fun build(): Map<String, (RawJson.JsonObject, MapperExtensionsValidationContext?, ScopedErrorCollector) -> RawJson.JsonObject> = configs
}

private fun RawJson.toJackson(): JsonNode = when (this) {
	is RawJson.JsonObject -> JsonNodeFactory.instance.objectNode().apply {
		properties.forEach { (key, value) -> set<JsonNode>(key, value.toJackson()) }
	}
	is RawJson.JsonArray -> JsonNodeFactory.instance.arrayNode().apply {
		items.forEach { add(it.toJackson()) }
	}
	is RawJson.JsonString -> TextNode(value)
	is RawJson.JsonInteger -> LongNode(value)
	is RawJson.JsonFloat -> DoubleNode(value)
	RawJson.JsonBoolean.True -> BooleanNode.TRUE
	RawJson.JsonBoolean.False -> BooleanNode.FALSE
	RawJson.JsonNull -> NullNode.instance
}

@Component
class ExtendableBuiltinEntityValidatorMapperConfigsProviderImpl(
	objectMapper: ObjectMapper,
	addressMapper: AddressV2Mapper,
	codeStubMapper: CodeStubV2Mapper,
	telecomMapper: TelecomV2Mapper,
): ExtendableBuiltinEntityValidatorMapperConfigsProvider {
	override val configs = ExtendableBuiltinEntityValidatorMapperConfigsProviderBuilder(objectMapper).apply {
		addMapperForBuiltin(
			entityName = "Address",
			dtoClass = AddressDto::class.java,
			mapper = addressMapper::map
		)
		addMapperForBuiltin(
			entityName = "CodeStub",
			dtoClass = CodeStubDto::class.java,
			mapper = codeStubMapper::mapNotNull
		)
		addMapperForBuiltin(
			entityName = "Telecom",
			dtoClass = TelecomDto::class.java,
			mapper = telecomMapper::map
		)
	}.build()
}