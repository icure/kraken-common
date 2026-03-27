/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.base

import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.mergers.annotations.MergeStrategyMax
import org.taktik.icure.mergers.annotations.MergeStrategyMin

/**
 * Used to represent an entity that is saved inside the CouchDB database and includes some basic properties used to describe the lifecycle of the entity.
 *
 * @property created the timestamp (unix epoch in ms) of creation of the patient, will be filled automatically if missing. Not enforced by the application server.
 * @property modified the date (unix epoch in ms) of latest modification of the patient, will be filled automatically if missing. Not enforced by the application server.
 * @property author the id of the User that has created this patient, will be filled automatically if missing. Not enforced by the application server.
 * @property responsible the id of the HealthcareParty that is responsible for this patient, will be filled automatically if missing. Not enforced by the application server.
 * @property medicalLocationId
 *
 */
interface ICureDocument<T> :
	Identifiable<T>,
	HasTags,
	HasCodes {
	@MergeStrategyMin val created: Long?
	@MergeStrategyMax val modified: Long?
	val author: String?
	val responsible: String?
	val medicalLocationId: String?
	@MergeStrategyMax val endOfLife: Long?

	fun withTimestamps(created: Long? = null, modified: Long? = null): ICureDocument<T>
}
