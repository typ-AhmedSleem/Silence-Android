package org.smssecure.smssecure.search.threads

import android.content.Context
import android.database.Cursor
import android.util.Log
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
import org.smssecure.smssecure.search.GlobalSearchResult
import org.smssecure.smssecure.search.contacts.Contacts
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
					val contacts = Contacts.queryContacts(context, query)
					contacts.forEach { results.add(GlobalSearchResult.ContactSearchResult(it)) }
				}.await()

				async {
					resolvedThreads(
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

	private fun resolvedThreads(
		db: ThreadDatabase,
		locale: Locale,
		secret: MasterSecret,
		query: String,
		options: AdvancedSearchOptions,
	): List<ConversationRecord> {

		val threadsCursor: Cursor = db.enhancedFilterThreads(locale, secret, query, options) ?: return emptyList()

		return threadsCursor.use { cursor ->
			Log.i("SearchManager", "threadsListFromCursor: ${cursor.count}")
			val threads = mutableListOf<ConversationRecord>()
			val reader = db.readerFor(threadsCursor, MasterCipher(secret))
			var record: ThreadRecord?
			while ((reader.next.also { record = it }) != null) {
				val conversation = ConversationRecord(
					encryptedBody = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(ThreadDatabase.SNIPPET)) ?: "",
					record = record!!
				)
				threads.add(conversation)
				Log.d("SearchManager", "\t$conversation")
			}
			return@use threads
		}
	}

	interface ResultCallback<T> {
		fun onResult(result: T)
	}

}