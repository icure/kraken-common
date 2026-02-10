package org.taktik.icure.domain.customentities.config

interface ExtendableRootEntitiesConfiguration<T : Any> {
	val patient: T?
	// TODO others
}

val <T : Any> ExtendableRootEntitiesConfiguration<T>.allDefined get(): List<Pair<ExtendableEntityName, T>> = listOfNotNull(
	patient?.let { ExtendableEntityName.Patient to it },
	// TODO others
)

