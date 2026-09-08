package org.taktik.icure.entities.dao

sealed class KeyComponent : Comparable<KeyComponent> {
	abstract val value: Any?

	data class Int(override val value: kotlin.Int?) : KeyComponent() {
		override fun compareTo(other: KeyComponent): kotlin.Int = when {
			other !is Int -> throw IllegalStateException("Cannot compare ${this::class.simpleName} with ${other::class.simpleName}")
			value != null && other.value != null -> value.compareTo(other.value)
			value == null && other.value == null -> 0
			value != null -> 1
			else -> -1
		}
	}
	data class Long(override val value: kotlin.Long?) : KeyComponent() {
		override fun compareTo(other: KeyComponent): kotlin.Int = when {
			other !is Long -> throw IllegalStateException("Cannot compare ${this::class.simpleName} with ${other::class.simpleName}")
			value != null && other.value != null -> value.compareTo(other.value)
			value == null && other.value == null -> 0
			value != null -> 1
			else -> -1
		}
	}
	data class Double(override val value: kotlin.Double?) : KeyComponent() {
		override fun compareTo(other: KeyComponent): kotlin.Int = when {
			other !is Double -> throw IllegalStateException("Cannot compare ${this::class.simpleName} with ${other::class.simpleName}")
			value != null && other.value != null -> value.compareTo(other.value)
			value == null && other.value == null -> 0
			value != null -> 1
			else -> -1
		}
	}
	data class Boolean(override val value: kotlin.Boolean?) : KeyComponent() {
		override fun compareTo(other: KeyComponent): kotlin.Int = when {
			other !is Boolean -> throw IllegalStateException("Cannot compare ${this::class.simpleName} with ${other::class.simpleName}")
			value != null && other.value != null -> value.compareTo(other.value)
			value == null && other.value == null -> 0
			value != null -> 1
			else -> -1
		}
	}
	data class String(override val value: kotlin.String?) : KeyComponent() {
		override fun compareTo(other: KeyComponent): kotlin.Int = when {
			other !is String -> throw IllegalStateException("Cannot compare ${this::class.simpleName} with ${other::class.simpleName}")
			value != null && other.value != null -> value.compareTo(other.value)
			value == null && other.value == null -> 0
			value != null -> 1
			else -> -1
		}
	}

}