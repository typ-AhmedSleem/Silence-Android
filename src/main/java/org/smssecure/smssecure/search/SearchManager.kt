package org.smssecure.smssecure.search

import android.content.Context
import android.database.Cursor
import androidx.core.database.getStringOrNull
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.smssecure.smssecure.AdvancedSearchOptions
import org.smssecure.smssecure.crypto.MasterCipher
import org.smssecure.smssecure.crypto.MasterSecret
import org.smssecure.smssecure.database.DatabaseFactory
import org.smssecure.smssecure.database.ThreadDatabase
import org.smssecure.smssecure.database.model.ThreadRecord
import org.smssecure.smssecure.search.contacts.Contacts
import org.smssecure.smssecure.search.threads.ConversationRecord
import org.smssecure.smssecure.util.takeUntil
import java.util.Locale

object SearchManager {
	@JvmStatic
	@OptIn(DelicateCoroutinesApi::class)
	fun performGlobalSearch(
		context: Context,
		locale: Locale,
		secret: MasterSecret,
		options: AdvancedSearchOptions,
		query: String,
		callback: ResultCallback<MutableList<GlobalSearchResult>>
	) {
		val results = mutableListOf<GlobalSearchResult>()
		GlobalScope.launch(Dispatchers.IO) {
			val job = runCatching {
				async {
					Contacts.queryContacts(context, query)
						.takeUntil(options.contactsLimit)
						.forEach {
							results.add(GlobalSearchResult.ContactSearchResult(it))
						}
				}.await()

				async {
					getThreadsResolved(
						DatabaseFactory.getThreadDatabase(context),
						locale,
						secret,
						query,
						options
					).forEach { results.add(GlobalSearchResult.MessageSearchResult(it)) }
				}.await()

			}
			withContext(Dispatchers.Main) {
				job.onSuccess { callback.onResult(results) }
				job.onFailure { callback.onResult(results) }
			}
		}
	}

	private fun getThreadsResolved(
		db: ThreadDatabase,
		locale: Locale,
		secret: MasterSecret,
		query: String,
		options: AdvancedSearchOptions,
	): List<ConversationRecord> {

		val threadsCursor: Cursor = db.enhancedFilterThreads(locale, secret, query, options) ?: return emptyList()

		return threadsCursor.use { cursor ->
			val threads = mutableListOf<ConversationRecord>()
			val reader = db.readerFor(threadsCursor, MasterCipher(secret))
			var record: ThreadRecord?
			while ((reader.next.also { record = it }) != null) {
				val conversation = ConversationRecord(
					encryptedBody = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(ThreadDatabase.SNIPPET)) ?: "",
					record = record!!
				)
				threads.add(conversation)
			}
			return@use threads
		}
	}

	interface ResultCallback<T> {
		fun onResult(result: T)
	}

}