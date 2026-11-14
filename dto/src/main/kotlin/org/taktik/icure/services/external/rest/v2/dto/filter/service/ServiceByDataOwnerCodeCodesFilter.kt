/*
 * Copyright (c) 2026. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v2.dto.filter.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.dto.annotations.filtering.ActiveField
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.embed.ServiceDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import org.taktik.icure.services.external.rest.v2.dto.filter.VersionFilteringDto

/**
 * Filter that matches services shared with a data owner, having at least one of the
 * given codes, optionally within a value date range.
 * With [filterVersion] = [VersionFilteringDto.LATEST] only services whose latest version matches are returned;
 * with [VersionFilteringDto.ANY] (default) a service is returned if any of its versions matches.
 */
@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ServiceByDataOwnerCodeCodesFilter(
	/** The id of the data owner. */
	@ActiveField val dataOwnerId: String,
	/** Map of code type to the accepted code codes. */
	@ActiveField val codeCodes: Map<String, Collection<String>>,
	/** Optional minimum value date (fuzzy date, inclusive). */
	@ActiveField val startValueDate: Long? = null,
	/** Optional maximum value date (fuzzy date, inclusive). */
	@ActiveField val endValueDate: Long? = null,
	/** Optional version filtering; defaults to ANY. */
	@ActiveField val filterVersion: VersionFilteringDto? = null,
	override val desc: String? = null,
) : AbstractFilterDto<ServiceDto>
