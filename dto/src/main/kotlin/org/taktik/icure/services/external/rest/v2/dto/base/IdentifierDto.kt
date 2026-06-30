package org.taktik.icure.services.external.rest.v2.dto.base

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.ExposedToCustomEntities
import java.io.Serializable
import com.fasterxml.jackson.annotation.JsonFilter
import org.taktik.icure.dto.annotations.filtering.ActiveField

/**
 * An identifier intended for computation
 *
 * An identifier - identifies some entity uniquely and unambiguously. Typically this is used for
 * business identifiers.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@ExposedToCustomEntities
data class IdentifierDto(
	/**
	 * Unique id for inter-element referencing
	 */
	@ActiveField val id: String? = null,
	/**
	 * Organization that issued id (may be just text)
	 */
	@ActiveField val assigner: String? = null,
	/**
	 * Unique id for inter-element referencing
	 */
	/**
	 * Time period when id is/was valid for use
	 */
	@ActiveField val start: String? = null,
	@ActiveField val end: String? = null,
	/**
	 * The namespace for the identifier value
	 */
	@ActiveField val system: String? = null,
	/**
	 * Description of identifier
	 */
	@ActiveField val type: CodeStubDto? = null,
	/**
	 * usual | official | temp | secondary | old (If known)
	 */
	@ActiveField val use: String? = null,
	/**
	 * The value that is unique
	 */
	@ActiveField val value: String? = null,
) : Serializable
