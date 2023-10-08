package org.smssecure.smssecure.search.threads

import org.smssecure.smssecure.database.model.ThreadRecord
import java.io.Serializable

data class ConversationRecord(
	val encryptedBody: String,
	val record: ThreadRecord
) : Serializable {

	override fun toString(): String {
		return "ConversationRecord(encryptedBody='$encryptedBody', record=${record})"
	}
}