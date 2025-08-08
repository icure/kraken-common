/*
 * Copyright (c) 2020. Taktik SA, All rights reserved.
 */
package org.taktik.icure.services.external.rest.v1.dto.embed

@Suppress("EnumEntryName")
enum class DocumentStatusDto {
	draft,
	finalized,
	pending_review,
	reviewed,
	pending_signature,
	signed,
	canceled,
	sent,
	delivered,
}
