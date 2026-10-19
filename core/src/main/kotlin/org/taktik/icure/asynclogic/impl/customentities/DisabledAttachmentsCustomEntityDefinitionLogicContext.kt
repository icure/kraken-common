package org.taktik.icure.asynclogic.impl.customentities

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asynclogic.objectstorage.CustomEntityDataAttachmentModificationLogic

/**
 * Context for custom entities that support attachments, but where they are currently disabled (they were enabled in
 * the past, so existing entities may still have attachments).
 *
 * Doesn't provide any helper: methods specific to attachments are not allowed when attachments are disabled.
 */
@Service
@Profile("app")
class DisabledAttachmentsCustomEntityDefinitionLogicContext(
	attachmentModificationLogic: CustomEntityDataAttachmentModificationLogic,
) : AbstractAttachmentCustomEntityDefinitionLogicContext(emptyMap(), attachmentModificationLogic) {
	override fun invalidAttachmentChangeMessage(entityType: String): String =
		"Attachments are not enabled for custom entity type $entityType"
}
