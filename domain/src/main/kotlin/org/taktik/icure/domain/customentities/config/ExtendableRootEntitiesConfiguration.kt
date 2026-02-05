package org.taktik.icure.domain.customentities.config

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
interface ExtendableRootEntitiesConfiguration<T : Any> {
	val patient: T?
	// TODO others
}

val <T : Any> ExtendableRootEntitiesConfiguration<T>.allDefined get(): List<Pair<String, T>> = listOfNotNull(
	patient?.let { "Patient" to it },
	// TODO others
)