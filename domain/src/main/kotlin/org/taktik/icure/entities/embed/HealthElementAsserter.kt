package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
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
 * The two fields must agree: [asserterType] declares which kind of entity [asserterId] points at. Nothing enforces
 * this. The field is encrypted, so the server never sees the values and cannot validate or repair them; the invariant
 * is owned by the SDK. See ADR 0006.
 *
 * Note on organisations: an organisation (hospital, practice, care home, ...) is not a distinct asserter type.
 * Organisations are stored as [org.taktik.icure.entities.HealthcareParty] records, distinguished from individual
 * practitioners by tags set by the client, so an organisation asserter is an entry with
 * `asserterType = "healthcareParty"` whose [asserterId] points to such a record. The association between a
 * practitioner and the organisation they were acting for at the time of the assertion is deliberately NOT modelled
 * here: if that link matters, the client asserts both parties, or resolves the association from its own data.
 *
 * @property asserterId The id of the entity making the assertion. Which entity it refers to is given by
 * [asserterType].
 * @property asserterType The kind of entity [asserterId] refers to. Free string; using the names of
 * [PartnerType] entries (`patient`, `relatedPerson`, `healthcareParty`) is encouraged but not enforced. [PartnerType]
 * is cited because it answers the same question for [Partnership.partnerId] - not because asserter semantics and
 * partnership semantics coincide. Do not confuse this axis with [org.taktik.icure.entities.base.ParticipantType],
 * which qualifies the *role* a party played rather than the kind of entity it is.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class HealthElementAsserter(
	val asserterId: String,
	val asserterType: String,
) : Serializable
