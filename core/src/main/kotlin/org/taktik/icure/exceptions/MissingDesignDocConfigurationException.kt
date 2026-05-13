package org.taktik.icure.exceptions

class MissingDesignDocConfigurationException(
	designDocConfigId: String
) : Exception("Design doc configuration with id $designDocConfigId not found") {
	companion object {
		const val EXCEPTION_DETAIL = "MissingDesignDocConfigurationException"
	}
}