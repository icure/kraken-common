package org.taktik.icure.services.external.rest.v2.dto.filter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.base.IdentifiableDto
import org.taktik.icure.services.external.rest.v2.dto.utils.ExternalFilterKeyDto
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that queries an external CouchDB view to retrieve matching entities.
 */
data class ExternalViewFilter(
	/** Optional description of this filter. */
	override val desc: String? = null,
	/** The name of the external view to query. */
	@ActiveField val view: String,
	/** The partition of the view. */
	@ActiveField val partition: String,
	/** The fully qualified name of the entity type to filter. */
	@ActiveField val entityQualifiedName: String,
	/** The start key for the view query range. */
	@ActiveField val startKey: ExternalFilterKeyDto?,
	/** The end key for the view query range. */
	@ActiveField val endKey: ExternalFilterKeyDto?,
) : AbstractFilterDto<IdentifiableDto<String>>
