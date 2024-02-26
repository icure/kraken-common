/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.base

/**
 * @param <T> The type of the entity identity (a String, a UUID, etc.)
 */
interface VersionableDto<T> : IdentifiableDto<T>, Revisionable {
	override val rev: String?
	fun withIdRev(id: T?, rev: String): VersionableDto<T>
}
