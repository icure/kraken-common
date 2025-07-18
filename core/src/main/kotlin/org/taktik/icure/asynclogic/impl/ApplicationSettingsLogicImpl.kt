/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic.impl

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asyncdao.ApplicationSettingsDAO
import org.taktik.icure.asynclogic.ApplicationSettingsLogic
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.entities.ApplicationSettings
import org.taktik.icure.validation.aspect.Fixer

@Service
@Profile("app")
class ApplicationSettingsLogicImpl(
	private val applicationSettingsDAO: ApplicationSettingsDAO,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	fixer: Fixer,
	filters: Filters
) : GenericLogicImpl<ApplicationSettings, ApplicationSettingsDAO>(fixer, datastoreInstanceProvider, filters), ApplicationSettingsLogic {
	override fun getGenericDAO(): ApplicationSettingsDAO {
		return applicationSettingsDAO
	}

	override suspend fun createApplicationSettings(applicationSettings: ApplicationSettings): ApplicationSettings? {
		val datastoreInformation = getInstanceAndGroup()
		return applicationSettingsDAO.create(datastoreInformation, applicationSettings)
	}

	override suspend fun modifyApplicationSettings(applicationSettings: ApplicationSettings): ApplicationSettings? {
		val datastoreInformation = getInstanceAndGroup()
		return applicationSettingsDAO.save(datastoreInformation, applicationSettings)
	}
}
