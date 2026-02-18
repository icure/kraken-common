/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.entities.base.PropertyStub
import org.taktik.icure.entities.utils.MergeUtil.mergeSets
import org.taktik.icure.mergers.annotations.MergeStrategyUseReference
import org.taktik.icure.mergers.annotations.Mergeable
import org.taktik.icure.mergers.annotations.NonMergeable
import org.taktik.icure.utils.DynamicInitializer
import org.taktik.icure.utils.invoke
import java.io.Serializable
import java.util.*

/**
 * Created by aduchate on 02/07/13, 11:59
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Mergeable(["type", "healthcarePartyId"])
data class PatientHealthCareParty(
	val type: PatientHealthCarePartyType? = null,
	@NonMergeable val referral: Boolean = false, // mark this phcp as THE active referral link (gmd)
	val healthcarePartyId: String? = null,
	val sendFormats: Map<TelecomType, String> = emptyMap(), // String is in fact a UTI (uniform type identifier / a sort of super-MIME)
	@MergeStrategyUseReference("org.taktik.icure.entities.embed.PatientHealthCareParty.Companion.mergeReferralPeriods")
	val referralPeriods: SortedSet<ReferralPeriod> = sortedSetOf(), // History of DMG ownerships
	val properties: Set<PropertyStub>? = null,
	override val encryptedSelf: String? = null,
) : Encryptable,
	Serializable {
	companion object : DynamicInitializer<PatientHealthCareParty> {
		fun mergeReferralPeriods(
			thisReferralPeriods: SortedSet<ReferralPeriod>,
			otherReferralPeriods: SortedSet<ReferralPeriod>
		): SortedSet<ReferralPeriod> = mergeSets(
			thisReferralPeriods,
			otherReferralPeriods,
			{ a, b -> a.startDate == b.startDate },
			{ a, b -> if (a.endDate == null) a.copy(endDate = b.endDate) else a },
		).toSortedSet()
	}

	fun merge(other: PatientHealthCareParty) = PatientHealthCareParty(args = this.solveConflictsWith(other))
	fun solveConflictsWith(other: PatientHealthCareParty) = super.solveConflictsWith(other) +
		mapOf(
			"type" to (this.type ?: other.type),
			"referral" to this.referral,
			"healthcarePartyId" to (this.healthcarePartyId ?: other.healthcarePartyId),
			"sendFormats" to (other.sendFormats + this.sendFormats),
			"referralPeriods" to mergeSets(
				this.referralPeriods,
				other.referralPeriods,
				{ a, b -> a.startDate == b.startDate },
				{ a, b -> if (a.endDate == null) a.copy(endDate = b.endDate) else a },
			).toSortedSet(),
		)
}
