package org.taktik.icure.services.external.rest.v1.mapper.filter.predicate

import org.mapstruct.InjectionStrategy
import org.mapstruct.Mapper
import org.taktik.icure.services.external.rest.v1.dto.filter.predicate.AlwaysPredicate
import org.taktik.icure.services.external.rest.v1.dto.filter.predicate.AndPredicate
import org.taktik.icure.services.external.rest.v1.dto.filter.predicate.KeyValuePredicate
import org.taktik.icure.services.external.rest.v1.dto.filter.predicate.NotPredicate
import org.taktik.icure.services.external.rest.v1.dto.filter.predicate.OrPredicate
import org.taktik.icure.services.external.rest.v1.dto.filter.predicate.Predicate

@Mapper(componentModel = "spring", uses = [], injectionStrategy = InjectionStrategy.CONSTRUCTOR)
abstract class PredicateMapper {
	abstract fun map(predicateDto: AndPredicate): org.taktik.icure.domain.filter.impl.predicate.AndPredicate
	abstract fun map(predicateDto: OrPredicate): org.taktik.icure.domain.filter.impl.predicate.OrPredicate
	abstract fun map(predicateDto: NotPredicate): org.taktik.icure.domain.filter.impl.predicate.NotPredicate
	abstract fun map(predicateDto: AlwaysPredicate): org.taktik.icure.domain.filter.impl.predicate.AlwaysPredicate
	abstract fun map(predicateDto: KeyValuePredicate): org.taktik.icure.domain.filter.impl.predicate.KeyValuePredicate
	fun map(predicateDto: Predicate): org.taktik.icure.domain.filter.predicate.Predicate = when (predicateDto) {
		is AndPredicate -> map(predicateDto)
		is OrPredicate -> map(predicateDto)
		is NotPredicate -> map(predicateDto)
		is AlwaysPredicate -> map(predicateDto)
		is KeyValuePredicate -> map(predicateDto)
		else -> throw IllegalArgumentException("Unsupported filter class")
	}

	abstract fun map(predicate: org.taktik.icure.domain.filter.impl.predicate.AndPredicate): AndPredicate
	abstract fun map(predicate: org.taktik.icure.domain.filter.impl.predicate.OrPredicate): OrPredicate
	abstract fun map(predicate: org.taktik.icure.domain.filter.impl.predicate.NotPredicate): NotPredicate
	abstract fun map(predicate: org.taktik.icure.domain.filter.impl.predicate.AlwaysPredicate): AlwaysPredicate
	abstract fun map(predicate: org.taktik.icure.domain.filter.impl.predicate.KeyValuePredicate): KeyValuePredicate
	fun map(predicate: org.taktik.icure.domain.filter.predicate.Predicate): Predicate = when (predicate) {
		is org.taktik.icure.domain.filter.impl.predicate.AndPredicate -> map(predicate)
		is org.taktik.icure.domain.filter.impl.predicate.OrPredicate -> map(predicate)
		is org.taktik.icure.domain.filter.impl.predicate.NotPredicate -> map(predicate)
		is org.taktik.icure.domain.filter.impl.predicate.AlwaysPredicate -> map(predicate)
		is org.taktik.icure.domain.filter.impl.predicate.KeyValuePredicate -> map(predicate)
		else -> throw IllegalArgumentException("Unsupported filter class")
	}
}
