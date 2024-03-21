/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper.base

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.couchdb.entity.Security
import org.taktik.icure.services.external.rest.v1.dto.base.RightDto
import org.taktik.icure.services.external.rest.v1.dto.base.SecurityDto
@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface SecurityMapper {
	fun map(securityDto: SecurityDto): Security
	fun map(security: Security): SecurityDto
	fun map(securityDto: RightDto): Security.Right
	fun map(security: Security.Right): RightDto
}
