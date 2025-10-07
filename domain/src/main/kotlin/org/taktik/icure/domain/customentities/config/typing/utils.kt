package org.taktik.icure.domain.customentities.config.typing

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import org.taktik.icure.domain.customentities.util.ResolutionPath
import org.taktik.icure.exceptions.IllegalEntityException

/**
 * If [value] is [NullNode] requires that [nullable] is true and returns [NullNode] (fails if nullable is false).
 * Else executes the block and returns its value
 */
internal inline fun validatingAndIgnoringNullForStore(
	path: ResolutionPath,
	value: JsonNode,
	nullable: Boolean,
	block: () -> JsonNode
): JsonNode =
	if (value == NullNode.instance) {
		require(nullable) {
			"$path: invalid value `null`"
		}
		value
	} else {
		block()
	}

internal inline fun validatingAndIgnoringNullForRead(
	path: ResolutionPath,
	value: JsonNode,
	nullable: Boolean,
	block: () -> JsonNode
): JsonNode =
	if (value == NullNode.instance) {
		if (!nullable) throw IllegalEntityException("$path: invalid value `null`")
		value
	} else {
		block()
	}
