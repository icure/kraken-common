/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.embed

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import org.taktik.icure.mergers.annotations.Mergeable
import java.io.Serializable

/**
 * A directed, qualified link from one [org.taktik.icure.entities.HealthElement] to another.
 *
 * Links should be created in a single direction: the reverse link can be found through a view.
 *
 * @property type The qualification of the link. Free string; using the names of
 * [org.taktik.icure.entities.base.LinkQualification] entries is encouraged but not enforced.
 * @property associationId A caller-chosen correlation id that groups related links across entities.
 * @property healthElementId The id of the linked [org.taktik.icure.entities.HealthElement].
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Mergeable(["healthElementId", "type"])
data class HealthElementQualifiedLink(
	val type: String,
	val associationId: String? = null,
	val healthElementId: String,
) : Serializable
