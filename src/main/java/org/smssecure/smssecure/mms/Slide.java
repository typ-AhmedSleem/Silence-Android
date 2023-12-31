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
package org.smssecure.smssecure.mms;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.net.Uri;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.smssecure.smssecure.attachments.Attachment;
import org.smssecure.smssecure.attachments.UriAttachment;
import org.smssecure.smssecure.database.AttachmentDatabase;
import org.smssecure.smssecure.util.MediaUtil;
import org.smssecure.smssecure.util.Util;
import org.whispersystems.libsignal.util.guava.Optional;

public abstract class Slide {

    protected final Attachment attachment;
    protected final Context context;

    public Slide(@NonNull Context context, @NonNull Attachment attachment) {
        this.context = context;
        this.attachment = attachment;

    }

    protected static Attachment constructAttachmentFromUri(@NonNull Context context,
                                                           @NonNull Uri uri,
                                                           @NonNull String defaultMime,
                                                           long size,
                                                           boolean hasThumbnail) {
        Optional<String> resolvedType = Optional.fromNullable(MediaUtil.getMimeType(context, uri));
        return new UriAttachment(uri, hasThumbnail ? uri : null, resolvedType.or(defaultMime), AttachmentDatabase.TRANSFER_PROGRESS_STARTED, size);
    }

    public String getContentType() {
        return attachment.getContentType();
    }

    @Nullable
    public Uri getUri() {
        return attachment.getDataUri();
    }

    @Nullable
    public Uri getThumbnailUri() {
        return attachment.getThumbnailUri();
    }

    @NonNull
    public Optional<String> getBody() {
        return Optional.absent();
    }

    public boolean hasImage() {
        return false;
    }

    public boolean hasVideo() {
        return false;
    }

    public boolean hasAudio() {
        return false;
    }

    public @NonNull String getContentDescription() {
        return "";
    }

    public Attachment asAttachment() {
        return attachment;
    }

    public boolean isInProgress() {
        return attachment.isInProgress();
    }

    public boolean isPendingDownload() {
        return getTransferState() == AttachmentDatabase.TRANSFER_PROGRESS_FAILED ||
                getTransferState() == AttachmentDatabase.TRANSFER_PROGRESS_AUTO_PENDING;
    }

    public long getTransferState() {
        return attachment.getTransferState();
    }

    public @DrawableRes int getPlaceholderRes(Theme theme) {
        throw new AssertionError("getPlaceholderRes() called for non-drawable slide");
    }

    public boolean hasPlaceholder() {
        return false;
    }

    public boolean hasPlayOverlay() {
        return false;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Slide)) return false;

        Slide that = (Slide) other;

        return Util.equals(this.getContentType(), that.getContentType()) &&
                this.hasAudio() == that.hasAudio() &&
                this.hasImage() == that.hasImage() &&
                this.hasVideo() == that.hasVideo() &&
                this.getTransferState() == that.getTransferState() &&
                Util.equals(this.getUri(), that.getUri()) &&
                Util.equals(this.getThumbnailUri(), that.getThumbnailUri());
    }

    @Override
    public int hashCode() {
        return Util.hashCode(getContentType(), hasAudio(), hasImage(),
                hasVideo(), getUri(), getThumbnailUri(), getTransferState());
    }
}
