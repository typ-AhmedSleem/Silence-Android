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
package org.smssecure.smssecure.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import org.smssecure.smssecure.ApplicationContext;
import org.smssecure.smssecure.attachments.Attachment;
import org.smssecure.smssecure.attachments.AttachmentId;
import org.smssecure.smssecure.attachments.DatabaseAttachment;
import org.smssecure.smssecure.crypto.DecryptingPartInputStream;
import org.smssecure.smssecure.crypto.EncryptingPartOutputStream;
import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.mms.MediaStream;
import org.smssecure.smssecure.mms.MmsException;
import org.smssecure.smssecure.mms.PartAuthority;
import org.smssecure.smssecure.util.MediaUtil;
import org.smssecure.smssecure.util.MediaUtil.ThumbnailData;
import org.smssecure.smssecure.util.Util;
import org.smssecure.smssecure.video.EncryptedMediaDataSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

public class AttachmentDatabase extends Database {

    public static final int TRANSFER_PROGRESS_DONE = 0;
    public static final int TRANSFER_PROGRESS_STARTED = 1;
    public static final int TRANSFER_PROGRESS_AUTO_PENDING = 2;
    public static final int TRANSFER_PROGRESS_FAILED = 3;
    static final String TABLE_NAME = "part";
    static final String ROW_ID = "_id";
    static final String ATTACHMENT_ID_ALIAS = "attachment_id";
    static final String MMS_ID = "mid";
    static final String CONTENT_TYPE = "ct";
    static final String NAME = "name";
    static final String CONTENT_DISPOSITION = "cd";
    static final String CONTENT_LOCATION = "cl";
    static final String DATA = "_data";
    static final String TRANSFER_STATE = "pending_push";
    public static final String[] CREATE_INDEXS = {
            "CREATE INDEX IF NOT EXISTS part_mms_id_index ON " + TABLE_NAME + " (" + MMS_ID + ");",
            "CREATE INDEX IF NOT EXISTS pending_push_index ON " + TABLE_NAME + " (" + TRANSFER_STATE + ");",
    };
    static final String SIZE = "data_size";
    static final String THUMBNAIL = "thumbnail";
    static final String THUMBNAIL_ASPECT_RATIO = "aspect_ratio";
    static final String UNIQUE_ID = "unique_id";
    static final String DIGEST = "digest";
    public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" + ROW_ID + " INTEGER PRIMARY KEY, " +
            MMS_ID + " INTEGER, " + "seq" + " INTEGER DEFAULT 0, " +
            CONTENT_TYPE + " TEXT, " + NAME + " TEXT, " + "chset" + " INTEGER, " +
            CONTENT_DISPOSITION + " TEXT, " + "fn" + " TEXT, " + "cid" + " TEXT, " +
            CONTENT_LOCATION + " TEXT, " + "ctt_s" + " INTEGER, " +
            "ctt_t" + " TEXT, " + "encrypted" + " INTEGER, " +
            TRANSFER_STATE + " INTEGER, " + DATA + " TEXT, " + SIZE + " INTEGER, " +
            THUMBNAIL + " TEXT, " + THUMBNAIL_ASPECT_RATIO + " REAL, " + UNIQUE_ID + " INTEGER NOT NULL, " +
            DIGEST + " BLOB);";
    private static final String TAG = AttachmentDatabase.class.getSimpleName();
    private static final String PART_ID_WHERE = ROW_ID + " = ? AND " + UNIQUE_ID + " = ?";
    private static final String[] PROJECTION = new String[]{ROW_ID + " AS " + ATTACHMENT_ID_ALIAS,
            MMS_ID, CONTENT_TYPE, NAME, CONTENT_DISPOSITION,
            CONTENT_LOCATION, DATA, THUMBNAIL, TRANSFER_STATE,
            SIZE, THUMBNAIL, THUMBNAIL_ASPECT_RATIO,
            UNIQUE_ID, DIGEST};
    private final ExecutorService thumbnailExecutor = Util.newSingleThreadedLifoExecutor();

    public AttachmentDatabase(Context context, SQLiteOpenHelper databaseHelper) {
        super(context, databaseHelper);
    }

