package org.taktik.icure.services.external.rest.v2.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import java.io.Serializable
import org.taktik.icure.dto.annotations.filtering.ActiveField
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import org.taktik.icure.services.external.rest.v2.dto.base.IdentifierDto

/**
 * The party asserting that the patient has the healthcare element this asserter is attached to.
 *
 * This is the FHIR-style *asserter* concept: it does not say who recorded or authored the healthcare element, it says
 * on whose word the healthcare element is held to be true. A patient may self-report an allergy, a family member may
 * report a condition on behalf of the patient, and a physician may assert a diagnosis: all three are asserters, and the
 * same healthcare element may carry more than one of them.
 *
 * The party is named in exactly one of two ways, and exactly one of the two fields must be set:
 * - [localAsserterIdentifier] names a party stored in this iCure instance: an id, plus the [AsserterTypeDto] saying
 *   which kind of record that id points at;
 * - [externalAsserterIdentifier] names a party that has no record here, through a business [IdentifierDto] issued by
 *   another system, optionally qualified by codes. There is deliberately no [AsserterTypeDto] on this branch: the kind
 *   of a record we do not store is not knowable to us.
 *
 * The exactly-one rule is **not** checked on this DTO. It is checked one layer down, in `HealthElementAsserter`'s
 * `init`: mapping this DTO constructs one, so a violation still surfaces as a `400`, and the rule also covers the write
 * paths that never build a DTO at all. This class only checks that [LocalAsserterIdentifier.id] is not blank. Nothing
 * enforces the *pairing* inside [LocalAsserterIdentifier]: [AsserterTypeDto] bounds the vocabulary, not what
 * [LocalAsserterIdentifier.id] actually points at; that invariant is owned by the SDK.
 *
 * Note on organisations: an organisation (hospital, practice, care home, ...) is not a distinct asserter type.
 * Organisations are stored as healthcare party records, distinguished from individual practitioners by tags set by the
 * client, so an organisation asserter is a [localAsserterIdentifier] with `type = AsserterTypeDto.healthcareParty`
 * whose `id` points to such a record. The association between a practitioner and the organisation they were acting for
 * at the time of the assertion is deliberately NOT modelled here.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class HealthElementAsserterDto(
	/**
	 * The asserting party, as a reference to a record stored in this instance. Null when the party is named by
	 * [externalAsserterIdentifier].
	 */
	@ActiveField val localAsserterIdentifier: LocalAsserterIdentifier? = null,
	/**
	 * The asserting party, as a business identifier from a system that is not this one, optionally qualified by codes.
	 * Null when the party is named by [localAsserterIdentifier]. Carries no [AsserterTypeDto].
	 */
	@ActiveField val externalAsserterIdentifier: ExternalAsserterIdentifier? = null,
) : Serializable {
	/**
	 * A reference to the record, stored in iCure.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonIgnoreProperties(ignoreUnknown = true)
	data class LocalAsserterIdentifier(
		/** The id of the entity making the assertion. Which entity it refers to is given by [type]. */
		@ActiveField val id: String,
		/**
		 * The kind of entity [id] refers to. This is the kind of entity, not the role the party played - do not
		 * confuse it with ParticipantTypeDto.
		 */
		@ActiveField val type: AsserterTypeDto,
	) : Serializable {
		init {
			require(id.isNotBlank()) { "id cannot be blank" }
		}
	}

	/**
	 * The party making the assertion, when it has no record in this iCure instance.
	 *
	 * The party is named by a business [identifier] issued by another system: a national registry number, an entry in
	 * the sending hospital's directory, and so on. Because the record lives elsewhere there is no [AsserterTypeDto]
	 * here - the kind of a record we do not store is not knowable to us. What the issuing system does say about the
	 * party (its kind, its profession, ...) can be carried as [codes], so that a client can qualify an external
	 * asserter without the server pretending to know what it points at.
	 */
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	@JsonIgnoreProperties(ignoreUnknown = true)
	data class ExternalAsserterIdentifier(
		/**
		 * The business identifier of the party in the system that issued it. `system` names that issuing system and
		 * `value` is the party's identifier within it; together they are what makes the party resolvable.
		 */
		@ActiveField val identifier: IdentifierDto,
		/**
		 * Codes qualifying the external party, as stated by the system the [identifier] comes from: for instance the
		 * kind of party or its profession. Empty by default, and omitted from the JSON when empty.
		 */
		@ActiveField val codes: Set<CodeStubDto> = emptySet(),
	) : Serializable
}
