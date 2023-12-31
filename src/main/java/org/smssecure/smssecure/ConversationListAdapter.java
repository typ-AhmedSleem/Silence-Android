/**
 * Copyright (C) 2011 Whisper Systems
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.smssecure.smssecure;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.smssecure.smssecure.crypto.MasterCipher;
import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.database.CursorRecyclerViewAdapter;
import org.smssecure.smssecure.database.DatabaseFactory;
import org.smssecure.smssecure.database.ThreadDatabase;
import org.smssecure.smssecure.database.model.ThreadRecord;
import org.smssecure.smssecure.recipients.Recipients;
import org.smssecure.smssecure.util.Conversions;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Set;

/**
 * A CursorAdapter for building a list of conversation threads.
 *
 * @author Moxie Marlinspike
 */
public class ConversationListAdapter extends CursorRecyclerViewAdapter<ConversationListAdapter.ViewHolder> {

    private static final int MESSAGE_TYPE_SWITCH_ARCHIVE = 1;
    private static final int MESSAGE_TYPE_THREAD = 2;

    public final ThreadDatabase threadDatabase;
    private final MasterSecret masterSecret;
    private final MasterCipher masterCipher;
    private final Locale locale;
    private final LayoutInflater inflater;
    private final ItemClickListener clickListener;
    private final @NonNull MessageDigest digest;

    private final Set<Long> batchSet = Collections.synchronizedSet(new HashSet<Long>());
    private final LinkedList<Pair<Long, Recipients>> threadIdAndRecipients = new LinkedList<Pair<Long, Recipients>>();
    private boolean batchMode = false;

    public ConversationListAdapter (
            @NonNull Context context,
            @NonNull MasterSecret masterSecret,
            @NonNull Locale locale,
            @Nullable Cursor cursor,
            @Nullable ItemClickListener clickListener) {
        super(context, cursor);
        try {
            this.masterSecret = masterSecret;
            this.masterCipher = new MasterCipher(masterSecret);
            this.threadDatabase = DatabaseFactory.getThreadDatabase(context);
            this.locale = locale;
            this.inflater = LayoutInflater.from(context);
            this.clickListener = clickListener;
            this.digest = MessageDigest.getInstance("SHA1");
            setHasStableIds(true);
        } catch (NoSuchAlgorithmException nsae) {
            throw new AssertionError("SHA-1 missing");
        }
    }

    @Override
    public long getItemId (@NonNull Cursor cursor) {
        ThreadRecord record = getThreadRecord(cursor);
        StringBuilder builder = new StringBuilder(String.valueOf(record.getThreadId()));

        for (long recipientId : record.getRecipients().getIds()) {
            builder.append("::").append(recipientId);
        }

        return Conversions.byteArrayToLong(digest.digest(builder.toString().getBytes()));
    }

    @Override
    public ViewHolder onCreateItemViewHolder (ViewGroup parent, int viewType) {
        if (viewType == MESSAGE_TYPE_SWITCH_ARCHIVE) {
            ConversationListItemAction action = (ConversationListItemAction) inflater.inflate(R.layout.conversation_list_item_action, parent, false);

            action.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onSwitchToArchive();
            });

            return new ViewHolder(action);
        } else {
            final ConversationListItem item = (ConversationListItem) inflater.inflate(R.layout.conversation_list_item_view, parent, false);

            item.setOnClickListener(view -> {
                if (clickListener != null) clickListener.onItemClick(item);
            });

            item.setOnLongClickListener(view -> {
                if (clickListener != null) clickListener.onItemLongClick(item);
                return true;
            });

            return new ViewHolder(item);
        }
    }

    @Override
    public void onItemViewRecycled (ViewHolder holder) {
        holder.getItem().unbind();
    }

    @Override
    public void onBindItemViewHolder (ViewHolder viewHolder, @NonNull Cursor cursor) {
        final ThreadRecord thread = getThreadRecord(cursor);
        viewHolder.itemView.setTag(thread.getThreadId());
        viewHolder.getItem().bind(masterSecret, thread, locale, batchSet, batchMode);
    }

    @Override
    public int getItemViewType (@NonNull Cursor cursor) {
        ThreadRecord threadRecord = getThreadRecord(cursor);

        if (threadRecord.getDistributionType() == ThreadDatabase.DistributionTypes.ARCHIVE) {
            return MESSAGE_TYPE_SWITCH_ARCHIVE;
        } else {
            return MESSAGE_TYPE_THREAD;
        }
    }

    private ThreadRecord getThreadRecord (@NonNull Cursor cursor) {
        return threadDatabase.readerFor(cursor, masterCipher).getCurrent();
    }

    public void toggleThreadInBatchSet (long threadId) {
        if (batchSet.contains(threadId)) {
            batchSet.remove(threadId);
        } else if (threadId != -1) {
            batchSet.add(threadId);
        }
    }

    public void populateRecipients (long threadId, Recipients recipients) {
        threadIdAndRecipients.add(new Pair<>(threadId, recipients));
    }

    public @Nullable Recipients getRecipientsFromThreadId (long threadId) {
        for (Pair<Long, Recipients> pair : threadIdAndRecipients) {
            if (threadId == pair.first) return pair.second;
        }
        return null;
    }

    public Set<Long> getBatchSelections () {
        return batchSet;
    }

    public void initializeBatchMode (boolean toggle) {
        this.batchMode = toggle;
        unselectAllThreads();
    }

    public void unselectAllThreads () {
        this.batchSet.clear();
        notifyDataSetChanged();
    }

    public void selectAllThreads () {
        for (int i = 0; i < getItemCount(); i++) {
            long threadId = getThreadRecord(getCursorAtPositionOrThrow(i)).getThreadId();
            if (threadId != -1) batchSet.add(threadId);
        }
        notifyDataSetChanged();
    }

    public interface ItemClickListener {
        void onItemClick (ConversationListItem item);

        void onItemLongClick (ConversationListItem item);

        void onSwitchToArchive ();
    }

    protected static class ViewHolder extends RecyclerView.ViewHolder {
        public <V extends View & BindableConversationListItem> ViewHolder (final @NonNull V itemView) {
            super(itemView);
        }

        public BindableConversationListItem getItem () {
            return (BindableConversationListItem) itemView;
        }
    }
}
