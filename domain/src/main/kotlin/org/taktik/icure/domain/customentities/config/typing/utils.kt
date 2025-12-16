package org.taktik.icure.domain.customentities.config.typing

import org.taktik.icure.entities.RawJson
import org.taktik.icure.domain.customentities.util.ResolutionPath
import org.taktik.icure.exceptions.IllegalEntityException

/**
 * If [value] is [RawJson.JsonNull] requires that [nullable] is true and returns [RawJson.JsonNull] (fails if nullable is false).
 * Else executes the block and returns its value
 */
internal inline fun validatingAndIgnoringNullForStore(
	path: ResolutionPath,
	value: RawJson,
	nullable: Boolean,
	block: () -> RawJson
): RawJson =
	if (value === RawJson.JsonNull) {
		require(nullable) {
			"$path: invalid value `null`"
		}
		value
	} else {
		block()
	}

internal inline fun validatingAndIgnoringNullForRead(
	path: ResolutionPath,
	value: RawJson,
	nullable: Boolean,
	block: () -> RawJson
): RawJson =
	if (value === RawJson.JsonNull) {
		if (!nullable) throw IllegalEntityException("$path: invalid value `null`")
		value
	} else {
		block()
	}