package org.taktik.icure.services.external.rest.v2.dto.filter.document

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.DocumentDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches documents by data owner and code.
 */
@JsonFilter("org.taktik.icure.services.external.rest.v2.dto.filter.document.DocumentByDataOwnerCodeFilter")
data class DocumentByDataOwnerCodeFilter(
	/** The identifier of the data owner. */
	@ActiveField val dataOwnerId: String,
	/** The type of the code to match. */
	@ActiveField val codeType: String,
	/** The code value to match. */
	@ActiveField val codeCode: String? = null,
	/** Optional description of this filter. */
	override val desc: String? = null,
) : AbstractFilterDto<DocumentDto>
