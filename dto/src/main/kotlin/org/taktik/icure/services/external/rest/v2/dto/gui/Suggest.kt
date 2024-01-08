/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v2.dto.gui

import java.io.Serializable

/**
 * Created by aduchate on 03/12/13, 20:57
 */
class Suggest(
	val filterKey: String? = null,
	val filterValue: String? = null,
	val entityClass: String? = null,
	val fieldValue: String? = null,
	val fieldDisplay: String? = null,
) : Serializable
