package org.taktik.icure.services.external.rest.v2.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.io.Serializable
import org.taktik.icure.dto.annotations.filtering.ActiveField

/**
 * The party asserting that the patient has the healthcare element this asserter is attached to.
 *
 * This is the FHIR-style *asserter* concept: it does not say who recorded or authored the healthcare element, it says
 * on whose word the healthcare element is held to be true. A patient may self-report an allergy, a family member may
 * report a condition on behalf of the patient, and a physician may assert a diagnosis: all three are asserters, and the
 * same healthcare element may carry more than one of them.
 *
 * Note on organisations: an organisation (hospital, practice, care home, ...) is not a separate branch of this
 * hierarchy. Organisations are stored as healthcare party records, distinguished from individual practitioners by tags
 * set by the client, so an organisation asserter is a [HealthElementAsserterDto.HealthcareParty] whose
 * [HealthElementAsserterDto.HealthcareParty.hcpId] points to such a record. The association between a practitioner and
 * the organisation they were acting for at the time of the assertion is deliberately NOT modelled here.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
	JsonSubTypes.Type(value = HealthElementAsserterDto.Patient::class, name = "patient"),
	JsonSubTypes.Type(value = HealthElementAsserterDto.RelatedPerson::class, name = "relatedPerson"),
	JsonSubTypes.Type(value = HealthElementAsserterDto.HealthcareParty::class, name = "healthcareParty"),
)
sealed interface HealthElementAsserterDto : Serializable {
	/**
	 * The patient themselves asserts the healthcare element: it is self-reported.
	 *
	 * @property patientId The id of the patient making the assertion. Usually the same patient the healthcare element
	 * is about, but nothing enforces it.
	 */
	data class Patient(
		@ActiveField val patientId: String,
	) : HealthElementAsserterDto,
		Serializable

	/**
	 * Someone related to the patient - a parent, a spouse, a carer, ... - asserts the healthcare element on the
	 * patient's behalf.
	 *
	 * @property relatedPersonId The id of the related person making the assertion.
	 */
	data class RelatedPerson(
		@ActiveField val relatedPersonId: String,
	) : HealthElementAsserterDto,
		Serializable

	/**
	 * A healthcare party asserts the healthcare element. This covers both individual practitioners and organisations:
	 * both are stored as healthcare party records and are told apart by client-side tags, not by the shape of this
	 * branch.
	 *
	 * @property hcpId The id of the healthcare party making the assertion.
	 */
	data class HealthcareParty(
		@ActiveField val hcpId: String,
	) : HealthElementAsserterDto,
		Serializable
}
