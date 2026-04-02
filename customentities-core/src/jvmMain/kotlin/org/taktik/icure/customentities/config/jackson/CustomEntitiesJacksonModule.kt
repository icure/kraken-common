package org.taktik.icure.customentities.config.jackson

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import org.taktik.icure.customentities.config.migration.EnumMigration
import org.taktik.icure.customentities.config.typing.BinaryTypeConfig
import org.taktik.icure.customentities.config.typing.BooleanTypeConfig
import org.taktik.icure.customentities.config.typing.EnumTypeConfig
import org.taktik.icure.customentities.config.typing.FloatTypeConfig
import org.taktik.icure.customentities.config.typing.FuzzyDateTimeTypeConfig
import org.taktik.icure.customentities.config.typing.FuzzyDateTypeConfig
import org.taktik.icure.customentities.config.typing.FuzzyTimeTypeConfig
import org.taktik.icure.customentities.config.typing.GenericTypeConfig
import org.taktik.icure.customentities.config.typing.IntTypeConfig
import org.taktik.icure.customentities.config.typing.JsonTypeConfig
import org.taktik.icure.customentities.config.typing.ListTypeConfig
import org.taktik.icure.customentities.config.typing.MapTypeConfig
import org.taktik.icure.customentities.config.typing.ObjectDefinition
import org.taktik.icure.customentities.config.typing.ObjectTypeConfig
import org.taktik.icure.customentities.config.typing.StringTypeConfig
import org.taktik.icure.customentities.config.typing.UnknownTypeConfig
import org.taktik.icure.customentities.config.typing.UuidTypeConfig

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
	JsonSubTypes.Type(value = BinaryTypeConfig::class, name = "Binary"),
	JsonSubTypes.Type(value = BooleanTypeConfig::class, name = "Boolean"),
	JsonSubTypes.Type(value = EnumTypeConfig::class, name = "Enum"),
	JsonSubTypes.Type(value = FloatTypeConfig::class, name = "Float"),
	JsonSubTypes.Type(value = FuzzyDateTimeTypeConfig::class, name = "FuzzyDateTime"),
	JsonSubTypes.Type(value = FuzzyDateTypeConfig::class, name = "FuzzyDate"),
	JsonSubTypes.Type(value = FuzzyTimeTypeConfig::class, name = "FuzzyTime"),
	JsonSubTypes.Type(value = IntTypeConfig::class, name = "Int"),
	JsonSubTypes.Type(value = JsonTypeConfig::class, name = "Json"),
	JsonSubTypes.Type(value = ListTypeConfig::class, name = "List"),
	JsonSubTypes.Type(value = MapTypeConfig::class, name = "Map"),
	JsonSubTypes.Type(value = ObjectTypeConfig::class, name = "Object"),
	JsonSubTypes.Type(value = StringTypeConfig::class, name = "String"),
	JsonSubTypes.Type(value = UuidTypeConfig::class, name = "Uuid"),
	JsonSubTypes.Type(value = UnknownTypeConfig::class, name = "Unknown"),
)
private abstract class GenericTypeConfigMixin

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
	JsonSubTypes.Type(value = ObjectDefinition.PropertyConfiguration.DefaultValue.Constant::class, name = "Constant"),
	JsonSubTypes.Type(value = ObjectDefinition.PropertyConfiguration.DefaultValue.GenerateUuidV4::class, name = "GenerateUuidV4"),
	JsonSubTypes.Type(value = ObjectDefinition.PropertyConfiguration.DefaultValue.NowDateTime::class, name = "NowDateTime"),
	JsonSubTypes.Type(value = ObjectDefinition.PropertyConfiguration.DefaultValue.NowDate::class, name = "NowDate"),
	JsonSubTypes.Type(value = ObjectDefinition.PropertyConfiguration.DefaultValue.NowTime::class, name = "NowTime"),
)
private abstract class DefaultValueMixin

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
	JsonSubTypes.Type(value = MapTypeConfig.ValidationConfig.KeyValidation.EnumKeyValidation::class, name = "Enum"),
	JsonSubTypes.Type(value = MapTypeConfig.ValidationConfig.KeyValidation.StringKeyValidation::class, name = "String"),
)
private abstract class KeyValidationMixin

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
	JsonSubTypes.Type(value = EnumMigration.TargetValue.Use::class, name = "Use"),
	JsonSubTypes.Type(value = EnumMigration.TargetValue.Null::class, name = "Null"),
	JsonSubTypes.Type(value = EnumMigration.TargetValue.Custom::class, name = "Custom"),
)
private abstract class TargetValueMixin

private class JsonTypeConfigDeserializer : StdDeserializer<JsonTypeConfig>(JsonTypeConfig::class.java) {
	override fun deserialize(p: JsonParser, ctxt: DeserializationContext): JsonTypeConfig {
		var token = p.currentToken()
		if (token == JsonToken.START_OBJECT) token = p.nextToken()
		while (token == JsonToken.FIELD_NAME) {
			p.nextToken()
			p.skipChildren()
			token = p.nextToken()
		}
		return JsonTypeConfig
	}
}

private class UnknownTypeConfigDeserializer : StdDeserializer<UnknownTypeConfig>(UnknownTypeConfig::class.java) {
	override fun deserialize(p: JsonParser, ctxt: DeserializationContext): UnknownTypeConfig {
		var nullable = false
		var token = p.currentToken()
		if (token == JsonToken.START_OBJECT) token = p.nextToken()
		while (token == JsonToken.FIELD_NAME) {
			val fieldName = p.currentName()
			p.nextToken()
			when (fieldName) {
				"nullable" -> nullable = p.booleanValue
				else -> p.skipChildren()
			}
			token = p.nextToken()
		}
		return if (nullable) UnknownTypeConfig.Nullable else UnknownTypeConfig.NonNull
	}
}

class CustomEntitiesJacksonModule : SimpleModule("CustomEntities") {
	init {
		setMixInAnnotation(GenericTypeConfig::class.java, GenericTypeConfigMixin::class.java)
		setMixInAnnotation(ObjectDefinition.PropertyConfiguration.DefaultValue::class.java, DefaultValueMixin::class.java)
		setMixInAnnotation(MapTypeConfig.ValidationConfig.KeyValidation::class.java, KeyValidationMixin::class.java)
		setMixInAnnotation(EnumMigration.TargetValue::class.java, TargetValueMixin::class.java)
		addDeserializer(JsonTypeConfig::class.java, JsonTypeConfigDeserializer())
		addDeserializer(UnknownTypeConfig::class.java, UnknownTypeConfigDeserializer())
	}
}