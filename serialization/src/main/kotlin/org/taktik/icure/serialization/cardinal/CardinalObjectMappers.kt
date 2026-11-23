package org.taktik.icure.serialization.cardinal

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.taktik.icure.serialization.IcureDomainObjectMapper.registerMultiplatformSupportModules

/**
 * The [ObjectMapper]s used by kraken to read and write the dtos over http, so that every service exposing the dtos
 * serializes them the same way.
 */
object CardinalObjectMappers {
	/**
	 * Mapper for clients that do not declare a cardinal version (legacy iCure SDK): serializes all non-null fields.
	 */
	fun legacyObjectMapper(): ObjectMapper =
		ObjectMapper()
			.registerModule(
				KotlinModule
					.Builder()
					.configure(KotlinFeature.NullIsSameAsDefault, true)
					.build(),
			).apply {
				setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
			}.registerMultiplatformSupportModules()

	/**
	 * Base configuration of the mappers of [CardinalMappers], on which the mix-ins of each version are set.
	 */
	fun cardinalObjectMapper(): ObjectMapper =
		ObjectMapper()
			.registerModule(
				KotlinModule
					.Builder()
					.configure(KotlinFeature.NullIsSameAsDefault, true)
					.build(),
			).apply {
				// Single-arg setDefaultPropertyInclusion(NON_EMPTY) sets both valueInclusion AND contentInclusion to
				// NON_EMPTY. The latter would elide individual Map entries/List elements whose own value is empty (e.g.
				// dropping "x" from a Map<String, Set<T>> if the set for "x" is empty), even though the entry itself is
				// meaningful. We only want to omit a property entirely when it is empty, not filter within its contents.
				setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY, JsonInclude.Include.ALWAYS))
			}.registerMultiplatformSupportModules()

	/**
	 * The versioned mappers built by [provider] on top of [cardinalObjectMapper].
	 */
	fun cardinalMappers(provider: CardinalMappersProvider): CardinalMappers = provider.getCardinalMappers(::cardinalObjectMapper)

	/**
	 * Mapper for request bodies, same for every client.
	 */
	fun requestObjectMapper(): ObjectMapper =
		ObjectMapper()
			.registerModule(
				KotlinModule
					.Builder()
					.configure(KotlinFeature.NullIsSameAsDefault, true)
					// TODO : may have significant performance impact but provides better error reporting (400 instead of 500), disable in case of issues.
					.configure(KotlinFeature.StrictNullChecks, true)
					.build(),
			).registerMultiplatformSupportModules()
}
