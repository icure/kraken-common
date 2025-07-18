package org.taktik.icure.asynclogic.impl

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.applications.utils.JarUtils
import org.taktik.icure.asynclogic.VersionLogic

@Service
@Profile("app")
class VersionLogicImpl(
	private val jarUtils: JarUtils,
	@Value("\${icure.version}") val versionFromProperties: String,
) : VersionLogic {
	override fun getVersion(): String {
		val manifest = jarUtils.manifest
		return if (manifest != null) {
			val version = manifest.mainAttributes.getValue("Build-revision")
			version?.trim { it <= ' ' } ?: ""
		} else {
			versionFromProperties.trim { it <= ' ' }
		}
	}

	override fun getSemanticVersion(): String {
		val semVerRegex = "(\\d+\\.\\d+\\.\\d+)".toRegex()
		return getVersion().let { version ->
			semVerRegex.find(version)?.groupValues?.get(1) ?: throw IllegalStateException("Invalid semantic version format")
		}
	}
}
