package org.taktik.icure.exceptions

class MissingDesignDocConfigurationException(
	designDocConfigId: String
) : IllegalStateException("Design doc configuration with id $designDocConfigId not found")