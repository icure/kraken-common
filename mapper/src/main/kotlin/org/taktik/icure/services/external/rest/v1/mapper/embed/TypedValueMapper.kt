/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.embed.TypedValuesType
import org.taktik.icure.entities.embed.TypedValue
import org.taktik.icure.services.external.rest.InstantMapper
import org.taktik.icure.services.external.rest.v1.dto.embed.TypedValueDto
import org.taktik.icure.services.external.rest.v1.dto.embed.TypedValuesTypeDto
import java.util.*

@Mapper(componentModel = "spring", uses = [InstantMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
abstract class TypedValueMapper {
	fun map(typedValueDto: TypedValueDto<*>?): TypedValue<*>? {
		return if (typedValueDto == null) null else when (typedValueDto.type) {
			TypedValuesTypeDto.STRING -> TypedValue.withTypeAndValue(TypedValuesType.STRING, typedValueDto.getValue<String>())
			TypedValuesTypeDto.DATE -> TypedValue.withTypeAndValue(TypedValuesType.DATE, typedValueDto.getValue<Date>())
			TypedValuesTypeDto.INTEGER -> TypedValue.withTypeAndValue(TypedValuesType.INTEGER, typedValueDto.getValue<Int>())
			TypedValuesTypeDto.DOUBLE -> TypedValue.withTypeAndValue(TypedValuesType.DOUBLE, typedValueDto.getValue<Double>())
			TypedValuesTypeDto.BOOLEAN -> TypedValue.withTypeAndValue(TypedValuesType.BOOLEAN, typedValueDto.getValue<Boolean>())
			TypedValuesTypeDto.CLOB -> TypedValue.withTypeAndValue(TypedValuesType.CLOB, typedValueDto.getValue<String>())
			TypedValuesTypeDto.JSON -> TypedValue.withTypeAndValue(TypedValuesType.JSON, typedValueDto.getValue<String>())
			null -> TypedValue<String>(
				booleanValue = typedValueDto.booleanValue,
				integerValue = typedValueDto.integerValue,
				doubleValue = typedValueDto.doubleValue,
				stringValue = typedValueDto.stringValue,
				dateValue = typedValueDto.dateValue
			)
		} ?: TypedValue<String>(
			booleanValue = typedValueDto.booleanValue,
			integerValue = typedValueDto.integerValue,
			doubleValue = typedValueDto.doubleValue,
			stringValue = typedValueDto.stringValue,
			dateValue = typedValueDto.dateValue
		)
	}

	fun map(typedValue: TypedValue<*>?): TypedValueDto<*>? {
		return if (typedValue == null) null else when (typedValue.type) {
			TypedValuesType.STRING -> TypedValueDto.withTypeAndValue(TypedValuesTypeDto.STRING, typedValue.getValue<String>())
			TypedValuesType.DATE -> TypedValueDto.withTypeAndValue(TypedValuesTypeDto.DATE, typedValue.getValue<Date>())
			TypedValuesType.INTEGER -> TypedValueDto.withTypeAndValue(TypedValuesTypeDto.INTEGER, typedValue.getValue<Int>())
			TypedValuesType.DOUBLE -> TypedValueDto.withTypeAndValue(TypedValuesTypeDto.DOUBLE, typedValue.getValue<Double>())
			TypedValuesType.BOOLEAN -> TypedValueDto.withTypeAndValue(TypedValuesTypeDto.BOOLEAN, typedValue.getValue<Boolean>())
			TypedValuesType.CLOB -> TypedValueDto.withTypeAndValue(TypedValuesTypeDto.CLOB, typedValue.getValue<String>())
			TypedValuesType.JSON -> TypedValueDto.withTypeAndValue(TypedValuesTypeDto.JSON, typedValue.getValue<String>())
			null -> TypedValueDto<String>(
				booleanValue = typedValue.booleanValue,
				integerValue = typedValue.integerValue,
				doubleValue = typedValue.doubleValue,
				stringValue = typedValue.stringValue,
				dateValue = typedValue.dateValue
			)
		}
	}
}
