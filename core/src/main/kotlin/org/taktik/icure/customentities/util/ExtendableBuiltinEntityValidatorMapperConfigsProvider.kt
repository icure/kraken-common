package org.taktik.icure.customentities.util

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.LongNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.taktik.icure.customentities.config.typing.truncateValueForErrorMessage
import org.taktik.icure.customentities.mapping.MapperExtensionsValidationContext
import org.taktik.icure.entities.RawJson
import org.taktik.icure.entities.base.Extendable
import org.taktik.icure.errorreporting.ScopedErrorCollector
import org.taktik.icure.errorreporting.addError
import org.taktik.icure.services.external.rest.v2.dto.base.ExtendableDto
import org.taktik.icure.services.external.rest.v2.mapper.base.CodeStubV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.AddressV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.TelecomV2Mapper
import kotlin.collections.set

interface ExtendableBuiltinEntityValidatorMapperConfigsProvider {
	val objectToDomain: Map<String, (RawJson.JsonObject, MapperExtensionsValidationContext?, ScopedErrorCollector) -> RawJson.JsonObject>
	val enumToDomain: Map<String, (RawJson.JsonString, ScopedErrorCollector) -> RawJson.JsonString>
}

class ExtendableBuiltinEntityValidatorMapperConfigsProviderImpl(
	override val objectToDomain: Map<String, (RawJson.JsonObject, MapperExtensionsValidationContext?, ScopedErrorCollector) -> RawJson.JsonObject>,
	override val enumToDomain: Map<String, (RawJson.JsonString, ScopedErrorCollector) -> RawJson.JsonString>,
) : ExtendableBuiltinEntityValidatorMapperConfigsProvider

