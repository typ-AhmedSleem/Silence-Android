package org.smssecure.smssecure.search

import android.content.Context
import android.view.ViewGroup
import com.google.android.material.textview.MaterialTextView
import com.jude.easyrecyclerview.adapter.BaseViewHolder
import com.jude.easyrecyclerview.decoration.StickyHeaderDecoration
import org.smssecure.smssecure.R

class SearchStickyHeaderAdapter(val context: Context, val adapter: SearchResultsAdapter) : StickyHeaderDecoration.IStickyHeaderAdapter<SearchStickyHeaderAdapter.SearchStickyHeaderVH> {

	override fun getHeaderId(position: Int): Long {
		return if (adapter.getItem(position) is GlobalSearchResult.ContactSearchResult) TYPE_CONTACT
		else TYPE_THREAD
	}

	override fun onCreateHeaderViewHolder(parent: ViewGroup): SearchStickyHeaderVH {
		return SearchStickyHeaderVH(parent)
	}

	override fun onBindHeaderViewHolder(vh: SearchStickyHeaderVH, position: Int) {
		vh.setData(adapter.getItem(position))
	}

	class SearchStickyHeaderVH(parent: ViewGroup) : BaseViewHolder<GlobalSearchResult>(parent, R.layout.sticky_header_search) {

		override fun setData(result: GlobalSearchResult) {
			(itemView as MaterialTextView).apply {
				setText(
					if (result is GlobalSearchResult.ContactSearchResult) R.string.contacts
					else R.string.conversations
				)
			}
		}
	}

	companion object {

		private const val TYPE_CONTACT = 1L
		private const val TYPE_THREAD = 2L

	}

}