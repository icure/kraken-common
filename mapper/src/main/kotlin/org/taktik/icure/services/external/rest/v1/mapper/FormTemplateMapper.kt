/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper

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
import org.taktik.icure.services.external.rest.v1.dto.FormTemplateDto
import org.taktik.icure.services.external.rest.v1.dto.embed.form.template.FormTemplateLayout
import org.taktik.icure.services.external.rest.v1.dto.gui.layout.FormLayout
import org.taktik.icure.services.external.rest.v1.mapper.base.CodeStubMapper
import org.taktik.icure.services.external.rest.v1.mapper.embed.DocumentGroupMapper
import java.lang.IllegalArgumentException

@Mapper(componentModel = "spring", uses = [DocumentGroupMapper::class, CodeStubMapper::class], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
abstract class FormTemplateMapper {
	val json: ObjectMapper = ObjectMapper().registerModule(
		KotlinModule.Builder()
			.configure(KotlinFeature.NullIsSameAsDefault, true)
			.configure(KotlinFeature.NullToEmptyCollection, true)
			.configure(KotlinFeature.NullToEmptyMap, true)
			.build(),
	).apply { setSerializationInclusion(JsonInclude.Include.NON_NULL) }

	val yaml: ObjectMapper = ObjectMapper(YAMLFactory()).registerModule(
		KotlinModule.Builder()
			.configure(KotlinFeature.NullIsSameAsDefault, true)
			.configure(KotlinFeature.NullToEmptyCollection, true)
			.configure(KotlinFeature.NullToEmptyMap, true)
			.build(),
	).apply { setSerializationInclusion(JsonInclude.Include.NON_NULL) }

	@Mappings(
		Mapping(target = "isAttachmentDirty", ignore = true),
		Mapping(target = "layout", ignore = true),
		Mapping(target = "templateLayout", source = "formTemplateDto"),
		Mapping(target = "attachments", ignore = true),
		Mapping(target = "revHistory", ignore = true),
		Mapping(target = "conflicts", ignore = true),
		Mapping(target = "revisionsInfo", ignore = true),
	)
	abstract fun map(formTemplateDto: FormTemplateDto): FormTemplate

	@Mappings(
		Mapping(target = "rawTemplateLayout", ignore = true),
	)
	abstract fun map(formTemplate: FormTemplate): FormTemplateDto

	fun mapLayout(formLayout: ByteArray?): FormLayout? = formLayout?.let {
		try {
			if (it[0] == 123.toByte()) {
				json.readValue(it, FormLayout::class.java)
			} else {
				yaml.readValue(it, FormLayout::class.java)
			}
		} catch (e: Exception) {
			throw IllegalArgumentException("Could not parse form template layout. Try again requesting the raw template.")
		}
	}

	fun mapTemplateLayout(formTemplateLayout: ByteArray?): FormTemplateLayout? = formTemplateLayout?.let {
		try {
			if (it[0] == 123.toByte()) {
				json.readValue(it, FormTemplateLayout::class.java)
			} else {
				yaml.readValue(it, FormTemplateLayout::class.java)
			}
		} catch (e: Exception) {
			try {
				yaml.readValue(it, FormTemplateLayout::class.java)
			} catch (e: Exception) {
				throw IllegalArgumentException("Could not parse form template layout. Try again requesting the raw template.")
			}
		}
	}

	fun mapLayout(formTemplateDto: FormTemplateDto): ByteArray? = formTemplateDto.templateLayout?.let {
		json.writeValueAsBytes(it)
	}
}
