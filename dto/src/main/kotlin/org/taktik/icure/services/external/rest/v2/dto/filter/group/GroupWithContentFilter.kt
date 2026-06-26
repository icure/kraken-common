package org.taktik.icure.services.external.rest.v2.dto.filter.group

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.GroupDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches groups within a super group whose content matches a search string.
 */
data class GroupWithContentFilter(
	/** The identifier of the super group to search within. */
	@ActiveField val superGroupId: String,
	/** The string to search for in group content. */
	@ActiveField val searchString: String,
	/** Optional description of this filter. */
	override val desc: String? = null,
) : AbstractFilterDto<GroupDto>
