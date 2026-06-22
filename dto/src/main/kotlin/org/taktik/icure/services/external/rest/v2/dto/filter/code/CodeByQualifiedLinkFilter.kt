package org.taktik.icure.services.external.rest.v2.dto.filter.code

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.CodeDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches codes by their qualified link type and optionally by linked entity identifier.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.filter.code.CodeByQualifiedLinkFilter")
data class CodeByQualifiedLinkFilter(
	/** The type of qualified link to match. */
	@ActiveField val linkType: String,
	/** The optional identifier of the linked entity. */
	@ActiveField val linkedId: String? = null,
	/** Optional description of this filter. */
	override val desc: String? = null,
) : AbstractFilterDto<CodeDto>
