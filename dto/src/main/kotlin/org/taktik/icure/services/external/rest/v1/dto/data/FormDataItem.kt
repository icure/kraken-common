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
package org.taktik.icure.services.external.rest.v1.dto.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.commons.serialization.SerializableValue
import org.taktik.icure.services.external.rest.v1.dto.CodeDto
import java.io.Serializable
import java.util.Date

/**
 * Created by aduchate on 01/02/13, 12:27
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class FormDataItem(
	val openingDate: Date? = null,
	val closingDate: Date? = null,
	val previousVersion: FormItem? = null,
	val content: Map<String, SerializableValue>? = null,
	val listOfMultipleValues: List<Map<String, SerializableValue>>? = null,
	val codes: List<CodeDto> = emptyList(),
) : IFormItem,
	Serializable
