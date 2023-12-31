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

import com.google.android.mms.ContentType;

import org.smssecure.smssecure.R;
import org.smssecure.smssecure.attachments.Attachment;

import java.io.IOException;

public class ImageSlide extends Slide {

    private static final String TAG = ImageSlide.class.getSimpleName();

    public ImageSlide(@NonNull Context context, @NonNull Attachment attachment) {
        super(context, attachment);
    }

    public ImageSlide(Context context, Uri uri, long size) throws IOException {
        super(context, constructAttachmentFromUri(context, uri, ContentType.IMAGE_JPEG, size, true));
    }

    @Override
    public @DrawableRes int getPlaceholderRes(Theme theme) {
        return 0;
    }

    @Override
    public boolean hasImage() {
        return true;
    }

    @NonNull
    @Override
    public String getContentDescription() {
        return context.getString(R.string.Slide_image);
    }
}
