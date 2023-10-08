package org.smssecure.smssecure.search

import org.smssecure.smssecure.database.model.MessageRecord
import org.smssecure.smssecure.search.contacts.Contact

abstract class SearchResult {

	data class ContactSearchResult(val contact: Contact) : SearchResult()

	class MessageSearchResult constructor(val record: MessageRecord) : SearchResult()

}