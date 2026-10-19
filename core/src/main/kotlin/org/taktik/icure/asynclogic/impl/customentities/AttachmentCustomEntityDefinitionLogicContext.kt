package org.taktik.icure.asynclogic.impl.customentities

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.taktik.icure.asynclogic.objectstorage.CustomEntityDataAttachmentLoader
import org.taktik.icure.asynclogic.objectstorage.CustomEntityDataAttachmentModificationLogic

/**
 * Context for custom entities with attachments enabled: provides the helpers for the attachment-specific methods.
 */
@Service
@Profile("app")
class AttachmentCustomEntityDefinitionLogicContext(
	attachmentModificationLogic: CustomEntityDataAttachmentModificationLogic,
	attachmentLoader: CustomEntityDataAttachmentLoader,
) : AbstractAttachmentCustomEntityDefinitionLogicContext(
	buildMap {
		put(CustomEntityLogicContextHelper.AttachmentModification, attachmentModificationLogic)
		put(CustomEntityLogicContextHelper.AttachmentLoader, attachmentLoader)
	},
	attachmentModificationLogic,
) {
	override fun invalidAttachmentChangeMessage(entityType: String): String =
		"Attachment information of $entityType can only be changed through the attachment methods"
}
