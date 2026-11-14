/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.domain.filter

import kotlinx.coroutines.flow.Flow
import org.taktik.couchdb.id.Identifiable
import org.taktik.icure.entities.base.HasEncryptionMetadata
import java.io.Serializable

/**
 * A filter on entities of type [O], identified by ids of type [T].
 *
 * A filter can be used in two ways:
 * - **Resolution** (the usual way): the logic-layer implementation of the filter (in
 *   `org.taktik.icure.asynclogic.impl.filter`) queries CouchDB views and returns the ids of the matching entities.
 * - **In-memory evaluation** ([matches] and [applyTo]): checks entities that are already loaded. This is used to
 *   re-check the entities returned for ids from resolution (e.g. `ContactLogic.filterServices`, where the services
 *   are extracted from their contacts), and to decide whether an entity changed in a WebSocket subscription should
 *   be sent to the subscriber (see `AbstractFilter.canBeUsedInWebsocket`).
 *
 * In-memory evaluation must be consistent with resolution: an entity whose id is returned by resolution should
 * match. Some criteria, however, can't be checked on a single entity. For example, "this is the latest version of
 * the service" needs the other versions of the service. Such criteria are only enforced during resolution.
 *
 * ## The search key matcher
 *
 * Encryptable entities ([HasEncryptionMetadata]) don't always store the ids of the data owners they are shared with
 * in clear text. With secure delegations, an entity can be shared through an anonymous delegation, keyed by the hash
 * of an access control key instead of the data owner id. Only the data owner (or someone holding their access
 * control keys) can tell that such a delegation is theirs. A filter that has, for example, a `dataOwnerId` therefore
 * can't check "is this entity shared with `dataOwnerId`" by reading the entity alone.
 *
 * `searchKeyMatcher(dataOwnerId, entity)` answers that question. It returns true if `entity` has a delegation for
 * `dataOwnerId`, using everything the current session knows about that data owner. Filters call it instead of
 * inspecting the delegations themselves, and must pass the encryption metadata of the entity being checked (for a
 * `Service`, use `withEncryptionMetadata()`, which returns null for a service that wasn't extracted from its contact).
 *
 * The matcher normally comes from `SessionInformationProvider.getSearchKeyMatcher()`, which builds it from the
 * current session:
 * - If `dataOwnerId` is the data owner of the current session, the entity matches if it has a delegation keyed by
 *   that id or by the hash of any of the session's access control keys.
 * - For any other data owner, the entity matches only if it has a delegation keyed by `dataOwnerId` in clear text.
 *   The session has no access control keys for that data owner, so it can't recognise their anonymous delegations.
 *
 * This is the in-memory equivalent of `SessionInformationProvider.getAllSearchKeysIfCurrentDataOwner`, which
 * resolution uses to build the view keys. Both must give the same answer for the same session.
 */
interface Filter<T : Serializable, O : Identifiable<T>> {
	/**
	 * Returns true if [item] satisfies the criteria of this filter that can be checked in memory (see [Filter]).
	 *
	 * @param item the entity to check.
	 * @param searchKeyMatcher returns true if the encryption metadata given as second argument has a delegation for
	 * the data owner id given as first argument, according to the keys known by the current session. Filters with a
	 * data owner criterion must use it instead of reading the delegations of the entity directly.
	 */
	fun matches(item: O, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Boolean

	/**
	 * Returns the elements of [items] for which [matches] is true, in their original order.
	 * See [matches] for the role of [searchKeyMatcher].
	 */
	fun applyTo(items: List<O>, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): List<O>

	/**
	 * Returns the elements of [items] for which [matches] is true.
	 * See [matches] for the role of [searchKeyMatcher].
	 */
	fun applyTo(items: Set<O>, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Set<O>

	/**
	 * Returns a flow that lazily emits the elements of [items] for which [matches] is true, in their original order.
	 * Nothing is evaluated until the returned flow is collected.
	 * See [matches] for the role of [searchKeyMatcher].
	 */
	fun applyTo(items: Flow<O>, searchKeyMatcher: (String, HasEncryptionMetadata) -> Boolean): Flow<O>
}
