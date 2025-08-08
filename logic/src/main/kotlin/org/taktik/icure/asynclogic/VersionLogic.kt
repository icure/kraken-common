package org.taktik.icure.asynclogic

interface VersionLogic {

	/**
	 * Get the full version of the application server including for example `-g[commit hash]`.
	 *
	 * @return the version of the application server
	 */
	fun getVersion(): String

	/**
	 * Get the semantic version of the application server.
	 *
	 * @return the semantic version of the application server
	 * @sample 1.0.0
	 */
	fun getSemanticVersion(): String
}
