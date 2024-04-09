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

package org.taktik.icure.services.external.rest.v2.mapper

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.FormTemplate
import org.taktik.icure.services.external.rest.v2.dto.FormTemplateDto
import org.taktik.icure.services.external.rest.v2.dto.embed.form.template.FormTemplateLayout
import org.taktik.icure.services.external.rest.v2.mapper.base.CodeStubV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.embed.DocumentGroupV2Mapper
import java.lang.IllegalArgumentException

@Mapper(componentModel = "spring", uses = [DocumentGroupV2Mapper::class, CodeStubV2Mapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
abstract class FormTemplateV2Mapper {
	val json: ObjectMapper = ObjectMapper().registerModule(
		KotlinModule.Builder()
			.configure(KotlinFeature.NullIsSameAsDefault, true)
			.configure(KotlinFeature.NullToEmptyCollection, true)
			.configure(KotlinFeature.NullToEmptyMap, true)
			.build()
	).apply { setSerializationInclusion(JsonInclude.Include.NON_NULL) }

	val yaml: ObjectMapper = ObjectMapper(YAMLFactory()).registerModule(
		KotlinModule.Builder()
			.configure(KotlinFeature.NullIsSameAsDefault, true)
			.configure(KotlinFeature.NullToEmptyCollection, true)
			.configure(KotlinFeature.NullToEmptyMap, true)
			.build()
	).apply { setSerializationInclusion(JsonInclude.Include.NON_NULL) }

	@Mappings(
		Mapping(target = "isAttachmentDirty", ignore = true),
		Mapping(target = "layout", ignore = true),
		Mapping(target = "templateLayout", source = "formTemplateDto"),
		Mapping(target = "attachments", ignore = true),
		Mapping(target = "revHistory", ignore = true),
		Mapping(target = "conflicts", ignore = true),
		Mapping(target = "revisionsInfo", ignore = true)
	)
	abstract fun map(formTemplateDto: FormTemplateDto): FormTemplate

	@Mappings(
		Mapping(target = "rawTemplateLayout", ignore = true),
	)
	abstract fun map(formTemplate: FormTemplate): FormTemplateDto

	fun mapTemplateLayout(formTemplateLayout: ByteArray?): FormTemplateLayout? = formTemplateLayout?.let {
		try {
			if (it[0] == 123.toByte()) json.readValue(it, FormTemplateLayout::class.java) else
				yaml.readValue(it, FormTemplateLayout::class.java)
		} catch (e: Exception) {
			throw IllegalArgumentException("Could not parse form template layout. Try again requesting the raw template.")
		}
	}

	fun mapLayout(formTemplateDto: FormTemplateDto): ByteArray? {
		return formTemplateDto.templateLayout?.let {
			json.writeValueAsBytes(it)
		}
	}
}
