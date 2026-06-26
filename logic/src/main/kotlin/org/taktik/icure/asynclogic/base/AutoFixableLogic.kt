package org.taktik.icure.asynclogic.base

import org.taktik.couchdb.entity.Revisionable
import org.taktik.icure.validation.EntityIdValidation
import org.taktik.icure.validation.aspect.Fixer

abstract class AutoFixableLogic<E : Revisionable<String>>(private val fixer: Fixer) {

	protected open suspend fun shouldCheckIdValidity(): Boolean = true

	protected suspend fun checkValidityForCreation(entity: E) {
		if (entity.rev != null) {
			throw IllegalArgumentException("An entity with a non-null revision is not valid for creation")
		}
		if (shouldCheckIdValidity()) {
			EntityIdValidation.checkValidForCreation(entity.id)
		}
	}

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
