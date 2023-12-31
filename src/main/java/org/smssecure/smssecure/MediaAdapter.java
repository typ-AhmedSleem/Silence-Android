/**
 * Copyright (C) 2015 Open Whisper Systems
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
import android.content.Intent;
import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import org.smssecure.smssecure.MediaAdapter.ViewHolder;
import org.smssecure.smssecure.components.ThumbnailView;
import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.database.CursorRecyclerViewAdapter;
import org.smssecure.smssecure.database.MediaDatabase.MediaRecord;
import org.smssecure.smssecure.mms.Slide;
import org.smssecure.smssecure.recipients.RecipientFactory;
import org.smssecure.smssecure.recipients.Recipients;
import org.smssecure.smssecure.util.MediaUtil;

public class MediaAdapter extends CursorRecyclerViewAdapter<ViewHolder> {
    private static final String TAG = MediaAdapter.class.getSimpleName();

    private final MasterSecret masterSecret;
    private final long threadId;

    public MediaAdapter(Context context, MasterSecret masterSecret, Cursor c, long threadId) {
        super(context, c);
        this.masterSecret = masterSecret;
        this.threadId = threadId;
    }

    @Override
    public ViewHolder onCreateItemViewHolder(final ViewGroup viewGroup, final int i) {
        final View view = LayoutInflater.from(getContext()).inflate(R.layout.media_overview_item, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindItemViewHolder(final ViewHolder viewHolder, final @NonNull Cursor cursor) {
        final ThumbnailView imageView = viewHolder.imageView;
        final MediaRecord mediaRecord = MediaRecord.from(cursor);

        Slide slide = MediaUtil.getSlideForAttachment(getContext(), mediaRecord.getAttachment());

        if (slide != null) {
            imageView.setImageResource(masterSecret, slide, false);
        }

        imageView.setOnClickListener(new OnMediaClickListener(mediaRecord));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ThumbnailView imageView;

        public ViewHolder(View v) {
            super(v);
            imageView = v.findViewById(R.id.image);
        }
    }

    private class OnMediaClickListener implements OnClickListener {
        private final MediaRecord mediaRecord;

        private OnMediaClickListener(MediaRecord mediaRecord) {
            this.mediaRecord = mediaRecord;
        }

        @Override
        public void onClick(View v) {
            if (mediaRecord.getAttachment().getDataUri() != null) {
                Intent intent = new Intent(getContext(), MediaPreviewActivity.class);
                intent.putExtra(MediaPreviewActivity.DATE_EXTRA, mediaRecord.getDate());
                intent.putExtra(MediaPreviewActivity.SIZE_EXTRA, mediaRecord.getAttachment().getSize());
                intent.putExtra(MediaPreviewActivity.THREAD_ID_EXTRA, threadId);

                if (!TextUtils.isEmpty(mediaRecord.getAddress())) {
                    Recipients recipients = RecipientFactory.getRecipientsFromString(getContext(),
                            mediaRecord.getAddress(),
                            true);
                    if (recipients != null && recipients.getPrimaryRecipient() != null) {
                        intent.putExtra(MediaPreviewActivity.RECIPIENT_EXTRA, recipients.getPrimaryRecipient().getRecipientId());
                    }
                }
                intent.setDataAndType(mediaRecord.getAttachment().getDataUri(), mediaRecord.getContentType());
                getContext().startActivity(intent);
            }
        }
    }
}
