package org.smssecure.smssecure.search.contacts

data class Contact(
	val id: Int,
	val name: String,
	val number: String
) {
	override fun toString(): String {
		return "Contact[($id): $name -> $number]"
	}
}