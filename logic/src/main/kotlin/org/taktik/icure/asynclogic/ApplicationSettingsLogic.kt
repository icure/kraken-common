/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import org.taktik.icure.entities.ApplicationSettings

interface ApplicationSettingsLogic : EntityPersister<ApplicationSettings, String> {
	suspend fun createApplicationSettings(applicationSettings: ApplicationSettings): ApplicationSettings?
	suspend fun modifyApplicationSettings(applicationSettings: ApplicationSettings): ApplicationSettings?
}
