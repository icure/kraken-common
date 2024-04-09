/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import org.taktik.icure.entities.DataOwnerType
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.security.DataOwnerAuthenticationDetails
import org.taktik.icure.validation.DataOwnerProvider
import java.io.Serializable

interface SessionInformationProvider: DataOwnerProvider {
	suspend fun getCurrentSessionContext(): AsyncSessionContext

	suspend fun getCurrentHealthcarePartyId(): String

	/**
	 * @return a function that receives two parameter: a DataOwnerId and an [HasEncryptionMetadata] entity and returns true
	 * if the data owner can access the entity and false otherwise.
	 */
	suspend fun getSearchKeyMatcher(): (String, HasEncryptionMetadata) -> Boolean

	// TODO probably this should be cached in the coroutine context to avoid calculating the hash every time
	suspend fun getDataOwnerAuthenticationDetails(): DataOwnerAuthenticationDetails

	interface AsyncSessionContext : Serializable {

		/**
		 * Id of the global user if applicable, or id of the current user in local-only installations.
		 */
		fun getGlobalUserId(): String

		/**
		 * Id of the user
		 */
		fun getUserId(): String

		fun getPatientId(): String?
		fun getHealthcarePartyId(): String?
		fun getDeviceId(): String?

		/**
		 * The data owner type of the current user, or null if the current user is not a data owner
		 */
		fun getDataOwnerType(): DataOwnerType?

		/**
		 * If the data owner is a hcp returns a list where the first element is the topmost ancestor in the hierarchy
		 * of the logged hcp, while the last is the direct parent of the hcp.
		 * Always empty for non-hcp users
		 */
		fun getHcpHierarchy(): List<String>
	}

}
