/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.base

import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.entities.embed.Address
import org.taktik.icure.entities.embed.Gender
import org.taktik.icure.entities.embed.PersonName
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
	@MergeStrategyUse(
		canMerge = "true",
		merge = "mergeListOfStringsIgnoringCase({{LEFT}}.{{PROP}}, {{RIGHT}}.{{PROP}})",
	)
	val languages: List<String>
}
