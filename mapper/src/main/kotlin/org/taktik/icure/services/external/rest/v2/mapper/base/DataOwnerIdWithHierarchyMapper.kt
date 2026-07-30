package org.taktik.icure.services.external.rest.v2.mapper.base

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.base.DataOwnerIdWithHierarchy
import org.taktik.icure.services.external.rest.v2.dto.base.DataOwnerIdWithHierarchyDto

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
interface DataOwnerIdWithHierarchyV2Mapper {
	fun map(dataOwnerIdWithHierarchyDto: DataOwnerIdWithHierarchyDto): DataOwnerIdWithHierarchy

	fun map(dataOwnerIdWithHierarchy: DataOwnerIdWithHierarchy): DataOwnerIdWithHierarchyDto
}