    public @NonNull InputStream getAttachmentStream(MasterSecret masterSecret, AttachmentId attachmentId)
            throws IOException {
        InputStream dataStream = getDataStream(masterSecret, attachmentId, DATA);

        if (dataStream == null) throw new IOException("No stream for: " + attachmentId);
        else return dataStream;
    }

    public @NonNull InputStream getThumbnailStream(@NonNull MasterSecret masterSecret, @NonNull AttachmentId attachmentId)
            throws IOException {
        Log.w(TAG, "getThumbnailStream(" + attachmentId + ")");
        InputStream dataStream = getDataStream(masterSecret, attachmentId, THUMBNAIL);

        if (dataStream != null) {
            return dataStream;
        }

        try {
            InputStream generatedStream = thumbnailExecutor.submit(new ThumbnailFetchCallable(masterSecret, attachmentId)).get();

            if (generatedStream == null)
                throw new FileNotFoundException("No thumbnail stream available: " + attachmentId);
            else return generatedStream;
        } catch (InterruptedException ie) {
            throw new AssertionError("interrupted");
        } catch (ExecutionException ee) {
            Log.w(TAG, ee);
            throw new IOException(ee);
        }
    }

    public void setTransferProgressFailed(AttachmentId attachmentId, long mmsId)
            throws MmsException {
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TRANSFER_STATE, TRANSFER_PROGRESS_FAILED);

