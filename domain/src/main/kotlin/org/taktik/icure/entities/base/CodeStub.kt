/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.entities.base

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.taktik.icure.handlers.CodeStubDeserializer
import org.taktik.icure.utils.DynamicInitializer
import org.taktik.icure.utils.invoke
import java.io.Serializable

/**
 *
 *  __          __     _____  _   _ _____ _   _  _____
 *  \ \        / /\   |  __ \| \ | |_   _| \ | |/ ____|
 *   \ \  /\  / /  \  | |__) |  \| | | | |  \| | |  __
 *    \ \/  \/ / /\ \ |  _  /| . ` | | | | . ` | | |_ |
 *     \  /\  / ____ \| | \ \| |\  |_| |_| |\  | |__| |
 *      \/  \/_/    \_\_|  \_\_| \_|_____|_| \_|\_____|
 *
 * WARNING: If you ever change this class, you must also change the deserializer
 * Luca and Clement lost 2hr of their life because of this
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = CodeStubDeserializer::class)
data class CodeStub(
	@JsonProperty("_id") override val id: String, // id = type|code|version  => this must be unique
	override val context: String? = null, // ex: When embedded the context where this code is used
	override val type: String? = null, // ex: ICD (type + version + code combination must be unique) (or from tags -> CD-ITEM)
	override val code: String? = null, // ex: I06.2 (or from tags -> healthcareelement). Local codes are encoded as LOCAL:SLLOCALFROMMYSOFT
	override val version: String? = null, // ex: 10. Must be lexicographically searchable
	val contextLabel: String? = null,
	override val label: Map<String, String>? = null, // ex: {en: Rheumatic Aortic Stenosis, fr: Sténose rhumatoïde de l'Aorte}
) : CodeIdentification,
	Serializable {

	companion object : DynamicInitializer<CodeStub> {
		fun from(type: String, code: String, version: String) = CodeStub(id = "$type|$code|$version", type = type, code = code, version = version)
		fun fromId(id: String) = id.split("|")
			.also { require(it.size == 3) { "id: $id must have type|code|version format" } }
			.let { CodeStub(id = id, type = it[0], code = it[1], version = it[2]) }
	}

	fun merge(other: CodeStub) = CodeStub(args = this.solveConflictsWith(other))
	fun solveConflictsWith(other: CodeStub) = super.solveConflictsWith(other)

	override fun normalizeIdentification(): CodeStub {
		val parts = this.id.split("|").toTypedArray()
		return if (this.type == null || this.code == null || this.version == null) {
			this.copy(
				type = this.type ?: parts[0],
				code = this.code ?: parts[1],
				version = this.version ?: parts[2],
			)
		} else {
			this
		}
	}

	fun requireNormalized() {
		val parts = this.id.split("|")
		require(
			parts.size == 3 &&
				!type.isNullOrBlank() &&
				parts[0] == type &&
				!code.isNullOrBlank() &&
				parts[1] == code &&
				!version.isNullOrBlank() &&
				parts[2] == version &&
				label.isNullOrEmpty(), // TODO Deprecate label
		) { "Invalid code stub: $this" }
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is CodeStub) return false

		if (id != other.id) return false
		if (context != other.context) return false
		if (contextLabel != other.contextLabel) return false
		if (type != other.type) return false
		if (code != other.code) return false
		if (version != other.version) return false
		if (label != other.label && ((label?.size ?: 0) > 0 || ((other.label?.size ?: 0) > 0))) return false

		return true
	}

	override fun hashCode(): Int {
		var result = id.hashCode()
		result = 31 * result + (context?.hashCode() ?: 0)
		result = 31 * result + (contextLabel?.hashCode() ?: 0)
		result = 31 * result + (type?.hashCode() ?: 0)
		result = 31 * result + (code?.hashCode() ?: 0)
		result = 31 * result + (version?.hashCode() ?: 0)
		return result
	}
}

fun Iterable<CodeStub>.containsStubWithTypeAndCode(type: String, code: String?) = if (code != null) {
	this.any { it.type == type && it.code == code }
} else {
	this.any { it.type == type }
}
