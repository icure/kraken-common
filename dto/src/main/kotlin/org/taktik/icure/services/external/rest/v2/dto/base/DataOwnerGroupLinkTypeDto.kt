/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */

package org.taktik.icure.services.external.rest.v2.dto.base

/**
 * The nature of the link between a crypto actor and a data owner representing one of its groups.
 *
 * All links are transitive, whatever their type: when the linked group is itself a member of another group, the
 * original actor is a member of that further group as well, so resolving the complete set of groups of an actor
 * requires following the links recursively.
 */
enum class DataOwnerGroupLinkTypeDto {
	parent,
	simple,
}
