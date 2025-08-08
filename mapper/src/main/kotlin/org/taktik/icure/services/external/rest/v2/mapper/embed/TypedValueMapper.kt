/*
 *  iCure Data Stack. Copyright (c) 2020 Taktik SA
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public
 *     License along with this program.  If not, see
 *     <https://www.gnu.org/licenses/>.
 */

package org.taktik.icure.services.external.rest.v2.mapper.embed

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.embed.TypedValue
import org.taktik.icure.entities.embed.TypedValuesType
import org.taktik.icure.services.external.rest.InstantMapper
import org.taktik.icure.services.external.rest.v2.dto.embed.TypedValueDto
import org.taktik.icure.services.external.rest.v2.dto.embed.TypedValuesTypeDto
import java.util.*

@Mapper(componentModel = "spring", uses = [InstantMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
abstract class TypedValueV2Mapper {
	fun map(typedValueDto: TypedValueDto?): TypedValue? = if (typedValueDto == null) {
		null
	} else {
		when (typedValueDto.type) {
			TypedValuesTypeDto.STRING -> TypedValue.withTypeAndValue(TypedValuesType.STRING, typedValueDto.stringValue)
			TypedValuesTypeDto.DATE -> TypedValue.withTypeAndValue(TypedValuesType.DATE, typedValueDto.dateValue)
			TypedValuesTypeDto.INTEGER -> TypedValue.withTypeAndValue(TypedValuesType.INTEGER, typedValueDto.integerValue)
			TypedValuesTypeDto.DOUBLE -> TypedValue.withTypeAndValue(TypedValuesType.DOUBLE, typedValueDto.doubleValue)
			TypedValuesTypeDto.BOOLEAN -> TypedValue.withTypeAndValue(TypedValuesType.BOOLEAN, typedValueDto.booleanValue)
			TypedValuesTypeDto.CLOB -> TypedValue.withTypeAndValue(TypedValuesType.CLOB, typedValueDto.stringValue)
			TypedValuesTypeDto.JSON -> TypedValue.withTypeAndValue(TypedValuesType.JSON, typedValueDto.stringValue)
			null -> TypedValue(
				booleanValue = typedValueDto.booleanValue,
				integerValue = typedValueDto.integerValue,
				doubleValue = typedValueDto.doubleValue,
				stringValue = typedValueDto.stringValue,
				dateValue = typedValueDto.dateValue,
			)
		} ?: TypedValue(
			booleanValue = typedValueDto.booleanValue,
			integerValue = typedValueDto.integerValue,
			doubleValue = typedValueDto.doubleValue,
			stringValue = typedValueDto.stringValue,
			dateValue = typedValueDto.dateValue,
		)
	}

	fun map(typedValue: TypedValue?): TypedValueDto? = if (typedValue == null) {
		null
	} else {
		when (typedValue.type) {
			TypedValuesType.STRING -> TypedValueDto.withTypeAndValue(TypedValuesTypeDto.STRING, typedValue.stringValue)
			TypedValuesType.DATE -> TypedValueDto.withTypeAndValue(TypedValuesTypeDto.DATE, typedValue.dateValue)
			TypedValuesType.INTEGER -> TypedValueDto.withTypeAndValue(TypedValuesTypeDto.INTEGER, typedValue.integerValue)
			TypedValuesType.DOUBLE -> TypedValueDto.withTypeAndValue(TypedValuesTypeDto.DOUBLE, typedValue.doubleValue)
			TypedValuesType.BOOLEAN -> TypedValueDto.withTypeAndValue(TypedValuesTypeDto.BOOLEAN, typedValue.booleanValue)
			TypedValuesType.CLOB -> TypedValueDto.withTypeAndValue(TypedValuesTypeDto.CLOB, typedValue.stringValue)
			TypedValuesType.JSON -> TypedValueDto.withTypeAndValue(TypedValuesTypeDto.JSON, typedValue.stringValue)
			null -> TypedValueDto(
				booleanValue = typedValue.booleanValue,
				integerValue = typedValue.integerValue,
				doubleValue = typedValue.doubleValue,
				stringValue = typedValue.stringValue,
				dateValue = typedValue.dateValue,
			)
		}
	}
}
