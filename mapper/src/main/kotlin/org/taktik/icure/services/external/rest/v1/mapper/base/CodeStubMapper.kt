/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v1.mapper.base

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.entities.base.CodeStub
import org.taktik.icure.services.external.rest.v1.dto.base.CodeStubDto
@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
abstract class CodeStubMapper {
	fun map(codeStubDto: CodeStubDto?): CodeStub? {
		return codeStubDto?.let {
			CodeStub(
				id = it.id ?: "${it.type}|${it.code}|${it.version}",
				type = it.type,
				code = it.code,
				version = it.version,
				context = it.context,
				contextLabel = it.contextLabel,
				label = it.label
			)
		}
	}

	fun mapNotNull(codeStubDto: CodeStubDto): CodeStub {
		return codeStubDto.let {
			CodeStub(
				id = it.id ?: "${it.type}|${it.code}|${it.version}",
				type = it.type,
				code = it.code,
				version = it.version,
				context = it.context,
				contextLabel = it.contextLabel,
				label = it.label
			)
		}
	}
	abstract fun map(codeStub: CodeStub): CodeStubDto
}
