package org.taktik.icure.entities.base

import org.taktik.icure.entities.DataOwnerType

/**
 * The nature of the link between a crypto actor and a data owner representing one of its groups.
 *
 * This is intrinsic to the *target* of the link (see [CryptoActor.groupLinkType]): every link
 * pointing at a given data owner has the same type, whoever declares it.
 */
enum class DataOwnerGroupLinkType(val strength: Int) {

	/**
	 * This is type of link should be sufficient for most use cases, and it is the recommended type to use if you don't
	 * have to handle a legacy hierarchy based on [CryptoActor.parentId].
	 *
	 * When using a simple-type link sharing is done by ensuring that exchange data created towards the group is
	 * decryptable by each leaf data owner of that group (going down recursively if needed), using their own keypair;
	 * there is no shared keypair between all the users of the group.
	 *
	 * This simplifies safe removal of crypto actors from a group, since instead of invalidating a keypair that is known
	 * by all users it is instead sufficient to invalidate the exchange data for encryption.
	 */
	simple(0),

	/**
	 * The type of link matching the legacy [CryptoActor.parentId] style of link.
	 *
	 * When using a parent-type link the parent is required to have a keypair, and all users linking to the parent
	 * through a parent-type link are required to have access to the private key.
	 *
	 * This link type is generally recommended only for legacy [CryptoActor.parentId] compatibility, or for groups of
	 * data owners that are actually the same person, so that having a shared parent key is not going to cause issues.
	 *
	 * A data owner is allowed to have multiple parents, and a mix of [parent]/[simple] links, but normally a single
	 * chain of parent links should be sufficient.
	 *
	 * # Example use case
	 *
	 * A user has multiple devices, and he is using cardinal from each device. Some data should be available across
	 * all the devices of the user, but other should only be available on the device that created it, and saving the
	 * latter only on the device is not a valid option, for example because it needs to be shared with a separate user.
	 *
	 * In this case an option would be to have a main data owner for the user that will be parent to other sub-data
	 * owners. After login from a device the user changes its acting scope to the child data owner specifically of the
	 * device.
	 *
	 * Data that should be available only in the current device is created only with a delegation for the scoped data
	 * owner, data  that should be shared across all devices can be shared with the parent data owner
	 *
	 * # Additional rights
	 *
	 * There are some permissions that only apply when there are parent-type links; generally this involves:
	 * - access to encryption metadata: a user does not need to access key recovery metadata (e.g. transfer keys) of a
	 *   linked simple data owner group, since they don't need to have a key
	 * - acting in scope, as in the example above
	 */
	parent(10),

	/**
	 * This data owner may never be used as a group target: no [DataOwnerGroupLink] or legacy
	 * [CryptoActor.parentId] may point at a data owner whose [CryptoActor.effectiveGroupLinkType] is
	 * this value. [strength] is unused for this value in practice: a [notAllowed] target can never
	 * appear as a resolved link (it is rejected at write time, before any traversal ever sees it), so
	 * it is never actually compared against [simple]/[parent] strengths.
	 */
	notAllowed(Int.MAX_VALUE),
	;

	companion object {
		/**
		 * The [DataOwnerGroupLinkType] that a data owner of this [DataOwnerType] has as its
		 * [CryptoActor.effectiveGroupLinkType] when its own [CryptoActor.groupLinkType] is `null`:
		 * [parent] for [DataOwnerType.HCP] (preserves the legacy [CryptoActor.parentId] behavior),
		 * [notAllowed] otherwise.
		 */
		fun DataOwnerType.defaultGroupLinkType(): DataOwnerGroupLinkType = if (this == DataOwnerType.HCP) parent else notAllowed
	}
}

/**
 * Whether this type is acceptable under a [minAcceptedType] threshold: never [DataOwnerGroupLinkType.notAllowed],
 * and — if [minAcceptedType] is given — at least as strong as it. `null` means no minimum: any type but
 * `notAllowed` is acceptable.
 *
 * Expressing an acceptance rule as a single strength threshold, rather than an arbitrary set of accepted types,
 * matches the "weak things can't stand in for strong things" principle used throughout the hierarchy (see
 * `HcpHierarchyResolver.canTransitivelyFollow` and the write-time rule on `CryptoActorLogicHelper`): with only
 * [simple] and [parent] as real, non-`notAllowed` types today a threshold and an arbitrary set happen to describe
 * the same two cases, but a threshold stays meaningful and can't express a nonsensical, non-contiguous combination
 * (e.g. accepting a hypothetical weaker-than-simple type while rejecting `simple` itself) if a third type is ever
 * added in between.
 */
fun DataOwnerGroupLinkType.isAtLeast(minAcceptedType: DataOwnerGroupLinkType?): Boolean =
	this != DataOwnerGroupLinkType.notAllowed && (minAcceptedType == null || strength >= minAcceptedType.strength)
