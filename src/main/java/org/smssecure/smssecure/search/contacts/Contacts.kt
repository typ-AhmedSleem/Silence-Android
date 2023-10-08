package org.smssecure.smssecure.search.contacts

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.smssecure.smssecure.database.DatabaseFactory

object Contacts {

	@JvmStatic
	suspend fun queryContacts(context: Context, query: String): Array<ContactRecord> {
		return withContext(Dispatchers.IO) {
			val contacts = mutableListOf<ContactRecord>()
			var cursor: Cursor? = null

			try {
				cursor = context.contentResolver.query(
					Uri.withAppendedPath(
						Phone.CONTENT_FILTER_URI,
						Uri.encode(query)
					),
					arrayOf(Phone.CONTACT_ID, Phone.DISPLAY_NAME, Phone.NUMBER, Phone.PHOTO_URI),
					null,
					null,
					null
				)
				Log.i("Contacts", "queryContacts: START =======================")
				while (cursor != null && cursor.moveToNext()) {
					contacts.add(
						ContactRecord(
							id = cursor.getInt(cursor.getColumnIndexOrThrow(Phone.CONTACT_ID)),
							name = cursor.getString(cursor.getColumnIndexOrThrow(Phone.DISPLAY_NAME)),
							number = cursor.getString(cursor.getColumnIndexOrThrow(Phone.NUMBER)),
							photoUri = Uri.parse(cursor.getString(cursor.getColumnIndexOrThrow(Phone.PHOTO_URI)) ?: "")
						)
					)
					Log.d("Contacts", "\t${contacts.last()}")
				}
				Log.i("Contacts", "queryContacts: END =====================")
			} finally {
				cursor?.close()
			}
			return@withContext contacts.toTypedArray()
		}
	}

}