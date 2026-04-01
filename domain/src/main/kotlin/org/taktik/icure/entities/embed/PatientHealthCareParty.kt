/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.base.PropertyStub
import org.taktik.icure.mergers.Merger
import org.taktik.icure.mergers.annotations.MergeStrategyUse
import org.taktik.icure.mergers.annotations.Mergeable
import java.io.Serializable
import java.util.*

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Mergeable(["type", "healthcarePartyId"])
data class PatientHealthCareParty(
	val type: PatientHealthCarePartyType? = null,
	val referral: Boolean = false, // mark this phcp as THE active referral link (gmd)
	val healthcarePartyId: String? = null,
	val sendFormats: Map<TelecomType, String> = emptyMap(), // String is in fact a UTI (uniform type identifier / a sort of super-MIME)
	@MergeStrategyUse(
		canMerge = "canMergeReferralPeriods({{LEFT}}.{{PROP}}, {{RIGHT}}.{{PROP}})",
		merge = "mergeReferralPeriods({{LEFT}}.{{PROP}}, {{RIGHT}}.{{PROP}})",
		imports = [
			"org.taktik.icure.entities.embed.PatientHealthCareParty.Companion.canMergeReferralPeriods",
			"org.taktik.icure.entities.embed.PatientHealthCareParty.Companion.mergeReferralPeriods",
		]
	)
	val referralPeriods: SortedSet<ReferralPeriod> = sortedSetOf(), // History of DMG ownerships
	val properties: Set<PropertyStub>? = null,
	override val encryptedSelf: String? = null,
) : Encryptable,
	Serializable {
	companion object {

		context(mergerCtx: Merger<*>)
		fun canMergeReferralPeriods(
			thisReferralPeriods: SortedSet<ReferralPeriod>,
			otherReferralPeriods: SortedSet<ReferralPeriod>
		): Boolean = mergerCtx.canMergeSetsOfMergeable(
			l = thisReferralPeriods,
			r = otherReferralPeriods,
			canMerge = { a, b ->
				a.startDate == b.startDate &&
					(a.endDate == null || b.endDate == null || a.endDate == b.endDate) &&
					(a.comment == null || b.comment == null || b.comment == a.comment)

			},
			idEquals = { a, b -> a.startDate == b.startDate },
			idGetter = { it.startDate }
		)

		context(mergerCtx: Merger<*>)
		fun mergeReferralPeriods(
			thisReferralPeriods: SortedSet<ReferralPeriod>,
			otherReferralPeriods: SortedSet<ReferralPeriod>
		): SortedSet<ReferralPeriod> = mergerCtx.mergeSetsOfMergeable(
			l = thisReferralPeriods,
			r = otherReferralPeriods,
			merge = { a, b ->
				ReferralPeriod(
					startDate = a.startDate ?: b.startDate,
					endDate = a.endDate ?: b.endDate,
					comment = a.comment ?: b.comment,
				)
			},
			idEquals = { a, b -> a.startDate == b.startDate },
			idGetter = { it.startDate }
		).toSortedSet()
	}

}
