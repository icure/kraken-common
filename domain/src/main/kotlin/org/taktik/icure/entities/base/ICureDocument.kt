/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.entities.base

import org.taktik.couchdb.id.Identifiable
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
	val created: Long?
	val modified: Long?
	val author: String?
	val responsible: String?
	val medicalLocationId: String?
	val endOfLife: Long?

	fun solveConflictsWith(other: ICureDocument<T>): Map<String, Any?> = mapOf(
		"id" to this.id,
		"created" to (this.created?.coerceAtMost(other.created ?: Long.MAX_VALUE) ?: other.created),
		"modified" to (this.modified?.coerceAtLeast(other.modified ?: 0L) ?: other.modified),
		"endOfLife" to (this.endOfLife?.coerceAtMost(other.endOfLife ?: Long.MAX_VALUE) ?: other.endOfLife),
		"author" to (this.author ?: other.author),
		"responsible" to (this.responsible ?: other.responsible),
		"medicalLocationId" to (this.medicalLocationId ?: other.medicalLocationId),
		"tags" to (other.tags + this.tags),
		"codes" to (other.codes + this.codes),
	)

	fun withTimestamps(created: Long? = null, modified: Long? = null): ICureDocument<T>
}
