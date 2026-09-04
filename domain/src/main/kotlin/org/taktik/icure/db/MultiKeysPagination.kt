package org.taktik.icure.db

/**
 * [multiQueryComponents] is a list of N elements, each one containing all the different values for the 1 .. Nth component
 * of a key asked by the user.
 * This method computes the cross-product of the components to generate all the possible keys.
 *
 * @param startKey if not null, omits all the keys until startKey is found. The components in [multiQueryComponents] are
 * not sorted: for each component of the start key, it will get the first element of the corresponding component in
 * [multiQueryComponents] that has the same value, then it will start the generation from there.
 */
fun <T> crossProductKeysAfterStart(
	multiQueryComponents: List<List<T>>,
	startKey: List<T>?
): Sequence<List<T>> {
	val n = multiQueryComponents.size
	if (n == 0 || multiQueryComponents.any { it.isEmpty() }) {
		return emptySequence()
	}

	val start = IntArray(n)
	if (startKey != null) {
		require(startKey.size >= n) { "startKey size must be greater than components size" }
		for (i in 0 until n) {
			val k = multiQueryComponents[i].indexOf(startKey[i])
			require(k >= 0) { "value ${startKey[i]} is not present in lists[$i]" }
			start[i] = k
		}
	}

	return Sequence {
		object : Iterator<List<T>> {
			private val cursor = start.copyOf()
			private var hasMore = true

			override fun hasNext() = hasMore

			override fun next(): List<T> {
				if (!hasMore) throw NoSuchElementException()
				val v = ArrayList<T>(n)
				for (i in 0 until n) {
					v.add(multiQueryComponents[i][cursor[i]])
				}
				hasMore = advance()
				return v
			}

			/** Mixed-radix increment; returns false when it wraps past the last vector. */
			private fun advance(): Boolean {
				var i = n - 1
				while (i >= 0) {
					if (++cursor[i] < multiQueryComponents[i].size) return true
					cursor[i] = 0
					i--
				}
				return false
			}
		}
	}
}