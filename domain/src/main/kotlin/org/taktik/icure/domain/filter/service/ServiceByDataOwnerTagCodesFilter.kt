/*
 * Copyright (c) 2026. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter.service

import org.taktik.icure.domain.filter.Filter
import org.taktik.icure.domain.filter.VersionFiltering
import org.taktik.icure.entities.embed.Service

/**
 * Retrieves the ids of the [Service]s shared with the data owner [dataOwnerId], having at least one of
 * the tags in [tagCodes] (map of type to accepted codes), and optionally a [Service.valueDate] (or
 * [Service.openingDate] if the value date is null) between [startValueDate] and [endValueDate] (inclusive fuzzy dates).
 *
 * [filterVersion] controls how versions of a service (the same service id in multiple contacts) are handled:
 * - [VersionFiltering.ANY] (default when null): the service id is returned if any of its versions matches.
 * - [VersionFiltering.LATEST]: the service id is returned only if its latest version matches.
 */
interface ServiceByDataOwnerTagCodesFilter : Filter<String, Service> {
	val dataOwnerId: String
	val tagCodes: Map<String, Collection<String>>
	val startValueDate: Long?
	val endValueDate: Long?
	val filterVersion: VersionFiltering?
}
