package org.taktik.icure.services.external.rest.v2.dto.filter.contact

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.ContactDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Filter that matches contacts by data owner and service code.
 */
data class ContactByDataOwnerServiceCodeFilter(
	/** The identifier of the data owner. */
	val dataOwnerId: String,
	/** The type of the service code. */
	val codeType: String,
	/** The code value to match. */
	val codeCode: String?,
	/** Optional description of this filter. */
	override val desc: String? = null,
) : AbstractFilterDto<ContactDto>
