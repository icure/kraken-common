package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.io.Serializable

/**
 * The party asserting that the patient has the [org.taktik.icure.entities.HealthElement] this asserter is attached to.
 *
 * This is the FHIR-style *asserter* concept: it does not say who recorded or authored the health element (that is
 * covered by [org.taktik.icure.entities.HealthElement.author] and [org.taktik.icure.entities.HealthElement.responsible]),
 * it says on whose word the health element is held to be true. A patient may self-report an allergy, a family member
 * may report a condition on behalf of the patient, and a physician may assert a diagnosis: all three are asserters,
 * and the same health element may carry more than one of them.
 *
 * Note on organisations: an organisation (hospital, practice, care home, ...) is not a separate branch of this
 * hierarchy. Organisations are stored as [org.taktik.icure.entities.HealthcareParty] records, distinguished from
 * individual practitioners by tags set by the client, so an organisation asserter is a [HealthElementAsserter.HealthcareParty]
 * whose [HealthElementAsserter.HealthcareParty.hcpId] points to such a record. The association between a practitioner and the organisation
 * they were acting for at the time of the assertion is deliberately NOT modelled here: if that link matters, the
 * client asserts both parties, or resolves the association from its own data.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
	JsonSubTypes.Type(value = HealthElementAsserter.Patient::class, name = "patient"),
	JsonSubTypes.Type(value = HealthElementAsserter.RelatedPerson::class, name = "relatedPerson"),
	JsonSubTypes.Type(value = HealthElementAsserter.HealthcareParty::class, name = "healthcareParty"),
)
sealed interface HealthElementAsserter : Serializable {
	/**
	 * A patient asserts the health element. Usually the subject of the health element themselves, making it
	 * self-reported, but not necessarily: a mother who is herself a patient may assert about her child.
	 *
	 * @property patientId The id of the [org.taktik.icure.entities.Patient] making the assertion. There is no
	 * marker for "the subject of this health element" - the id is always explicit, and nothing enforces that it
	 * matches the subject.
	 */
	data class Patient(
		val patientId: String,
	) : HealthElementAsserter,
		Serializable

	/**
	 * Someone related to the patient - a parent, a spouse, a carer, ... - asserts the health element on the patient's
	 * behalf.
	 *
	 * @property relatedPersonId The id of the [org.taktik.icure.entities.RelatedPerson] making the assertion.
	 */
	data class RelatedPerson(
		val relatedPersonId: String,
	) : HealthElementAsserter,
		Serializable

	/**
	 * A healthcare party asserts the health element. This covers both individual practitioners and organisations:
	 * both are stored as [org.taktik.icure.entities.HealthcareParty] records and are told apart by client-side tags,
	 * not by the shape of this branch.
	 *
	 * @property hcpId The id of the [org.taktik.icure.entities.HealthcareParty] making the assertion.
	 */
	data class HealthcareParty(
		val hcpId: String,
	) : HealthElementAsserter,
		Serializable
}
