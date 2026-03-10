package org.taktik.icure.services.external.rest.v2.dto.base

import org.taktik.icure.services.external.rest.v2.dto.PropertyStubDto

/**
 * Interface for entities that own and control access to encrypted data in the iCure system.
 * Data owners (such as healthcare parties, patients, and devices) can grant access to their data through delegations.
 */
interface DataOwnerDto {
	val properties: Set<PropertyStubDto>
}
