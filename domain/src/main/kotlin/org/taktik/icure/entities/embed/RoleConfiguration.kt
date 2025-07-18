package org.taktik.icure.entities.embed

/**
 * This class represents the default roles assigned to a user type in a group.
 * The configuration can be defined in the group (then it will be in the entry of the map with type
 * [ConfigurationSource.CONFIGURATION]), in a supergroup ([ConfigurationSource.INHERITED]) or
 * be the default one defined by iCure ([ConfigurationSource.DEFAULT]).
 */
data class RoleConfiguration(
	val source: ConfigurationSource,
	val roles: Set<String> = emptySet(),
) {

	companion object {
		enum class ConfigurationSource { CONFIGURATION, INHERITED, DEFAULT }
	}
}
