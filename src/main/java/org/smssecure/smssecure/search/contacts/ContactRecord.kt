package org.smssecure.smssecure.search.contacts

import android.net.Uri
import java.net.URI

data class ContactRecord(
	val id: Int,
	val name: String,
	val number: String,
	val photoUri: Uri?
) {
	override fun toString(): String {
		return "Contact[($id): $name -> $number | $photoUri]"
	}
}