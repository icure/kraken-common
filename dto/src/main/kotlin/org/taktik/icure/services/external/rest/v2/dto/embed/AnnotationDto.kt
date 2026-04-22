package org.taktik.icure.services.external.rest.v2.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.ExposedToCustomEntities
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.IdentifiableDto
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
	description = """Text node with attribution. Could be written by a healthcare party, as a side node of a
    |medical record. For example, after taking a temperature, the HCP adds a node explaining the
    |thermometer is faulty.""",
)
/**
 * Text node with attribution that can be attached to a medical record. Used by healthcare parties to add side notes,
 * for example to flag a faulty thermometer after taking a temperature.
 */
@ExposedToCustomEntities
data class AnnotationDto(
	@param:Schema(
		description = "The Id of the Annotation. We encourage using either a v4 UUID or a HL7 Id.",
	/** The Id of the Annotation. We encourage using either a v4 UUID or a HL7 Id. */
	) override val id: String = UUID.randomUUID().toString(),
	/** The identifier of the author of this annotation. */
	val author: String? = null,
	@param:Schema(
		description = "The timestamp (unix epoch in ms) of creation of this note, will be filled automatically if missing. Not enforced by the application server.",
	/** The timestamp (unix epoch in ms) of creation of this note, filled automatically if missing. */
	) val created: Long? = null,
	@param:Schema(
		description = "The timestamp (unix epoch in ms) of the latest modification of this note, will be filled automatically if missing. Not enforced by the application server.",
	/** The timestamp (unix epoch in ms) of the latest modification of this note, filled automatically if missing. */
	) val modified: Long? = null,
	/** Text contained in the note, written as markdown. Deprecated in favor of [markdown]. */
	@param:Schema(description = "Text contained in the note, written as markdown.", deprecated = true) val text: String? = null,
	/** Localized text contained in the note, written as markdown. Keys should respect ISO 639-1. */
	@param:Schema(description = "Localized text contained in the note, written as markdown. Keys should respect ISO 639-1") val markdown: Map<String, String> = emptyMap(),
	/** Defines to which part of the corresponding information the note is related to. */
	@param:Schema(description = "Defines to which part of the corresponding information the note is related to.") val location: String? = null,
	/** Whether this annotation is confidential. */
	val confidential: Boolean? = null,
	/** Tags associated with this annotation. */
	val tags: Set<CodeStubDto> = emptySet(),
	/** The encrypted content of this annotation. */
	val encryptedSelf: String? = null,
) : IdentifiableDto<String>
