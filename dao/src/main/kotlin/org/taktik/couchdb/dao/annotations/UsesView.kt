package org.taktik.couchdb.dao.annotations

/**
 * Declares the configuration views queried by a DAO method.
 *
 * @param views the names of the configuration views used by the annotated method.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class UsesView(val views: Array<String>)
