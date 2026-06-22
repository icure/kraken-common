/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v2.dto.filter.patient

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.PatientDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches patients by data owner and tag.
 */
data class PatientByDataOwnerTagFilter(
	/** Optional description of this filter. */
	override val desc: String? = null,
	/** The identifier of the data owner. */
	@ActiveField val dataOwnerId: String,
	/** The type of the tag to match. */
	@ActiveField val tagType: String,
	/** The tag code value to match. */
	@ActiveField val tagCode: String? = null,
) : AbstractFilterDto<PatientDto>
