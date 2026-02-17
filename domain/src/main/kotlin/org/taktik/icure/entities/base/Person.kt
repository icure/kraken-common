/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.base

import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.entities.embed.Address
import org.taktik.icure.entities.embed.Gender
import org.taktik.icure.entities.embed.PersonName
import org.taktik.icure.entities.utils.MergeUtil.mergeListsDistinct
import org.taktik.icure.mergers.annotations.MergeStrategyUse
import java.io.Serializable

interface Person :
	Serializable,
	Identifiable<String> {
	val civility: String?
	val gender: Gender?
	val firstName: String?
	val lastName: String?
	val names: List<PersonName>
	val companyName: String?
	val addresses: List<Address>
	@MergeStrategyUse("mergeListOfStringsIgnoringCase") val languages: List<String>

	fun solveConflictsWith(other: Person): Map<String, Any?> = mapOf(
		"id" to this.id,
		"civility" to (this.civility ?: other.civility),
		"gender" to (this.gender ?: other.gender),
		"firstName" to (this.firstName ?: other.firstName),
		"lastName" to (this.lastName ?: other.lastName),
		"addresses" to mergeListsDistinct(
			this.addresses,
			other.addresses,
			{ a, b -> a.addressType?.equals(b.addressType) ?: false },
			{ a, b -> a.merge(b) },
		),
		"languages" to mergeListsDistinct(this.languages, other.languages, { a, b -> a.equals(b, true) }, { a, _ -> a }),
		"names" to mergeListsDistinct(
			this.names,
			other.names,
			{ a, b -> a.use == b.use && a.lastName == b.lastName },
			{ a, _ -> a },
		),
	)
}
