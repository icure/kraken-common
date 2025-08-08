package org.taktik.icure.annotations.customizers

import io.swagger.v3.oas.models.parameters.Parameter
import org.springdoc.core.customizers.ParameterCustomizer
import org.springframework.core.MethodParameter
import org.taktik.icure.annotations.entities.ContentValue

class ContentValueParameterCustomizer : ParameterCustomizer {
	override fun customize(
		parameterModel: Parameter?,
		methodParameter: MethodParameter?,
	): Parameter? = methodParameter?.parameter?.getAnnotation(ContentValue::class.java)?.let { annotation ->
		parameterModel?.example = annotation.contentValue.value()
		parameterModel
	}
}
