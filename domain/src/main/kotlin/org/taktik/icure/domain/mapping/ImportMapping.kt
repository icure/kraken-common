/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.domain.mapping

import java.io.Serializable
import org.taktik.icure.entities.base.CodeStub

class ImportMapping(
	val lifecycle: String? = null,
	val content: String? = null,
	val cdLocal: String? = null,
	val label: Map<String, String> = HashMap(),
	val tags: Set<CodeStub> = setOf()
) : Serializable
