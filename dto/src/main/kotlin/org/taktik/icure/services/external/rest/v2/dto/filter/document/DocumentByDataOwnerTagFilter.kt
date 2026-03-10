package org.taktik.icure.services.external.rest.v2.dto.filter.document

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.DocumentDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches documents by data owner and tag.
 */
data class DocumentByDataOwnerTagFilter(
	/** The identifier of the data owner. */
	val dataOwnerId: String,
	/** The type of the tag to match. */
	val tagType: String,
	/** The tag code value to match. */
	val tagCode: String? = null,
	/** Optional description of this filter. */
	override val desc: String? = null,
) : AbstractFilterDto<DocumentDto>
