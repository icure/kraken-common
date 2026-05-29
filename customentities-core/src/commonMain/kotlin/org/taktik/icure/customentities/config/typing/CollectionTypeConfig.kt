package org.taktik.icure.customentities.config.typing

sealed interface CollectionTypeConfig : GenericTypeConfig {
	val elementType: GenericTypeConfig
}