/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic.impl

import org.taktik.icure.asyncdao.ApplicationSettingsDAO
import org.taktik.icure.asynclogic.ApplicationSettingsLogic
import org.taktik.icure.asynclogic.ExchangeDataMapLogic
import org.taktik.icure.asynclogic.SessionInformationProvider
import org.taktik.icure.asynclogic.base.impl.EntityWithEncryptionMetadataLogic
import org.taktik.icure.asynclogic.impl.filter.Filters
import org.taktik.icure.datastore.DatastoreInstanceProvider
import org.taktik.icure.entities.ApplicationSettings
import org.taktik.icure.entities.embed.SecurityMetadata
import org.taktik.icure.validation.aspect.Fixer

open class ApplicationSettingsLogicImpl(
	private val applicationSettingsDAO: ApplicationSettingsDAO,
	datastoreInstanceProvider: DatastoreInstanceProvider,
	sessionInformationProvider: SessionInformationProvider,
	exchangeDataMapLogic: ExchangeDataMapLogic,
	fixer: Fixer,
	filters: Filters,
) : EntityWithEncryptionMetadataLogic<ApplicationSettings, ApplicationSettingsDAO>(
	fixer,
	sessionInformationProvider,
	datastoreInstanceProvider,
	exchangeDataMapLogic,
	filters
), ApplicationSettingsLogic {
	override fun getGenericDAO(): ApplicationSettingsDAO = applicationSettingsDAO

	override suspend fun createApplicationSettings(applicationSettings: ApplicationSettings): ApplicationSettings {
		val datastoreInformation = getInstanceAndGroup()
		return applicationSettingsDAO.create(datastoreInformation, applicationSettings)
	}

	override suspend fun modifyApplicationSettings(applicationSettings: ApplicationSettings): ApplicationSettings? {
		val datastoreInformation = getInstanceAndGroup()
		checkValidEntityChange(applicationSettings, null)
		return applicationSettingsDAO.save(datastoreInformation, applicationSettings)
	}

	override fun entityWithUpdatedSecurityMetadata(
		entity: ApplicationSettings,
		updatedMetadata: SecurityMetadata
	): ApplicationSettings =
		entity.copy(securityMetadata = updatedMetadata)
}
