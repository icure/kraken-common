package org.taktik.icure.services.external.rest.v2.dto.filter.code

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.CodeDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches code identifiers within an interval defined by start and end type, code, and version.
 */
data class CodeIdsByTypeCodeVersionIntervalFilter(
	/** Optional description of this filter. */
	override val desc: String? = null,
	/** The type at the start of the interval. */
	val startType: String? = null,
	/** The code value at the start of the interval. */
	val startCode: String? = null,
	/** The version at the start of the interval. */
	val startVersion: String? = null,
	/** The type at the end of the interval. */
	val endType: String? = null,
	/** The code value at the end of the interval. */
	val endCode: String? = null,
	/** The version at the end of the interval. */
	val endVersion: String? = null,
) : AbstractFilterDto<CodeDto>
