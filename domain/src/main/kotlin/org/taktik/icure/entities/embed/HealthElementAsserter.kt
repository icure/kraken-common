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
 * The exactly-one rule is enforced in `init`, on the entity rather than on the DTO: construction fails on both-null and
 * on both-set, whichever path builds the instance - mapping a DTO, a service path, a `copy`. On a write that surfaces
 * as a `400` (`IllegalArgumentException` → `GlobalErrorHandler`). The cost is accepted deliberately, not overlooked:
 * `init` also runs on CouchDB read, so an asserter stored in a shape this check rejects makes the whole health element
 * undeserializable and - the field being encrypted - unfixable through the API. See ADR 0006 decision 10.
 *
 * What is *not* enforced is the *pairing* inside [LocalAsserterIdentifier]: [AsserterType] bounds the vocabulary, not
 * what [LocalAsserterIdentifier.id] actually points at. The field is encrypted, so the server never sees the values and
 * can neither validate nor repair them; that invariant is owned by the SDK.
 *
 * Note on organisations: an organisation (hospital, practice, care home, ...) is not a distinct asserter type.
 * Organisations are stored as [org.taktik.icure.entities.HealthcareParty] records, distinguished from individual
 * practitioners by tags set by the client, so an organisation asserter is a [localAsserterIdentifier] with
 * `type = AsserterType.healthcareParty` whose `id` points to such a record. The association between a practitioner and
 * the organisation they were acting for at the time of the assertion is deliberately NOT modelled here: if that link
 * matters, the client asserts both parties, or resolves the association from its own data.
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
	init {
		require((localAsserterIdentifier == null) != (externalAsserterIdentifier == null)) {
			"Exactly one of localAsserterIdentifier and externalAsserterIdentifier must be set"
		}
	}

	/**
	 * A reference to the record, stored in this iCure instance, of the party making the assertion.
	 *
	 * The fields are bare `id` / `type` rather than `asserterId` / `asserterType`: inside a class that already says
	 * whose identifier this is, the prefix only stutters. `type` here is the *entity-kind* axis, and it is unambiguous
	 * because this class holds nothing else - the role axis, if HealthElement ever needs one, belongs on
	 * [HealthElementAsserter] itself, where `type` and `function` are still free. See ADR 0006 decision 8.
	 *
	 * @property id The id of the entity making the assertion. Which entity it refers to is given by [type].
	 * @property type The kind of entity [id] refers to. Do not confuse this axis with
	 * [org.taktik.icure.entities.base.ParticipantType], which qualifies the *role* a party played rather than the kind
	 * of entity it is.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
	data class LocalAsserterIdentifier(
		val id: String,
		val type: AsserterType,
	) : Serializable
}
