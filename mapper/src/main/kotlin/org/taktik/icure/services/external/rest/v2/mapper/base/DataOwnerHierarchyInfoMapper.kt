package org.taktik.icure.services.external.rest.v2.mapper.base

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.base.DataOwnerHierarchyInfo
import org.taktik.icure.services.external.rest.v2.dto.base.DataOwnerHierarchyInfoDto

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface DataOwnerHierarchyInfoV2Mapper {
	fun map(dataOwnerHierarchyInfoDto: DataOwnerHierarchyInfoDto): DataOwnerHierarchyInfo

	fun map(dataOwnerHierarchyInfo: DataOwnerHierarchyInfo): DataOwnerHierarchyInfoDto

	fun map(node: DataOwnerHierarchyInfoDto.HierarchyNode): DataOwnerHierarchyInfo.HierarchyNode

	fun map(node: DataOwnerHierarchyInfo.HierarchyNode): DataOwnerHierarchyInfoDto.HierarchyNode
}
