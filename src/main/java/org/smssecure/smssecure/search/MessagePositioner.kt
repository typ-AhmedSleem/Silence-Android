package org.smssecure.smssecure.search

import android.content.Context
import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.smssecure.smssecure.database.DatabaseFactory
import org.smssecure.smssecure.database.model.MessageRecord

object MessagePositioner {

	@OptIn(DelicateCoroutinesApi::class)
	@JvmStatic
	fun positionMessage(context: Context, threadId: Long, encryptedBody: String?, callback: SearchManager.ResultCallback<Int>) {
		if (encryptedBody.isNullOrEmpty()) return
		GlobalScope.launch(Dispatchers.IO) {
			var msgPosition = 0
			val db = DatabaseFactory.getMmsSmsDatabase(context)
			val cursor = db.getConversation(threadId, 2500)
			cursor.use { cur ->
				val reader = db.readerFor(cur)
				var record: MessageRecord?
				while (reader.next.also { record = it } != null) {
					Log.i("Conversations", "\t\tTrying content: ${record?.body?.body ?: ""}")
					if (encryptedBody == (record?.body?.body ?: "")) break
					msgPosition++
				}
			}
			callback.onResult(msgPosition)
		}
	}

}