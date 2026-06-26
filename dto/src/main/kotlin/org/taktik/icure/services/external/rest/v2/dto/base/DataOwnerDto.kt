package org.taktik.icure.services.external.rest.v2.dto.base

import org.taktik.icure.services.external.rest.v2.dto.PropertyStubDto
import org.taktik.icure.dto.annotations.filtering.ActiveField

/**
 * Interface for entities that own and control access to encrypted data in the iCure system.
 * Data owners (such as healthcare parties, patients, and devices) can grant access to their data through delegations.
 */
interface DataOwnerDto {
	@ActiveField val properties: Set<PropertyStubDto>
}
