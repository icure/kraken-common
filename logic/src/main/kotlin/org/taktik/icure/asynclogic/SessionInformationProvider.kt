/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.asynclogic

import org.taktik.icure.entities.DataOwnerType
import org.taktik.icure.entities.base.HasEncryptionMetadata
import org.taktik.icure.entities.utils.SemanticVersion
import org.taktik.icure.security.DataOwnerAuthenticationDetails
import org.taktik.icure.validation.DataOwnerProvider
import java.io.Serializable

interface SessionInformationProvider : DataOwnerProvider {
	suspend fun getCurrentSessionContext(): AsyncSessionContext

	/**
	 * @return a function that receives two parameter: a DataOwnerId and an [HasEncryptionMetadata] entity and returns true
	 * if the data owner can access the entity and false otherwise.
	 */
	suspend fun getSearchKeyMatcher(): (String, HasEncryptionMetadata) -> Boolean

	/**
	 * This method returns all the keys that can be used by a Data Owner to access their own encryptable entities, given their
	 * id. If the Data Owner that is currently logged in is not the one passed as parameter, only the Data Owner id is returned.
	 * @param dataOwnerId the id of the DataOwner to retrieve the access keys for.
	 * @return a [Set] of [String]
	 */
	suspend fun getAllSearchKeysIfCurrentDataOwner(dataOwnerId: String): Set<String>

	/**
	 * @return the version of the Cardinal SDK that made the request, if it was specified in the appropriate header and
	 * null otherwise
	 */
	suspend fun getCallerCardinalVersion(): SemanticVersion?

	// TODO probably this should be cached in the coroutine context to avoid calculating the hash every time
	suspend fun getDataOwnerAuthenticationDetails(): DataOwnerAuthenticationDetails

	suspend fun getDataOwnerAuthenticationDetailsOrNull(): DataOwnerAuthenticationDetails?

	/**
	 * If the current user is not a data owner returns false.
	 * If the current user has no parents data owner and the current data owner matches return true.
	 * If the data owner has parents, search for a match in the parents and return true if a match is found.
	 * Otherwise return false
	 */
	suspend fun selfOrDataOwnerHierarchyContains(id: String): Boolean

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
		 * The type of the current data owner (the data owner associated with the current user, or the data owner chosen
		 * as scope), or null if the current user is not a data owner and is not acting in the scope of a data owner.
		 */
		fun getDataOwnerType(): DataOwnerType?
	}
}
