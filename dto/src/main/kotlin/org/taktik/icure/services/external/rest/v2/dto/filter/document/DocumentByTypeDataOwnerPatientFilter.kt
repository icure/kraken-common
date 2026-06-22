package org.taktik.icure.services.external.rest.v2.dto.filter.document

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.DocumentDto
import org.taktik.icure.services.external.rest.v2.dto.embed.DocumentTypeDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches documents by document type, data owner, and patient.
 */
data class DocumentByTypeDataOwnerPatientFilter(
	/** The identifier of the data owner. */
	@ActiveField val dataOwnerId: String,
	/** The type of document to match. */
	@ActiveField val documentType: DocumentTypeDto,
	/** The set of secret patient keys used for secure delegation matching. */
	@ActiveField val secretPatientKeys: Set<String>,
	/** Optional description of this filter. */
	override val desc: String? = null,
) : AbstractFilterDto<DocumentDto>
