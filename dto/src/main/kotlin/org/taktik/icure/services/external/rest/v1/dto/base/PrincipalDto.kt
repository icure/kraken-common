/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.base

import org.taktik.icure.services.external.rest.v1.dto.PropertyStubDto
import org.taktik.icure.services.external.rest.v1.dto.security.PermissionDto

interface PrincipalDto : IdentifiableDto<String>, NamedDto {
	val properties: Set<PropertyStubDto>
}
