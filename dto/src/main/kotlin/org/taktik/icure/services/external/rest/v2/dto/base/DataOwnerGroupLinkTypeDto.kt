/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.dto.base

/**
 * The nature of the link between a crypto actor and a data owner representing one of its groups.
 */
enum class DataOwnerGroupLinkTypeDto {

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
	simple,

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
	parent,
}
