package org.taktik.icure.services.external.rest.v2.dto.filter.user

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.UserDto
import org.taktik.icure.services.external.rest.v2.dto.filter.AbstractFilterDto

@JsonPolymorphismRoot(AbstractFilterDto::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * A filter that matches users by a free-text search string applied against name, email, and phone fields.
 */
data class UserByNameEmailPhoneFilter(
	/** The search string to match against user name, email address, or phone number. */
	@param:Schema(required = true)
	val searchString: String,
	/** Optional human-readable description of this filter instance. */
	override val desc: String? = null,
) : AbstractFilterDto<UserDto>
