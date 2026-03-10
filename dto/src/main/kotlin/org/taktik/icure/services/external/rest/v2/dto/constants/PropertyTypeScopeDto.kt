package org.taktik.icure.services.external.rest.v2.dto.constants

/**
 * Defines the scope at which a property type applies within the iCure system.
 * Scopes range from system-wide settings to per-user or per-event configurations.
 */
enum class PropertyTypeScopeDto {
	SYSTEM,
	NODE,
	ROLE,
	USER,
	EVENT,
}
