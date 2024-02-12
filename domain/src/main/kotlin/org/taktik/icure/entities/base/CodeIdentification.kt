/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.base

interface CodeIdentification {
	val id: String
	val code: String?
	val type: String?
	val version: String?
	val context: String?
	val contextLabel: String?
	val label: Map<String, String>?

	fun solveConflictsWith(other: CodeIdentification): Map<String, Any?> {
		return mapOf(
			"id" to (this.id),
			"code" to (this.code ?: other.code),
			"type" to (this.type ?: other.type),
			"context" to (this.context ?: other.context),
			"contextLabel" to (this.contextLabel ?: other.contextLabel),
			"version" to (this.version ?: other.version),
			"label" to (other.label?.let { it + (this.label ?: mapOf()) } ?: this.label)
		)
	}
	fun normalizeIdentification(): CodeIdentification
}
