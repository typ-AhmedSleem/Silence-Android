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

import org.smssecure.smssecure.R;
import org.smssecure.smssecure.attachments.Attachment;
import org.smssecure.smssecure.util.MediaUtil;
import org.smssecure.smssecure.util.ResUtil;

import java.io.IOException;

public class VideoSlide extends Slide {

    public VideoSlide(Context context, Uri uri, long dataSize) throws IOException {
        super(context, constructAttachmentFromUri(context, uri, MediaUtil.VIDEO_UNSPECIFIED, dataSize, false));
    }

    public VideoSlide(Context context, Attachment attachment) {
        super(context, attachment);
    }

    @Override
    public boolean hasPlaceholder() {
        return true;
    }

    @Override
    public boolean hasPlayOverlay() {
        return true;
    }

    @Override
    public @DrawableRes int getPlaceholderRes(Theme theme) {
        return ResUtil.getDrawableRes(theme, R.attr.conversation_icon_attach_video);
    }

    @Override
    public boolean hasImage() {
        return true;
    }

    @Override
    public boolean hasVideo() {
        return true;
    }

    @NonNull
    @Override
    public String getContentDescription() {
        return context.getString(R.string.Slide_video);
    }
}
