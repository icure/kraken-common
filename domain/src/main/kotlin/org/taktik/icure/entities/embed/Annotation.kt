package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.utils.DynamicInitializer
import org.taktik.icure.utils.invoke
import org.taktik.icure.validation.AutoFix
import org.taktik.icure.validation.NotNull
import org.taktik.icure.validation.ValidCode
import java.io.Serializable
import java.util.*

/**
 * Text node with attribution.
 * Could be written by a healthcare party, as a side node of a medical record.
 * For example, after taking a temperature, the HCP adds a node explaining the thermometer is faulty.
 *
 * @property id The Id of the annotation. We encourage using either a v4 UUID or a HL7 Id.
 * @property author The id of the User that has created this note, will be filled automatically if missing with current user id. Not enforced by the application server.
 * @property created The timestamp (unix epoch in ms) of creation of the note, will be filled automatically if missing. Not enforced by the application server.
 * @property modified The date (unix epoch in ms) of the latest modification of the note, will be filled automatically if missing. Not enforced by the application server.
 * @property text Text contained in the note, written as markdown.
 * @property location Defines to which part of the corresponding information the note is related to
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Annotation(
	@JsonProperty("_id") override val id: String = UUID.randomUUID().toString(),
	@field:NotNull(autoFix = AutoFix.CURRENTUSERID, applyOnModify = false) val author: String? = null,
	@field:NotNull(autoFix = AutoFix.NOW) val created: Long? = null,
	@field:NotNull(autoFix = AutoFix.NOW) val modified: Long? = null,
	@field:ValidCode(autoFix = AutoFix.NORMALIZECODE) val tags: Set<CodeStub> = emptySet(),
	@Deprecated("Use markdown instead") val text: String? = null,
	val markdown: Map<String, String> = emptyMap(),
	val confidential: Boolean? = null,
	val location: String? = null,
	val encryptedSelf: String? = null,
) : Identifiable<String>,
	Serializable {

	companion object : DynamicInitializer<Annotation>

	fun merge(other: Annotation) = Annotation(args = this.solveConflictsWith(other))

	fun solveConflictsWith(other: Annotation) = mapOf(
		"id" to (this.id),
		"author" to (this.author ?: other.author),
		"created" to (this.created ?: other.created),
		"modified" to (this.modified ?: other.modified),
		"text" to (this.text ?: other.text),
		"markdown" to (other.markdown + this.markdown),
		"location" to (this.location ?: other.location),
		"tags" to (this.tags + other.tags),
		"confidential" to (this.confidential ?: other.confidential),
		"encryptedSelf" to (this.encryptedSelf ?: other.encryptedSelf),
	)
}
