package org.taktik.icure.services.external.rest.v2.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.CryptoActorStub
import org.taktik.icure.entities.CryptoActorStubWithType
import org.taktik.icure.services.external.rest.v2.dto.CryptoActorStubDto
import org.taktik.icure.services.external.rest.v2.dto.CryptoActorStubWithTypeDto
import org.taktik.icure.services.external.rest.v2.mapper.base.CodeStubV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.base.PropertyStubV2Mapper

@Mapper(
    componentModel = "spring",
    uses = [PropertyStubV2Mapper::class, CodeStubV2Mapper::class],
    injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
interface CryptoActorStubV2Mapper {
    fun map(cryptoActorStub: CryptoActorStub): CryptoActorStubDto
    @Mappings(
        Mapping(target = "revHistory", ignore = true),
    )
    fun map(cryptoActorStubDto: CryptoActorStubDto): CryptoActorStub
    fun map(cryptoActorStubWithType: CryptoActorStubWithType): CryptoActorStubWithTypeDto
    fun map(cryptoActorStubWithTypeDto: CryptoActorStubWithTypeDto): CryptoActorStubWithType
}