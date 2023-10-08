package org.smssecure.smssecure.search

import android.content.Context
import android.view.ViewGroup
import android.widget.TextView
import com.jude.easyrecyclerview.adapter.BaseViewHolder
import com.jude.easyrecyclerview.adapter.RecyclerArrayAdapter
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.smssecure.smssecure.ConversationListItem
import org.smssecure.smssecure.R
import org.smssecure.smssecure.components.AvatarImageView
import org.smssecure.smssecure.contacts.avatars.ContactColors
import org.smssecure.smssecure.contacts.avatars.ContactPhotoFactory
import org.smssecure.smssecure.crypto.MasterSecret
import java.util.Locale

class SearchResultsAdapter(
	context: Context,
	val secret: MasterSecret,
	val locale: Locale
) : RecyclerArrayAdapter<GlobalSearchResult>(context) {

	override fun OnCreateViewHolder(parent: ViewGroup, type: Int): BaseViewHolder<*> {
		return if (type == 1) ContactResultVH(parent)
		else ConversationResultVH(parent, secret, locale)
	}

	override fun getViewType(position: Int): Int {
		return if (getItem(position) is GlobalSearchResult.ContactSearchResult) 1 else 2
	}

	private class ContactResultVH(parent: ViewGroup) : BaseViewHolder<GlobalSearchResult.ContactSearchResult>(parent, R.layout.item_contact_result) {

		private val tvName: TextView = itemView.findViewById(R.id.name)
		private val tvNumber: TextView = itemView.findViewById(R.id.number)
		private val aivAvatar: AvatarImageView = itemView.findViewById(R.id.contact_photo_image)

		@OptIn(DelicateCoroutinesApi::class)
		override fun setData(result: GlobalSearchResult.ContactSearchResult) {
			tvName.text = result.contact.name
			tvNumber.text = result.contact.number
//			itemView.setOnClickListener { aivAvatar.performClick() }

			GlobalScope.launch(Dispatchers.IO) {
				val contactPhoto = async {
					ContactPhotoFactory.getContactPhoto(
						context,
						result.contact.photoUri,
						result.contact.name
					).asDrawable(
						context,
						ContactColors.generateFor(result.contact.name).toConversationColor(context)
					)
				}
				withContext(Dispatchers.Main) { aivAvatar.setImageDrawable(contactPhoto.await()) }
			}
		}

	}

	private class ConversationResultVH(parent: ViewGroup, val secret: MasterSecret, val locale: Locale) : BaseViewHolder<GlobalSearchResult.MessageSearchResult>(parent, R.layout.conversation_list_item_view) {

		private val listItem = itemView as ConversationListItem

		override fun setData(result: GlobalSearchResult.MessageSearchResult) {
			listItem.bind(secret, result.conversation.record, locale, emptySet(), false)
		}

	}

}