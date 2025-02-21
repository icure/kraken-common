package org.taktik.icure.services.external.rest.v1.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.taktik.icure.entities.CryptoActorStub
import org.taktik.icure.entities.CryptoActorStubWithType
import org.taktik.icure.services.external.rest.v1.dto.CryptoActorStubDto
import org.taktik.icure.services.external.rest.v1.dto.CryptoActorStubWithTypeDto
import org.taktik.icure.services.external.rest.v1.mapper.base.CodeStubMapper
import org.taktik.icure.services.external.rest.v1.mapper.base.PropertyStubMapper

@Mapper(
    componentModel = "spring",
    uses = [PropertyStubMapper::class, CodeStubMapper::class],
    injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
interface CryptoActorStubMapper {
    fun map(cryptoActorStub: CryptoActorStub): CryptoActorStubDto
    @Mappings(
        Mapping(target = "revHistory", ignore = true),
    )
    fun map(cryptoActorStubDto: CryptoActorStubDto): CryptoActorStub
    fun map(cryptoActorStubWithType: CryptoActorStubWithType): CryptoActorStubWithTypeDto
    fun map(cryptoActorStubWithTypeDto: CryptoActorStubWithTypeDto): CryptoActorStubWithType
}