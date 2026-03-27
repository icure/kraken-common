package org.taktik.icure.customentities.config

interface ExtendableRootEntitiesConfiguration<T : Any> {
	val patient: T?
	// TODO others
}

val <T : Any> ExtendableRootEntitiesConfiguration<T>.allDefined get(): List<Pair<String, T>> = listOfNotNull(
	patient?.let { "Patient" to it },
	// TODO others
)