        database.update(TABLE_NAME, values, PART_ID_WHERE, attachmentId.toStrings());
        notifyConversationListeners(DatabaseFactory.getMmsDatabase(context).getThreadIdForMessage(mmsId));
    }

    @VisibleForTesting
    public @Nullable DatabaseAttachment getAttachment(AttachmentId attachmentId) {
        SQLiteDatabase database = databaseHelper.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = database.query(TABLE_NAME, PROJECTION, PART_ID_WHERE, attachmentId.toStrings(), null, null, null);

            if (cursor != null && cursor.moveToFirst()) return getAttachment(cursor);
            else return null;

        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    public @NonNull List<DatabaseAttachment> getAttachmentsForMessage(long mmsId) {
        SQLiteDatabase database = databaseHelper.getReadableDatabase();
        List<DatabaseAttachment> results = new LinkedList<>();
        Cursor cursor = null;

        try {
            cursor = database.query(TABLE_NAME, PROJECTION, MMS_ID + " = ?", new String[]{String.valueOf(mmsId)},
                    null, null, null);

            while (cursor != null && cursor.moveToNext()) {
                results.add(getAttachment(cursor));
            }

            return results;
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    public @NonNull List<DatabaseAttachment> getPendingAttachments() {
        final SQLiteDatabase database = databaseHelper.getReadableDatabase();
        final List<DatabaseAttachment> attachments = new LinkedList<>();

        Cursor cursor = null;
        try {
            cursor = database.query(TABLE_NAME, PROJECTION, TRANSFER_STATE + " = ?", new String[]{String.valueOf(TRANSFER_PROGRESS_STARTED)}, null, null, null);
            while (cursor != null && cursor.moveToNext()) {
                attachments.add(getAttachment(cursor));
            }
        } finally {
            if (cursor != null) cursor.close();
        }

        return attachments;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void deleteAttachmentsForMessage(long mmsId) {
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        Cursor cursor = null;

        try {
            cursor = database.query(TABLE_NAME, new String[]{DATA, THUMBNAIL}, MMS_ID + " = ?",
                    new String[]{String.valueOf(mmsId)}, null, null, null);

            while (cursor != null && cursor.moveToNext()) {
                String data = cursor.getString(0);
                String thumbnail = cursor.getString(1);

                if (!TextUtils.isEmpty(data)) {
                    new File(data).delete();
                }

                if (!TextUtils.isEmpty(thumbnail)) {
                    new File(thumbnail).delete();
                }
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }

        database.delete(TABLE_NAME, MMS_ID + " = ?", new String[]{String.valueOf(mmsId)});
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void deleteAllAttachments() {
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        database.delete(TABLE_NAME, null, null);

        File attachmentsDirectory = context.getDir("parts", Context.MODE_PRIVATE);
        File[] attachments = attachmentsDirectory.listFiles();

        for (File attachment : attachments) {
            attachment.delete();
        }
    }

    void insertAttachmentsForMessage(@NonNull MasterSecret masterSecret,
                                     long mmsId,
                                     @NonNull List<Attachment> attachments)
            throws MmsException {
        Log.w(TAG, "insertParts(" + attachments.size() + ")");

        for (Attachment attachment : attachments) {
            AttachmentId attachmentId = insertAttachment(masterSecret, mmsId, attachment);
            Log.w(TAG, "Inserted attachment at ID: " + attachmentId);
        }
    }

    public @NonNull Attachment updateAttachmentData(@NonNull MasterSecret masterSecret,
                                                    @NonNull Attachment attachment,
                                                    @NonNull MediaStream mediaStream)
            throws MmsException {
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        DatabaseAttachment databaseAttachment = (DatabaseAttachment) attachment;
        File dataFile = getAttachmentDataFile(databaseAttachment.getAttachmentId(), DATA);

        if (dataFile == null) {
            throw new MmsException("No attachment data found!");
        }

        Pair<Long, byte[]> dataSizeAndDigest = setAttachmentData(masterSecret, dataFile, mediaStream.getStream());

        ContentValues contentValues = new ContentValues();
        contentValues.put(SIZE, dataSizeAndDigest.first);
        contentValues.put(DIGEST, dataSizeAndDigest.second);
        contentValues.put(CONTENT_TYPE, mediaStream.getMimeType());

        database.update(TABLE_NAME, contentValues, PART_ID_WHERE, databaseAttachment.getAttachmentId().toStrings());

        return new DatabaseAttachment(databaseAttachment.getAttachmentId(),
                databaseAttachment.getMmsId(),
                databaseAttachment.hasData(),
                databaseAttachment.hasThumbnail(),
                mediaStream.getMimeType(),
                databaseAttachment.getTransferState(),
                dataSizeAndDigest.first,
                databaseAttachment.getLocation(),
                databaseAttachment.getKey(),
                databaseAttachment.getRelay(),
                dataSizeAndDigest.second);
    }


    public void markAttachmentUploaded(long messageId, Attachment attachment) {
        ContentValues values = new ContentValues(1);
        SQLiteDatabase database = databaseHelper.getWritableDatabase();

        values.put(TRANSFER_STATE, TRANSFER_PROGRESS_DONE);
        database.update(TABLE_NAME, values, PART_ID_WHERE, ((DatabaseAttachment) attachment).getAttachmentId().toStrings());

        notifyConversationListeners(DatabaseFactory.getMmsDatabase(context).getThreadIdForMessage(messageId));
    }

    public void setTransferState(long messageId, @NonNull Attachment attachment, int transferState) {
        if (!(attachment instanceof DatabaseAttachment)) {
            throw new AssertionError("Attempt to update attachment that doesn't belong to DB!");
        }

        setTransferState(messageId, ((DatabaseAttachment) attachment).getAttachmentId(), transferState);
    }

    public void setTransferState(long messageId, @NonNull AttachmentId attachmentId, int transferState) {
        final ContentValues values = new ContentValues(1);
        final SQLiteDatabase database = databaseHelper.getWritableDatabase();

        values.put(TRANSFER_STATE, transferState);
        database.update(TABLE_NAME, values, PART_ID_WHERE, attachmentId.toStrings());
        notifyConversationListeners(DatabaseFactory.getMmsDatabase(context).getThreadIdForMessage(messageId));
        ApplicationContext.getInstance(context).notifyMediaControlEvent();
    }

    @VisibleForTesting
    @Nullable
    InputStream getDataStream(MasterSecret masterSecret, AttachmentId attachmentId, String dataType) {
        File dataFile = getAttachmentDataFile(attachmentId, dataType);

        byte[] digest = (!dataType.equals(THUMBNAIL)) ? getAttachment(attachmentId).getDigest() : null;

        try {
            if (dataFile != null)
                return new DecryptingPartInputStream(dataFile, masterSecret, digest);
            else return null;
        } catch (FileNotFoundException e) {
            Log.w(TAG, e);
            return null;
        }
    }

    private @Nullable File getAttachmentDataFile(@NonNull AttachmentId attachmentId,
                                                 @NonNull String dataType) {
        SQLiteDatabase database = databaseHelper.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = database.query(TABLE_NAME, new String[]{dataType}, PART_ID_WHERE, attachmentId.toStrings(),
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                if (cursor.isNull(0)) {
                    return null;
                }

                return new File(cursor.getString(0));
            } else {
                return null;
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }

    }

    private @NonNull Pair<File, Pair<Long, byte[]>> setAttachmentData(@NonNull MasterSecret masterSecret,
                                                                      @NonNull Uri uri)
            throws MmsException {
        try {
            InputStream inputStream = PartAuthority.getAttachmentStream(context, masterSecret, uri);
            return setAttachmentData(masterSecret, inputStream);
        } catch (IOException e) {
            throw new MmsException(e);
        }
    }

    private @NonNull Pair<File, Pair<Long, byte[]>> setAttachmentData(@NonNull MasterSecret masterSecret,
                                                                      @NonNull InputStream in)
            throws MmsException {
        try {
            File partsDirectory = context.getDir("parts", Context.MODE_PRIVATE);
            File dataFile = File.createTempFile("part", ".mms", partsDirectory);

            return new Pair<>(dataFile, setAttachmentData(masterSecret, dataFile, in));
        } catch (IOException e) {
            throw new MmsException(e);
        }
    }

    private @NonNull Pair<Long, byte[]> setAttachmentData(@NonNull MasterSecret masterSecret,
                                                          @NonNull File destination,
                                                          @NonNull InputStream in)
            throws MmsException {
        try {
            EncryptingPartOutputStream out = new EncryptingPartOutputStream(destination, masterSecret);
            return new Pair<>(Util.copy(in, out), out.getAttachmentDigest());
        } catch (IOException e) {
            throw new MmsException(e);
        }
    }

    DatabaseAttachment getAttachment(Cursor cursor) {
        return new DatabaseAttachment(new AttachmentId(cursor.getLong(cursor.getColumnIndexOrThrow(ATTACHMENT_ID_ALIAS)),
                cursor.getLong(cursor.getColumnIndexOrThrow(UNIQUE_ID))),
                cursor.getLong(cursor.getColumnIndexOrThrow(MMS_ID)),
                !cursor.isNull(cursor.getColumnIndexOrThrow(DATA)),
                !cursor.isNull(cursor.getColumnIndexOrThrow(THUMBNAIL)),
                cursor.getString(cursor.getColumnIndexOrThrow(CONTENT_TYPE)),
                cursor.getInt(cursor.getColumnIndexOrThrow(TRANSFER_STATE)),
                cursor.getLong(cursor.getColumnIndexOrThrow(SIZE)),
                cursor.getString(cursor.getColumnIndexOrThrow(CONTENT_LOCATION)),
                cursor.getString(cursor.getColumnIndexOrThrow(CONTENT_DISPOSITION)),
                cursor.getString(cursor.getColumnIndexOrThrow(NAME)),
                cursor.getBlob(cursor.getColumnIndexOrThrow(DIGEST)));
    }


    private AttachmentId insertAttachment(MasterSecret masterSecret, long mmsId, Attachment attachment)
            throws MmsException {
        Log.w(TAG, "Inserting attachment for mms id: " + mmsId);

        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        Pair<File, Pair<Long, byte[]>> partData = null;
        long uniqueId = System.currentTimeMillis();

        if (masterSecret != null && attachment.getDataUri() != null) {
            partData = setAttachmentData(masterSecret, attachment.getDataUri());
            Log.w(TAG, "Wrote part to file: " + partData.first.getAbsolutePath());
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put(MMS_ID, mmsId);
        contentValues.put(CONTENT_TYPE, attachment.getContentType());
        contentValues.put(TRANSFER_STATE, attachment.getTransferState());
        contentValues.put(UNIQUE_ID, uniqueId);
        contentValues.put(CONTENT_LOCATION, attachment.getLocation());
        contentValues.put(DIGEST, attachment.getDigest());
        contentValues.put(CONTENT_DISPOSITION, attachment.getKey());
        contentValues.put(NAME, attachment.getRelay());

        if (partData != null) {
            contentValues.put(DATA, partData.first.getAbsolutePath());
            contentValues.put(SIZE, partData.second.first);
            contentValues.put(DIGEST, partData.second.second);
        }

        long rowId = database.insert(TABLE_NAME, null, contentValues);
        AttachmentId attachmentId = new AttachmentId(rowId, uniqueId);

        if (partData != null) {
            Log.w(TAG, "Submitting thumbnail generation job...");
            thumbnailExecutor.submit(new ThumbnailFetchCallable(masterSecret, attachmentId));
        }

        return attachmentId;
    }


    @VisibleForTesting
    void updateAttachmentThumbnail(MasterSecret masterSecret, AttachmentId attachmentId, InputStream in, float aspectRatio)
            throws MmsException {
        Log.w(TAG, "updating part thumbnail for #" + attachmentId);

        File thumbnailFile = setAttachmentData(masterSecret, in).first;

        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues(2);

        values.put(THUMBNAIL, thumbnailFile.getAbsolutePath());
        values.put(THUMBNAIL_ASPECT_RATIO, aspectRatio);

        database.update(TABLE_NAME, values, PART_ID_WHERE, attachmentId.toStrings());

        Cursor cursor = database.query(TABLE_NAME, new String[]{MMS_ID}, PART_ID_WHERE, attachmentId.toStrings(), null, null, null);

        try {
            if (cursor != null && cursor.moveToFirst()) {
                notifyConversationListeners(DatabaseFactory.getMmsDatabase(context).getThreadIdForMessage(cursor.getLong(cursor.getColumnIndexOrThrow(MMS_ID))));
            }
        } finally {
            if (cursor != null) cursor.close();
        }
    }


    @VisibleForTesting
    class ThumbnailFetchCallable implements Callable<InputStream> {

        private final MasterSecret masterSecret;
        private final AttachmentId attachmentId;

        ThumbnailFetchCallable(MasterSecret masterSecret, AttachmentId attachmentId) {
            this.masterSecret = masterSecret;
            this.attachmentId = attachmentId;
        }

        @Override
        public @Nullable InputStream call() throws Exception {
            Log.w(TAG, "Executing thumbnail job...");
            final InputStream stream = getDataStream(masterSecret, attachmentId, THUMBNAIL);

            if (stream != null) {
                return stream;
            }

            DatabaseAttachment attachment = getAttachment(attachmentId);

            if (attachment == null || !attachment.hasData()) {
                return null;
            }

            ThumbnailData data;

            if (MediaUtil.isVideoType(attachment.getContentType())) {
                data = generateVideoThumbnail(masterSecret, attachmentId);
            } else {
                data = MediaUtil.generateThumbnail(context, masterSecret, attachment.getContentType(), attachment.getDataUri());
            }

            if (data == null) {
                return null;
            }

            updateAttachmentThumbnail(masterSecret, attachmentId, data.toDataStream(), data.getAspectRatio());

            return getDataStream(masterSecret, attachmentId, THUMBNAIL);
        }

        private ThumbnailData generateVideoThumbnail(MasterSecret masterSecret, AttachmentId attachmentId) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                Log.w(TAG, "Video thumbnails not supported...");
                return null;
            }

            File mediaFile = getAttachmentDataFile(attachmentId, DATA);

            if (mediaFile == null) {
                Log.w(TAG, "No data file found for video thumbnail...");
                return null;
            }

            EncryptedMediaDataSource dataSource = new EncryptedMediaDataSource(masterSecret, mediaFile);
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(dataSource);

            Bitmap bitmap = retriever.getFrameAtTime(1000);

            Log.w(TAG, "Generated video thumbnail...");
            return new ThumbnailData(bitmap);
        }
    }
}
