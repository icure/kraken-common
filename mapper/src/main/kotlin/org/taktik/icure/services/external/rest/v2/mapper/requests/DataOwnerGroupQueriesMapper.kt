package org.taktik.icure.services.external.rest.v2.mapper.requests

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.requests.DataOwnerPublicKeys
import org.taktik.icure.entities.requests.LinkedDataOwner
import org.taktik.icure.entities.requests.PublicKeyInfo
import org.taktik.icure.services.external.rest.v2.dto.requests.DataOwnerPublicKeysDto
import org.taktik.icure.services.external.rest.v2.dto.requests.LinkedDataOwnerDto
import org.taktik.icure.services.external.rest.v2.dto.requests.PublicKeyInfoDto

@Mapper(
	componentModel = "spring",
	uses = [],
	injectionStrategy = InjectionStrategy.CONSTRUCTOR,
)
interface LinkedDataOwnerV2Mapper {
	fun map(linkedDataOwnerDto: LinkedDataOwnerDto): LinkedDataOwner

	fun map(linkedDataOwner: LinkedDataOwner): LinkedDataOwnerDto
}

@Mapper(
	componentModel = "spring",
	uses = [],
	injectionStrategy = InjectionStrategy.CONSTRUCTOR,
)
interface DataOwnerPublicKeysV2Mapper {
	fun map(dataOwnerPublicKeysDto: DataOwnerPublicKeysDto): DataOwnerPublicKeys

	fun map(dataOwnerPublicKeys: DataOwnerPublicKeys): DataOwnerPublicKeysDto

	fun map(publicKeyInfoDto: PublicKeyInfoDto): PublicKeyInfo

	fun map(publicKeyInfo: PublicKeyInfo): PublicKeyInfoDto
}
