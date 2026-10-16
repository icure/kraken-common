package org.taktik.icure.asynclogic.impl.customentities


abstract class AbstractCustomEntityDefinitionLogicContext(
	val helpers: Map<CustomEntityLogicContextHelper.Key<*>, Any>
) : CustomEntityDefinitionLogicContext {
	/**
	 * Get a helper required to be present in this context to perform a logic operation.
	 */
	@Suppress("UNCHECKED_CAST")
	final override fun <T : Any> requireHelper(key: CustomEntityLogicContextHelper.Key<T>): T =
		requireNotNull(helpers[key]) { key.errorMessageInvalid } as T
}
