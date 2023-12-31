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
package org.smssecure.smssecure.providers;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import androidx.annotation.NonNull;
import android.util.Log;

import org.smssecure.smssecure.attachments.AttachmentId;
import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.database.DatabaseFactory;
import org.smssecure.smssecure.mms.PartUriParser;
import org.smssecure.smssecure.service.KeyCachingService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PartProvider extends ContentProvider {
    private static final String TAG = PartProvider.class.getSimpleName();

    private static final String CONTENT_URI_STRING = "content://org.smssecure.provider.smssecure/part";
    private static final Uri CONTENT_URI = Uri.parse(CONTENT_URI_STRING);
    private static final int SINGLE_ROW = 1;

    private static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI("org.smssecure.provider.smssecure", "part/*/#", SINGLE_ROW);
    }

    public static Uri getContentUri(AttachmentId attachmentId) {
        Uri uri = Uri.withAppendedPath(CONTENT_URI, String.valueOf(attachmentId.getUniqueId()));
        return ContentUris.withAppendedId(uri, attachmentId.getRowId());
    }

    @Override
    public boolean onCreate() {
        Log.w(TAG, "onCreate()");
        return true;
    }

    @SuppressWarnings("ConstantConditions")
    private File copyPartToTemporaryFile(MasterSecret masterSecret, AttachmentId attachmentId) throws IOException {
        InputStream in = DatabaseFactory.getAttachmentDatabase(getContext()).getAttachmentStream(masterSecret, attachmentId);
        File tmpDir = getContext().getDir("tmp", 0);
        File tmpFile = File.createTempFile("test", ".jpg", tmpDir);
        FileOutputStream fout = new FileOutputStream(tmpFile);

        byte[] buffer = new byte[512];
        int read;

        while ((read = in.read(buffer)) != -1)
            fout.write(buffer, 0, read);

        in.close();

        return tmpFile;
    }

    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        MasterSecret masterSecret = KeyCachingService.getMasterSecret(getContext());
        Log.w(TAG, "openFile() called!");

        if (masterSecret == null) {
            Log.w(TAG, "masterSecret was null, abandoning.");
            return null;
        }

        if (uriMatcher.match(uri) == SINGLE_ROW) {
            Log.w(TAG, "Parting out a single row...");
            try {
                PartUriParser partUri = new PartUriParser(uri);
                File tmpFile = copyPartToTemporaryFile(masterSecret, partUri.getPartId());
                ParcelFileDescriptor pdf = ParcelFileDescriptor.open(tmpFile, ParcelFileDescriptor.MODE_READ_ONLY);

                if (!tmpFile.delete()) {
                    Log.w(TAG, "Failed to delete temp file.");
                }

                return pdf;
            } catch (IOException ioe) {
                Log.w(TAG, ioe);
                throw new FileNotFoundException("Error opening file");
            }
        }

        throw new FileNotFoundException("Request for bad part.");
    }

    @Override
    public int delete(@NonNull Uri arg0, String arg1, String[] arg2) {
        return 0;
    }

    @Override
    public String getType(@NonNull Uri arg0) {
        return null;
    }

    @Override
    public Uri insert(@NonNull Uri arg0, ContentValues arg1) {
        return null;
    }

    @Override
    public Cursor query(@NonNull Uri arg0, String[] arg1, String arg2, String[] arg3, String arg4) {
        return null;
    }

    @Override
    public int update(@NonNull Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
        return 0;
    }
}
