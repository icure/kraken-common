/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asyncservice

import kotlinx.coroutines.flow.Flow
import org.taktik.icure.asyncservice.base.EntityWithSecureDelegationsService
import org.taktik.icure.entities.ApplicationSettings

interface ApplicationSettingsService : EntityWithSecureDelegationsService<ApplicationSettings> {
	suspend fun createApplicationSettings(applicationSettings: ApplicationSettings): ApplicationSettings
	suspend fun modifyApplicationSettings(applicationSettings: ApplicationSettings): ApplicationSettings?

	/**
	 * @return a [Flow] containing all the [ApplicationSettings] that the current user can access.
	 */
	fun getAllApplicationSettings(): Flow<ApplicationSettings>
}
