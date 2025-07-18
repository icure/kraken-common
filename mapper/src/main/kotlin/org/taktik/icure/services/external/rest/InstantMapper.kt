/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import java.time.Instant

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
abstract class InstantMapper {
	fun map(instant: Instant): Long = instant.toEpochMilli()

	fun map(aLong: Long?): Instant = Instant.ofEpochMilli(aLong!!)

	fun map(aLong: Long): Instant = Instant.ofEpochMilli(aLong)
}
