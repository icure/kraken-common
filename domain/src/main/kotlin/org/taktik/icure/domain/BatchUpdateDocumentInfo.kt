package org.taktik.icure.domain

import org.taktik.icure.entities.Document

/**
 * Information on a single document part of a batch updated.
 * @property newDocument new value for a document.
 * @property previousDocument the current version of the document or null if [newDocument] is a completely new document which will be created.
 */
data class BatchUpdateDocumentInfo(val newDocument: Document, val previousDocument: Document?) {
	/**
	 * Checks whether the document is a new document which will be created.
	 */
	val isNewDocument: Boolean
		get() = previousDocument == null
}
