package org.taktik.icure.asynclogic.base

import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.validation.aspect.Fixer

abstract class AutoFixableLogic<E : Identifiable<String>>(private val fixer: Fixer) {
	/**
	 * Applies autofix on a [doc] of type [E], automatically filling the null parameters according to the auto-fixing
	 * configuration provided in the entity class and then applies to it the function [next].
	 * Each class parameter that is annotated with an annotation that has the autofix parameter, will be set to a
	 * default value based on the strategy defined in the [fixer].
	 *
	 * @param doc an [E] to autofix.
	 * @param isCreate true if the fix is for a create operation or not.
	 * @param next a suspend function that takes as input the auto-fixed document [E] and returns an [R].
	 * @return the output [R] of next.
	 */
	protected suspend fun <R> fix(doc: E, isCreate: Boolean, next: suspend (doc: E) -> R): R = next(fixer.fix(doc, isCreate))

	/**
	 * Applies autofix on a [doc] of type [E], automatically filling the null parameters according to the auto-fixing
	 * configuration provided in the entity class.
	 * Each class parameter that is annotated with an annotation that has the autofix parameter, will be set to a
	 * default value based on the strategy defined in the [fixer].
	 *
	 * @param doc an [E] to autofix.
	 * @param isCreate true if the fix is for a create operation or not.
	 * @return an auto-fixed [E].
	 */
	protected suspend fun fix(doc: E, isCreate: Boolean): E = fixer.fix(doc, isCreate)
}
