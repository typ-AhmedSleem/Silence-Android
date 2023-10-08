package org.smssecure.smssecure.search.messages

import java.io.Serializable

data class ConversationRecord(
	val senderId: Long,
	val senderName: String,
	val decryptedBody: String,
	val encryptedBody: String,
	val timestamp: Long
	// * Got more work here to do
) : Serializable
