package org.taktik.icure.entities.embed

import org.taktik.icure.entities.base.EnumVersion

/**
 * The kind of entity a [HealthElementAsserter.LocalAsserterIdentifier.asserterId] refers to.
 *
 * This is the *entity-kind* axis, not the role axis: it says what sort of record the id points at, not what part the
 * party played in the assertion. Do not confuse it with [org.taktik.icure.entities.base.ParticipantType].
 *
 * It applies to the *local* branch of [HealthElementAsserter] only: an asserter named by
 * [HealthElementAsserter.externalAsserterIdentifier] has no record here, so it carries no entity kind.
 *
 * The entries deliberately mirror [PartnerType], which answers the same question for [Partnership.partnerId]. The two
 * enums are kept separate on purpose: partnership vocabulary and asserter vocabulary are free to diverge, and neither
 * should widen because the other did.
 *
 * Note that an organisation (hospital, practice, care home, ...) is not a distinct entry: organisations are stored as
 * [org.taktik.icure.entities.HealthcareParty] records, so they use [healthcareParty]. See ADR 0006.
 */
@EnumVersion(1L)
enum class AsserterType {
	patient,
	healthcareParty,
	relatedPerson,
}
