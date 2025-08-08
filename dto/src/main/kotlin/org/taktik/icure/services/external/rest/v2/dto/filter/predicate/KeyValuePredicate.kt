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
package org.taktik.icure.services.external.rest.v2.dto.filter.predicate

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.swagger.v3.oas.annotations.media.Schema
import org.taktik.icure.handlers.JsonPolymorphismRoot
import org.taktik.icure.services.external.rest.v2.dto.specializations.AnyPrimitive

@JsonPolymorphismRoot(org.taktik.icure.services.external.rest.v2.dto.filter.predicate.Predicate::class)
@JsonDeserialize(using = JsonDeserializer.None::class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class KeyValuePredicate(
	val key: String? = null,
	val operator: Operator? = null,
	val value: AnyPrimitive? = null,
) : Predicate

enum class Operator(
	val code: String,
) {
	@Schema(defaultValue = "==")
	EQUAL("=="),

	@Schema(defaultValue = "!=")
	NOTEQUAL("!="),

	@Schema(defaultValue = ">")
	GREATERTHAN(">"),

	@Schema(defaultValue = "<")
	SMALLERTHAN("<"),

	@Schema(defaultValue = ">=")
	GREATERTHANOREQUAL(">="),

	@Schema(defaultValue = "<=")
	SMALLERTHANOREQUAL("<="),

	@Schema(defaultValue = "%=")
	LIKE("%="),

	@Schema(defaultValue = "%%=")
	ILIKE("%%="),
	;

	override fun toString(): String = code
}
