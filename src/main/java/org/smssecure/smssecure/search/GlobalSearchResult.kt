package org.smssecure.smssecure.search

import org.smssecure.smssecure.search.contacts.ContactRecord
import org.smssecure.smssecure.search.threads.ConversationRecord

abstract class GlobalSearchResult {

	data class ContactSearchResult(val contact: ContactRecord) : GlobalSearchResult()

	data class MessageSearchResult constructor(val conversation: ConversationRecord) : GlobalSearchResult()

}