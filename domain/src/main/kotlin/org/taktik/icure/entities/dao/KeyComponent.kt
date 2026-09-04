package org.taktik.icure.entities.dao

sealed class KeyComponent<T> : Comparable<KeyComponent<T>> {
	abstract val value: T?

	protected abstract fun compareValue(thisValue: T, otherValue: T): kotlin.Int

	override fun compareTo(other: KeyComponent<T>): kotlin.Int {
		if (this::class != other::class) {
			throw IllegalStateException("Cannot compare ${this::class.simpleName} with ${other::class.simpleName}")
		}
		val thisValue = value
		val otherValue = other.value
		return when {
			thisValue != null && otherValue != null -> compareValue(thisValue, otherValue)
			thisValue == null && otherValue == null -> 0
			thisValue != null -> 1
			else -> -1
		}
	}

	data class Int(override val value: kotlin.Int?) : KeyComponent<kotlin.Int>() {
		override fun compareValue(thisValue: kotlin.Int, otherValue: kotlin.Int): kotlin.Int = thisValue.compareTo(otherValue)
	}
	data class Float(override val value: kotlin.Float?) : KeyComponent<kotlin.Float>() {
		override fun compareValue(thisValue: kotlin.Float, otherValue: kotlin.Float): kotlin.Int = thisValue.compareTo(otherValue)
	}
	data class Double(override val value: kotlin.Double?) : KeyComponent<kotlin.Double>() {
		override fun compareValue(thisValue: kotlin.Double, otherValue: kotlin.Double): kotlin.Int = thisValue.compareTo(otherValue)
	}
	data class Boolean(override val value: kotlin.Boolean?) : KeyComponent<kotlin.Boolean>() {
		override fun compareValue(thisValue: kotlin.Boolean, otherValue: kotlin.Boolean): kotlin.Int = thisValue.compareTo(otherValue)
	}
	data class String(override val value: kotlin.String?) : KeyComponent<kotlin.String>() {
		override fun compareValue(thisValue: kotlin.String, otherValue: kotlin.String): kotlin.Int = thisValue.compareTo(otherValue)
	}

}