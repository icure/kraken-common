package org.taktik.icure.validation

/**
 * Validation of entity ids performed at creation time.
 *
 * Entity ids are routinely used as path parameters in the REST controllers (e.g. `/rest/v2/patient/{patientId}`).
 * Characters that have a special meaning in a URL path would either break the routing or be silently truncated by
 * Spring/Tomcat (for instance everything after a `;` is interpreted as a matrix variable and dropped), so we reject
 * them as soon as an entity is created to avoid storing entities that could never be retrieved by their id.
 */
object EntityIdValidation {
	/**
	 * Characters that are explicitly forbidden in an entity id because they are problematic when the id is used as a
	 * path parameter:
	 * - `/` and `\`: path separators;
	 * - `?` and `#`: start of the query string and of the fragment;
	 * - `%`: start of a percent-encoded sequence (e.g. `%2F` decodes to `/`);
	 * - `;`: separator of matrix variables, everything after it is stripped from the path segment by Spring.
	 *
	 * Note that `|`, used by composite ids such as `type|code|version` (e.g. Code, Tarification), is intentionally
	 * allowed.
	 */
	private val FORBIDDEN_CHARACTERS = setOf('/', '\\', '?', '#', '%', ';')

	/**
	 * Checks that [id] can be safely used as a path parameter and throws an [IllegalArgumentException] otherwise.
	 *
	 * An id is considered invalid if it contains any of the [FORBIDDEN_CHARACTERS], any whitespace character (space,
	 * tab, new line, ...) or any ISO control character.
	 *
	 * @param id the id of the entity being created.
	 * @throws IllegalArgumentException if [id] contains a forbidden character.
	 */
	fun checkValidForCreation(id: String) {
		val invalidChar = id.firstOrNull { it in FORBIDDEN_CHARACTERS || it.isWhitespace() || it.isISOControl() }
		require(invalidChar == null) {
			"Invalid entity id `$id`: an id cannot contain whitespaces, control characters or any of the following characters: ${FORBIDDEN_CHARACTERS.joinToString(" ")}"
		}
	}
}
