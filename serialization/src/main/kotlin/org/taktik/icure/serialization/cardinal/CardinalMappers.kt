package org.taktik.icure.serialization.cardinal

import com.fasterxml.jackson.databind.ObjectMapper
import org.taktik.icure.entities.utils.SemanticVersion
import java.util.TreeMap

/**
 * Mappers for Cardinal SDK, selectively omit some data based on the version of cardinal sdk declared in use by the
 * user.
 *
 * - Version < 2.0.0 or not declared:
 *   - Include all data, including legacy autofilled fields (this is the legacy mapper, see
 *     [CardinalObjectMappers.legacyObjectMapper], it is not part of the [CardinalMappers])
 * - 2.0.0 <= version < 2.4.0 :
 *   - Does not include DataAttachment.storedDataSize and Document.mainAttachmentStoredDataSize, as they are filled
 *     in automatically by the server regardless of the CardinalSDK version.
 * - 2.4.0 <= version, currently:
 *   - Everything is included except for legacy autofilled fields by default
 *
 * # Legacy fields
 *
 * Many fields of the data model are considered part of the legacy data model.
 *
 * Most of the fields from the legacy data model were never used by cardinal or kraken, so they always have the
 * default null/empty value, therefore, when we serialize the dto for Cardinal there is no entry for those fields.
 * However, if the user was using those fields and gave explicit values to them we will include them in the
 * serialized DTO, and that would cause an error when using new (standard) versions of cardinal.
 *
 * Since no one ever used explicitly the legacy fields we haven't incurred in these problems, however, some fields
 * of the legacy data model were given a value automatically by older versions o of Cardinal and/or older versions
 * of Kraken.
 * We are explicitly ignoring these fields, regardless of their value, when the user declares they are using
 * Cardinal version 2.0.0 or above, unless additional flags are used to request explicitly that they are included.
 *
 * This supports transition without migration of data for cardinal users to 2.0.0.
 * However, if the user ever explicitly set a value for one of these autofilled fields this would cause silent
 * partial loss of information, since the Cardinal SDK would not reject the data.
 * Again, this is not a problem for cardinal users since no user of cardinal < 2.0.0 ever used those fields, and
 * Cardinal >= 2.0.0 does not expose them, but it would be a problem if users of the legacy iCure SDK migrate to
 * cardinal without using the proper configuration.
 *
 * ## Example
 *
 * Health element has:
 * - MedicalLocationId, a legacy field that has been removed from Cardinal 2.0.0 and was never autofilled
 * - Status, a legacy field that has been removed from Cardinal 2.0.0 but was autofilled to 0
 *
 * MedicalLocationId will not be included in the serialized DTO if null, BUT will always be included if not null.
 * If for some reason there is data that did set the MedicalLocationId to a value the CardinalSDK would throw an
 * error when parsing the health element. This protects against unintentional loss of data
 *
 * Status instead, will not be included, regardless of value, otherwise users of Cardinal would get unexpected
 * errors even when they did not explicitly set the status value. However, if a Cardinal user had explicitly set
 * a Status value to let's say 2, the status would be silently reset to 0 when the user updates the HealthElement
 * through Cardinal 2.0.0
 *
 * This is a problem only for users that moved from the legacy iCure SDK. These users should instead use the
 * legacy-support version of the cardinal SDK and use a header to request that legacy autofilled fields are
 * included.
 */
interface CardinalMappers {
	/**
	 * Mappers associated by minimum version of cardinal that can use that mapper.
	 */
	val byMinVersion: TreeMap<SemanticVersion, MapperConfig>

	data class MapperConfig(
		/**
		 * A mapper configuration that excludes all legacy fields.
		 */
		val default: ObjectMapper,
		/**
		 * A mapper configuration equivalent to [default] excepts it also includes the legacy fields
		 */
		val includingLegacyFields: ObjectMapper,
	)

	/**
	 * Get the cardinal mapper configuration to use for the provided [semanticVersion], or null if there is no
	 * mapper supporting that as minimum version.
	 */
	fun getForVersion(semanticVersion: SemanticVersion, includingLegacyFields: Boolean = false): ObjectMapper? =
		byMinVersion.floorEntry(semanticVersion)?.value?.let {
			if (includingLegacyFields) it.includingLegacyFields else it.default
		}
}

/**
 * Builds the [CardinalMappers]. The implementation (`CardinalMappersProviderImpl`) is generated from the filtering
 * annotations of the dtos, in the `org.taktik.icure.serialization.cardinal.filters` package.
 */
abstract class CardinalMappersProvider {
	/**
	 * @param mapperProvider provides a new, not shared, [ObjectMapper] for each version and mode, on which the mix-ins
	 * are set. See [CardinalObjectMappers.cardinalObjectMapper] for the base configuration used by kraken.
	 */
	abstract fun getCardinalMappers(mapperProvider: () -> ObjectMapper): CardinalMappers

	protected fun TreeMap<SemanticVersion, CardinalMappers.MapperConfig>.putConfig(
		version: String,
		provider: CardinalMixInProvider,
		mapperProvider: () -> ObjectMapper,
	) {
		put(
			SemanticVersion(version),
			CardinalMappers.MapperConfig(
				default = mapperProvider().setMixIns(provider.getMixInConfiguration()),
				includingLegacyFields = mapperProvider().setMixIns(provider.getLegacyMixInConfiguration()),
			),
		)
	}
}

/**
 * Mix-ins (target class to mix-in class) to apply for a cardinal version, generated from the filtering annotations
 * of the dtos.
 */
interface CardinalMixInProvider {
	fun getMixInConfiguration(): Map<Class<*>, Class<*>>

	fun getLegacyMixInConfiguration(): Map<Class<*>, Class<*>>
}
