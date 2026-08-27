package org.taktik.icure.services.external.rest.v2.mapper

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings
import org.springframework.stereotype.Service
import org.taktik.icure.entities.CryptoActorStub
import org.taktik.icure.entities.CryptoActorStubWithType
import org.taktik.icure.entities.DataOwnerType
import org.taktik.icure.entities.base.DataOwnerGroupLink
import org.taktik.icure.services.external.rest.ModelMappingVersionContext
import org.taktik.icure.services.external.rest.v1.mapper.base.CryptoActorMappingHelper
import org.taktik.icure.services.external.rest.v2.dto.CryptoActorStubDto
import org.taktik.icure.services.external.rest.v2.dto.CryptoActorStubWithTypeDto
import org.taktik.icure.services.external.rest.v2.dto.DataOwnerTypeDto
import org.taktik.icure.services.external.rest.v2.dto.base.DataOwnerGroupLinkDto
import org.taktik.icure.services.external.rest.v2.mapper.base.CodeStubV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.base.DataOwnerGroupLinkV2Mapper
import org.taktik.icure.services.external.rest.v2.mapper.base.PropertyStubV2Mapper

interface CryptoActorStubV2Mapper {
	fun map(
		cryptoActorStub: CryptoActorStub,
		modelMappingVersionContext: ModelMappingVersionContext,
	): CryptoActorStubDto

	fun map(cryptoActorStubDto: CryptoActorStubDto): CryptoActorStub
	fun map(
		cryptoActorStubWithType: CryptoActorStubWithType,
		modelMappingVersionContext: ModelMappingVersionContext,
	): CryptoActorStubWithTypeDto

	fun map(cryptoActorStubWithTypeDto: CryptoActorStubWithTypeDto): CryptoActorStubWithType
}

@Service
internal class CryptoActorStubV2MapperImpl(
	private val precomputedLinksMapper: CryptoActorStubMapperWithPrecomputedLinks,
	private val dataOwnerGroupLinkV2Mapper: DataOwnerGroupLinkV2Mapper,
) : CryptoActorStubV2Mapper {
	override fun map(
		cryptoActorStub: CryptoActorStub,
		modelMappingVersionContext: ModelMappingVersionContext,
	): CryptoActorStubDto {
		val (parentId, dataOwnerGroups) = CryptoActorMappingHelper.mapParentIdAndDataOwnerGroupLinks(
			cryptoActorStub,
			dataOwnerGroupLinkV2Mapper,
			modelMappingVersionContext,
		)
		return precomputedLinksMapper.map(cryptoActorStub, parentId, dataOwnerGroups)
	}

	override fun map(cryptoActorStubDto: CryptoActorStubDto): CryptoActorStub {
		// Dumb 1:1 copy: whether a link is admin-type or not is now intrinsic to its target, not declared here, so
		// there is nothing to fold/collapse on the way in. Validation and storage-shape normalization happen at the
		// logic layer.
		return precomputedLinksMapper.map(
			cryptoActorStubDto,
			cryptoActorStubDto.parentId,
			cryptoActorStubDto.dataOwnerGroups.map(dataOwnerGroupLinkV2Mapper::map),
		)
	}

	override fun map(
		cryptoActorStubWithType: CryptoActorStubWithType,
		modelMappingVersionContext: ModelMappingVersionContext,
	): CryptoActorStubWithTypeDto =
		CryptoActorStubWithTypeDto(
			type = DataOwnerTypeDto.valueOf(cryptoActorStubWithType.type.name),
			stub = map(cryptoActorStubWithType.stub, modelMappingVersionContext),
		)

	override fun map(cryptoActorStubWithTypeDto: CryptoActorStubWithTypeDto): CryptoActorStubWithType =
		CryptoActorStubWithType(
			type = DataOwnerType.valueOf(cryptoActorStubWithTypeDto.type.name),
			stub = map(cryptoActorStubWithTypeDto.stub),
		)
}

@Mapper(
	componentModel = "spring",
	uses = [PropertyStubV2Mapper::class, CodeStubV2Mapper::class],
	injectionStrategy = InjectionStrategy.CONSTRUCTOR,
)
internal interface CryptoActorStubMapperWithPrecomputedLinks {
	@Mappings(
		Mapping(target = "parentId", expression = """kotlin(parentId)"""),
		Mapping(target = "dataOwnerGroups", expression = """kotlin(dataOwnerGroups)"""),
	)
	fun map(
		cryptoActorStub: CryptoActorStub,
		parentId: String?,
		dataOwnerGroups: List<DataOwnerGroupLinkDto>,
	): CryptoActorStubDto

	@Mappings(
		Mapping(target = "parentId", expression = """kotlin(parentId)"""),
		Mapping(target = "dataOwnerGroups", expression = """kotlin(dataOwnerGroups)"""),
	)
	fun map(
		cryptoActorStubDto: CryptoActorStubDto,
		parentId: String?,
		dataOwnerGroups: List<DataOwnerGroupLink>,
	): CryptoActorStub
}
