/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.filter.maintenancetask

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v1.dto.MaintenanceTaskDto
import org.taktik.icure.services.external.rest.v1.dto.filter.AbstractFilterDto
import org.taktik.icure.services.external.rest.v1.dto.filter.FilterDto

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class MaintenanceTaskByIdsFilter(
	override val ids: Set<String>,
	override val desc: String? = null,
) : AbstractFilterDto<MaintenanceTaskDto>,
	FilterDto.IdsFilter<String, MaintenanceTaskDto>
