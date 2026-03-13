/*
 *  iCure Data Stack. Copyright (c) 2020 Taktik SA
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public
 *     License along with this program.  If not, see
 *     <https://www.gnu.org/licenses/>.
 */
package org.taktik.icure.services.external.rest.v2.dto.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.services.external.rest.v2.dto.base.CodeStubDto
import java.io.Serializable

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
/**
 * Represents a medicinal product with its intended and actually delivered codes and names.
 */
data class MedicinalproductDto(
	/** The list of coded identifiers for the intended medicinal product. */
	val intendedcds: List<CodeStubDto> = emptyList(),
	/** The list of coded identifiers for the actually delivered medicinal product. */
	val deliveredcds: List<CodeStubDto> = emptyList(),
	/** The name of the intended medicinal product. */
	val intendedname: String? = null,
	/** The name of the actually delivered medicinal product. */
	val deliveredname: String? = null,
	/** The product identifier. */
	val productId: String? = null,
) : Serializable
