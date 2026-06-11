/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter

import java.io.Serializable

/**
 * Identifies a CouchDB configuration view (a view managed through the design-doc schema) used by a filter.
 *
 * @property entity the simple name of the entity the view is defined on (e.g. "Patient", "Contact"). Note that this
 * is the entity owning the view, which is not necessarily the entity returned by the filter (e.g. a filter on
 * services queries views defined on `Contact`).
 * @property view the name of the configuration view (the `configurationView` argument passed in the DAO query).
 */
data class ConfigurationView(
	val entity: String,
	val view: String,
) : Serializable
