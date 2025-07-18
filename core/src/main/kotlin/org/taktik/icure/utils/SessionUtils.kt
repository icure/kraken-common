/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.utils

import org.taktik.icure.asynclogic.SessionInformationProvider
import javax.security.auth.login.LoginException

suspend fun getLoggedHealthCarePartyId(sessionLogic: SessionInformationProvider): String = sessionLogic.getCurrentSessionContext().getHealthcarePartyId() ?: throw LoginException("You must be a HCP to perform this action. ")

suspend fun getLoggedDataOwnerId(sessionLogic: SessionInformationProvider): String {
	val ctx = sessionLogic.getCurrentSessionContext()
	return ctx.getHealthcarePartyId() ?: ctx.getPatientId() ?: ctx.getDeviceId()
		?: throw LoginException("You must be a data owner to perform this action. ")
}
