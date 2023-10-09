package org.smssecure.smssecure.search.chat

import android.content.Context
import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.smssecure.smssecure.database.DatabaseFactory
import org.smssecure.smssecure.database.model.MessageRecord
import org.smssecure.smssecure.search.SearchManager

object MessagePositioner {

	@OptIn(DelicateCoroutinesApi::class)
	@JvmStatic
	fun positionMessage(context: Context, threadId: Long, encryptedBody: String, callback: SearchManager.ResultCallback<Int>) {
		GlobalScope.launch(Dispatchers.IO) {
			var msgPosition = 0
			val db = DatabaseFactory.getMmsSmsDatabase(context)
			val cursor = db.getConversation(threadId, 2500)
			cursor.use { cur ->
				Log.i("Conversations", "Trying to get position of message in db ===========")
				Log.d("Conversations", "\tthreadId => $threadId | encBody => $encryptedBody")
				val reader = db.readerFor(cur)
				var record: MessageRecord?
				while (reader.next.also { record = it } != null) {
					Log.i("Conversations", "\t\tTrying content: ${record?.body?.body ?: ""}")
					if (encryptedBody == (record?.body?.body ?: "")) break
					msgPosition++
				}
			}
			
			if (msgPosition != -1) Log.v("Conversations", "\tFinal found position => $msgPosition")
			else Log.e("Conversations", "\tCan't find position of message")
			Log.i("Conversations", "===================================================")

			callback.onResult(msgPosition)
		}
	}

}