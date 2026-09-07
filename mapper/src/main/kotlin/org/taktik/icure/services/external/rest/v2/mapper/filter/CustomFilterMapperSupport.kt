package org.taktik.icure.services.external.rest.v2.mapper.filter

import org.taktik.icure.entities.dao.KeyComponent
import org.taktik.icure.services.external.rest.v2.dto.dao.KeyComponentDto
import org.taktik.icure.services.external.rest.v2.mapper.dao.KeyComponentV2Mapper

/**
 * Shared helpers used by the per-entity [org.taktik.icure.services.external.rest.v2.dto.filter.CustomFilterDto]
 * mappers to translate their [KeyComponentDto] collections.
 */
fun KeyComponentV2Mapper.mapStartKey(startKey: List<KeyComponentDto>?): List<KeyComponent<*>>? =
	startKey?.map { map(it) }

fun KeyComponentV2Mapper.mapKeyComponents(keyComponents: List<List<KeyComponentDto>>): List<List<KeyComponent<*>>> =
	keyComponents.map { row -> row.map { map(it) } }
