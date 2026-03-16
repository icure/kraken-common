/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v2.dto.filter.maintenancetask

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.MaintenanceTaskDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches maintenance tasks created after a specific date.
 */
data class MaintenanceTaskAfterDateFilter(
	/** Optional description of this filter. */
	override val desc: String? = null,
	/** The identifier of the healthcare party. */
	val healthcarePartyId: String? = null,
	/** The date threshold; only tasks after this date are matched. */
	@param:Schema(required = true)
	val date: Long,
) : AbstractFilterDto<MaintenanceTaskDto>
