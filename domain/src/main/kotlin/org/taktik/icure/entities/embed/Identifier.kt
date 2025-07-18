package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.annotations.entities.ContentValue
import org.taktik.icure.annotations.entities.ContentValues
import org.taktik.icure.entities.base.CodeStub
import java.io.Serializable

/**
 * An identifier intended for computation
 *
 * An identifier - identifies some entity uniquely and unambiguously. Typically this is used for
 * business identifiers.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Identifier(

	/**
	 * Unique id for inter-element referencing
	 */
	@param:ContentValue(ContentValues.UUID) val id: String? = null,

	/**
	 * Organization that issued id (may be just text)
	 */
	@param:ContentValue(ContentValues.ANY_STRING) val assigner: String? = null,
	/**
	 * Unique id for inter-element referencing
	 */
	/**
	 * Time period when id is/was valid for use
	 */
	val start: String? = null,
	val end: String? = null,
	/**
	 * The namespace for the identifier value
	 */
	val system: String? = null,
	/**
	 * Description of identifier
	 */
	val type: CodeStub? = null,
	/**
	 * usual | official | temp | secondary | old (If known)
	 */
	val use: String? = null,
	/**
	 * The value that is unique
	 */
	val value: String? = null,
) : Serializable
