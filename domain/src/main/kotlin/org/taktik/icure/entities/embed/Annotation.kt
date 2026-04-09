package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.mergers.annotations.Mergeable
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.NotNull
import org.taktik.icure.validation.ValidCode
import java.io.Serializable
import java.util.*

/**
 * Text node with attribution.
 * Could be written by a healthcare party, as a side node of a medical record.
 * For example, after taking a temperature, the HCP adds a node explaining the thermometer is faulty.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Mergeable(["id"])
data class Annotation(
	/** The Id of the annotation. We encourage using either a v4 UUID or a HL7 Id. */
	@param:JsonProperty("_id") override val id: String = UUID.randomUUID().toString(),
	/** The id of the User that has created this note, will be filled automatically if missing with current user id. Not enforced by the application server. */
	@field:NotNull(autoFix = AutoFix.CURRENTUSERID, applyOnModify = false) val author: String? = null,
	/** The timestamp (unix epoch in ms) of creation of the note, will be filled automatically if missing. Not enforced by the application server. */
	@field:NotNull(autoFix = AutoFix.NOW) val created: Long? = null,
	/** The date (unix epoch in ms) of the latest modification of the note, will be filled automatically if missing. Not enforced by the application server. */
	@field:NotNull(autoFix = AutoFix.NOW) val modified: Long? = null,
	/** Tags associated with this annotation. */
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) val tags: Set<CodeStub> = emptySet(),
	/** Text contained in the note, written as markdown. */
	@Deprecated("Use markdown instead") val text: String? = null,
	/** Localized text contained in the note, written as markdown. Keys should respect ISO 639-1. */
	val markdown: Map<String, String> = emptyMap(),
	/** Whether this annotation is confidential. */
	val confidential: Boolean? = null,
	/** Defines to which part of the corresponding information the note is related to */
	val location: String? = null,
	/** The encrypted content of this annotation. */
	val encryptedSelf: String? = null,
) : Identifiable<String>,
	Serializable