class ExtendableBuiltinEntityValidatorMapperObjectProviderBuilder(
	val objectMapper: ObjectMapper
) {
	val objectToDomain = mutableMapOf<String, (RawJson.JsonObject, MapperExtensionsValidationContext?, ScopedErrorCollector) -> RawJson.JsonObject>()
	val enumToDomain = mutableMapOf<String, (RawJson.JsonString, ScopedErrorCollector) -> RawJson.JsonString>()

	inline fun <reified FROM : Enum<FROM>> createOneEnumMapper(
		enumType: String
	): (RawJson.JsonString, ScopedErrorCollector) -> RawJson.JsonString {
		val fromClass = FROM::class.java
		return lambda@{ value, errorCollector ->
			try {
				objectMapper.treeToValue(TextNode(value.value), fromClass)
				// Enums are converted using "valueOf" only, no need to do round-trip mapping like for objects
			} catch (e: JsonMappingException) {
				errorCollector.addError(
					"GE-ENUM-VALUE-BUILTIN",
					"value" to truncateValueForErrorMessage(value.value),
					"ref" to truncateValueForErrorMessage(enumType),
				)
			}
			return@lambda value // Must still use the serialized value, parse above is only for checking validity
		}
	}

	inline fun <reified DTO : Enum<DTO>, reified DOMAIN : Enum<DOMAIN>> addMapperForBuiltinEnum(
		enumType: String
	) {
		check(!enumToDomain.containsKey(enumType)) {
			"Mapper for enum $enumType already configured"
		}
		enumToDomain[enumType] = createOneEnumMapper<DTO>(enumType)
	}

	inline fun <DTO : Any, DOMAIN : Any> doAddMappersForBuiltinObject(
		entityName: String,
		dtoClass: Class<DTO>,
		checkValidationContext: (MapperExtensionsValidationContext?) -> Unit,
		mapperToDomain: (DTO, MapperExtensionsValidationContext?, ScopedErrorCollector) -> DOMAIN,
		mapperToDto: (DOMAIN) -> DTO,
	) {
		check(!objectToDomain.containsKey(entityName)) {
			"Mapper for $entityName already configured"
		}
		objectToDomain[entityName] = fun(
			value: RawJson.JsonObject,
			context: MapperExtensionsValidationContext?,
			collector: ScopedErrorCollector
		): RawJson.JsonObject {
			checkValidationContext(context)
			val parsedDto = try {
				objectMapper.treeToValue(value.toJackson(), dtoClass)
			} catch (_: JsonMappingException) {
				// Can maybe add path of jackson problem, but then becomes too implementation-dependent
				collector.addError("GE-OBJECT-BUILTIN", "entityName" to entityName)
				return value
			}
			// Round-trip mappig to domain and back to dto, if the mapper does some normalization we ensure we get it
			// this way
			val remapped = mapperToDto(mapperToDomain(parsedDto, context, collector))
			return objectMapper.valueToTree<JsonNode>(remapped).toRawJson() as RawJson.JsonObject
		}
	}

	// Add mappers for an object that is extendable (and available)
	inline fun <reified DTO : ExtendableDto, reified DOMAIN : Extendable> addMappersForExtendableBuiltinObject(
		mapperToDomain: (DTO, MapperExtensionsValidationContext) -> DOMAIN,
		mapperToDto: (DOMAIN) -> DTO,
	) {
		val entityName = DOMAIN::class.simpleName!!
		val dtoClass = DTO::class.java
		doAddMappersForBuiltinObject(
			entityName = entityName,
			dtoClass = dtoClass,
			checkValidationContext = {},
			mapperToDomain = { dto, context, collector ->
				mapperToDomain(
					dto,
					context ?: MapperExtensionsValidationContext.NotAllowed(collector)
				)
			},
			mapperToDto = mapperToDto
		)
	}

	// Add mappers for an object that is available but not extendable
	inline fun <reified DTO : Any, reified DOMAIN : Any> addMappersForAvailableBuiltinObject(
		mapperToDomain: (DTO) -> DOMAIN,
		mapperToDto: (DOMAIN) -> DTO,
	) {
		val entityName = DOMAIN::class.simpleName!!
		val dtoClass = DTO::class.java
		doAddMappersForBuiltinObject(
			entityName = entityName,
			dtoClass = dtoClass,
			checkValidationContext = { context ->
				check(context == null) {
					"Validating a non-extendable builtin entity as if it was extendable (entity: $entityName)"
				}
			},
			mapperToDomain = { dto, _, _ -> mapperToDomain(dto) },
			mapperToDto = mapperToDto
		)
	}

	fun build() = object : ExtendableBuiltinEntityValidatorMapperConfigsProvider {
		override val objectToDomain =
			this@ExtendableBuiltinEntityValidatorMapperObjectProviderBuilder.objectToDomain.toMap()
		override val enumToDomain =
			this@ExtendableBuiltinEntityValidatorMapperObjectProviderBuilder.enumToDomain.toMap()
	}

	companion object {
		fun RawJson.toJackson(): JsonNode = when (this) {
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

		fun JsonNode.toRawJson(): RawJson = when (this) {
			is ObjectNode -> RawJson.JsonObject(
				properties().associate { (key, value) -> key to value.toRawJson() }
			)
			is ArrayNode -> RawJson.JsonArray(
				elements().asSequence().map { it.toRawJson() }.toList()
			)
			is TextNode -> RawJson.JsonString(textValue())
			is LongNode, is IntNode -> RawJson.JsonInteger(longValue())
			is DoubleNode -> RawJson.JsonFloat(doubleValue())
			is BooleanNode -> if (booleanValue()) RawJson.JsonBoolean.True else RawJson.JsonBoolean.False
			is NullNode -> RawJson.JsonNull
			else -> throw IllegalStateException("Unexpected JsonNode type: ${this::class}")
		}
	}
}

@Configuration
class ExtendableBuiltinEntitiesValidationConfig {
	@Bean
	fun provider(
		objectMapper: ObjectMapper,
		addressMapper: AddressV2Mapper,
		codeStubMapper: CodeStubV2Mapper,
		telecomMapper: TelecomV2Mapper,
	): ExtendableBuiltinEntityValidatorMapperConfigsProvider = ExtendableBuiltinEntityValidatorMapperObjectProviderBuilder(
		objectMapper
	).apply {
		addMappersForExtendableBuiltinObject(
			mapperToDomain = addressMapper::map,
			mapperToDto = addressMapper::map,
		)
		addMappersForAvailableBuiltinObject(
			mapperToDomain = codeStubMapper::mapNotNull,
			mapperToDto = codeStubMapper::map,
		)
		addMappersForExtendableBuiltinObject(
			mapperToDomain = telecomMapper::map,
			mapperToDto = telecomMapper::map,
		)
	}.build()
}
