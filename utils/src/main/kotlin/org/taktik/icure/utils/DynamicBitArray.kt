package org.taktik.icure.utils

import java.io.Serializable
import java.util.Base64
import kotlin.math.ceil
import kotlin.math.max

/**
 * This class represents a bit array that can grow when needed, namely when:
 * - A boolean operation is applied using a bigger array as the other argument.
 * - When setting a bit in the array for an index not included in the array size.
 *
 * When getting an element of the array outside the limit of the array, it will not grow but 0 will be returned.
 * Since the underlying representation uses a [UByteArray], the size will always be rounded up to the nearest byte.
 */
@OptIn(ExperimentalUnsignedTypes::class)
class DynamicBitArray private constructor(
	vector: UByteArray,
) : Serializable {

	var vector: UByteArray = vector
		private set

	val size: Int
		get() = vector.size * 8

	companion object {
		/**
		 * Factory method.
		 * @param size the size of the array to create.
		 * @return a new [DynamicBitArray] of the specified size populated by only 0s.
		 */
		fun bitVectorOfSize(size: Int) = DynamicBitArray(ByteArray(ceil(size / 8.0).toInt()) { 0b0 }.toUByteArray())

		/**
		 * Factory method.
		 * @param array an initializer [UByteArray].
		 * @return a new [DynamicBitArray] which contains the bits of the array passed as parameter.
		 */
		fun fromByteArray(array: UByteArray) = DynamicBitArray(array)

		/**
		 * Factory method.
		 * @param base64String a [String] in base64 format.
		 * @return a new [DynamicBitArray] which contains the bit encoded in base64 in the string passed as parameter.
		 */
		fun fromBase64String(base64String: String) = DynamicBitArray(Base64.getDecoder().decode(base64String).toUByteArray())
	}

	/**
	 * Performs an operation between two [DynamicBitArray] applying it byte by byte.
	 */
	private fun safeBitwiseOperation(other: DynamicBitArray, op: (UByte?, UByte?) -> UByte): DynamicBitArray {
		val maxSize = max(other.vector.size, vector.size)
		val result = ByteArray(maxSize) { 0b0 }.toUByteArray()
		for (i in (0 until maxSize)) {
			result[i] = op(vector.getOrNull(i), other.vector.getOrNull(i))
		}
		return fromByteArray(result)
	}

	/**
	 * Performs an AND boolean operation between two [DynamicBitArray].
	 * If the size of the two arrays differs, the smallest one will be padded with 0s to the left.
	 * @param other another [DynamicBitArray]
	 * @return a [DynamicBitArray] containing the result of the operation.
	 */
	infix fun and(other: DynamicBitArray): DynamicBitArray = safeBitwiseOperation(other) { first, second ->
		(first ?: 0u) and (second ?: 0u)
	}

	/**
	 * Performs an OR boolean operation between two [DynamicBitArray].
	 * If the size of the two arrays differs, the smallest one will be padded with 0s to the left.
	 * @param other another [DynamicBitArray]
	 * @return a [DynamicBitArray] containing the result of the operation.
	 */
	infix fun or(other: DynamicBitArray): DynamicBitArray = safeBitwiseOperation(other) { first, second ->
		(first ?: 0u) or (second ?: 0u)
	}

	/**
	 * Performs a XOR boolean operation between two [DynamicBitArray].
	 * If the size of the two arrays differs, the smallest one will be padded with 0s to the left.
	 * @param other another [DynamicBitArray]
	 * @return a [DynamicBitArray] containing the result of the operation.
	 */
	infix fun xor(other: DynamicBitArray): DynamicBitArray = safeBitwiseOperation(other) { first, second ->
		(first ?: 0u) xor (second ?: 0u)
	}

	/**
	 * Gets a bit from the array.
	 * @param bitAddress the position of the bit in the array. If it is greater than the array size, false will be returned.
	 * @return the value of the bit as boolean.
	 */
	operator fun get(bitAddress: Int): Boolean {
		val byteIndex = bitAddress / 8
		val bitOffset = bitAddress % 8
		return byteIndex < vector.size && vector[byteIndex].and(1.shl(bitOffset).toUByte()) > 0u
	}

	/**
	 * Sets a bit in the array at the specified index. If the index is greater than the size, than the size of the array
	 * will increase.
	 * @param bitAddress the position where to set the bit.
	 * @param v the bit value to set, as boolean.
	 */
	operator fun set(bitAddress: Int, v: Boolean) {
		val byteIndex = bitAddress / 8
		val bitOffset = bitAddress % 8
		if (byteIndex < vector.size) {
			val currentByte = vector[byteIndex]
			val mask = 1.shl(bitOffset).inv().toUByte()
			val setter = (1.takeIf { v } ?: 0).shl(bitOffset).toUByte()
			vector[byteIndex] = currentByte.and(mask).or(setter)
		} else {
			val padding = ByteArray((byteIndex - vector.size) + 1) { 0b0 }.toUByteArray()
			padding[byteIndex - vector.size] = (1.takeIf { v } ?: 0).shl(bitOffset).toUByte()
			vector = (vector + padding)
		}
	}

	/**
	 * @return a base64 representation of the bit array
	 */
	fun toBase64String(): String = Base64.getEncoder().encodeToString(vector.toByteArray())
}
