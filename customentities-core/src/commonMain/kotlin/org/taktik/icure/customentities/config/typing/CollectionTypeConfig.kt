package org.taktik.icure.customentities.config.typing

import kotlinx.serialization.Serializable

@Serializable
sealed interface CollectionTypeConfig : GenericTypeConfig {
	val elementType: GenericTypeConfig
}