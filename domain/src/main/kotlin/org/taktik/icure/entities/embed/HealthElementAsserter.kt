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
 * The party is named in exactly one of two ways, and exactly one of the two fields is therefore set:
 * - [localAsserterIdentifier] names a party stored in this iCure instance: an id, plus the [AsserterType] saying which
 *   kind of record that id points at;
 * - [externalAsserterIdentifier] names a party that has no record here, through a business [Identifier] issued by
 *   another system. There is deliberately no [AsserterType] on this branch: the kind of a record we do not store is
 *   not knowable to us.
 *
 * Nothing here is validated by the server. The field is encrypted, so the server never sees the values and can neither
 * validate nor repair them: nothing enforces that exactly one branch is set, and nothing enforces the *pairing* inside
 * [LocalAsserterIdentifier] - [AsserterType] bounds the vocabulary, not what [LocalAsserterIdentifier.asserterId]
 * actually points at. Both invariants are owned by the SDK; the exactly-one check is mirrored on
 * `HealthElementAsserterDto` (a `400` for callers that bypass SDK encryption) and deliberately **not** here, so that a
 * single bad stored value cannot make the whole health element undeserializable. See ADR 0006.
 *
 * Note on organisations: an organisation (hospital, practice, care home, ...) is not a distinct asserter type.
 * Organisations are stored as [org.taktik.icure.entities.HealthcareParty] records, distinguished from individual
 * practitioners by tags set by the client, so an organisation asserter is a [localAsserterIdentifier] with
 * `asserterType = AsserterType.healthcareParty` whose `asserterId` points to such a record. The association between a
 * practitioner and the organisation they were acting for at the time of the assertion is deliberately NOT modelled
 * here: if that link matters, the client asserts both parties, or resolves the association from its own data.
 *
 * @property localAsserterIdentifier The asserting party, as a reference to a record stored in this instance. Null when
 * the party is named by [externalAsserterIdentifier].
 * @property externalAsserterIdentifier The asserting party, as a business identifier from a system that is not this
 * one. Null when the party is named by [localAsserterIdentifier]. Carries no [AsserterType].
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class HealthElementAsserter(
	val localAsserterIdentifier: LocalAsserterIdentifier? = null,
	val externalAsserterIdentifier: Identifier? = null,
) : Serializable {
	/**
	 * A reference to the record, stored in this iCure instance, of the party making the assertion.
	 *
	 * @property asserterId The id of the entity making the assertion. Which entity it refers to is given by
	 * [asserterType].
	 * @property asserterType The kind of entity [asserterId] refers to. Do not confuse this axis with
	 * [org.taktik.icure.entities.base.ParticipantType], which qualifies the *role* a party played rather than the kind
	 * of entity it is.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
	data class LocalAsserterIdentifier(
		val asserterId: String,
		val asserterType: AsserterType,
	) : Serializable
}
