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
import android.database.MergeCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.smssecure.smssecure.R;
import org.smssecure.smssecure.crypto.MasterCipher;
import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.database.model.DisplayRecord;
import org.smssecure.smssecure.database.model.MediaMmsMessageRecord;
import org.smssecure.smssecure.database.model.MessageRecord;
import org.smssecure.smssecure.database.model.ThreadRecord;
import org.smssecure.smssecure.mms.Slide;
import org.smssecure.smssecure.mms.SlideDeck;
import org.smssecure.smssecure.recipients.Recipient;
import org.smssecure.smssecure.recipients.RecipientFactory;
import org.smssecure.smssecure.recipients.Recipients;
import org.smssecure.smssecure.util.Util;
import org.whispersystems.libsignal.InvalidMessageException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ThreadDatabase extends Database {

    public static final String ID = "_id";
    public static final String DATE = "date";
    public static final String MESSAGE_COUNT = "message_count";
    public static final String RECIPIENT_IDS = "recipient_ids";
    public static final String SNIPPET = "snippet";
    public static final String READ = "read";
    public static final String TYPE = "type";
    public static final String SNIPPET_TYPE = "snippet_type";
    public static final String SNIPPET_URI = "snippet_uri";
    public static final String ARCHIVED = "archived";
    public static final String PINNED = "pinned";
    public static final String STATUS = "status";
    public static final String LAST_SEEN = "last_seen";
    public static final String SNIPPET_CHARSET = "snippet_cs";
    public static final String ERROR = "error";

    public static final String TABLE_NAME = "thread";
    public static final String[] CREATE_INDEXS = {
            "CREATE INDEX IF NOT EXISTS thread_recipient_ids_index ON " + TABLE_NAME + " (" + RECIPIENT_IDS + ");",
            "CREATE INDEX IF NOT EXISTS archived_index ON " + TABLE_NAME + " (" + ARCHIVED + ");",
    };
    public static final String CREATE_TABLE = String.format(
            "CREATE TABLE %s (%s INTEGER PRIMARY KEY, %s INTEGER DEFAULT 0, %s INTEGER DEFAULT 0, %s TEXT, %s TEXT, %s INTEGER DEFAULT 0, %s INTEGER DEFAULT 1, %s INTEGER DEFAULT 0, %s INTEGER DEFAULT 0, %s INTEGER DEFAULT 0, %s TEXT DEFAULT NULL, %s INTEGER DEFAULT 0, %s INTEGER DEFAULT 0, %s INTEGER DEFAULT 0, %s INTEGER DEFAULT 0);",
            TABLE_NAME,
            ID,
            DATE,
            MESSAGE_COUNT,
            RECIPIENT_IDS,
            SNIPPET,
            SNIPPET_CHARSET,
            READ,
            TYPE,
            ERROR,
            SNIPPET_TYPE,
            SNIPPET_URI,
            ARCHIVED,
            PINNED,
            STATUS,
            LAST_SEEN);
    private static final String TAG = ThreadDatabase.class.getSimpleName();
    private static final String ORDER_BY_CLAUSE = String.format("%s DESC, %s DESC", PINNED, DATE);

    public ThreadDatabase(Context context, SQLiteOpenHelper databaseHelper) {
        super(context, databaseHelper);
    }

    private long[] getRecipientIds(Recipients recipients) {
        Set<Long> recipientSet = new HashSet<>();
        List<Recipient> recipientList = recipients.getRecipientsList();

        for (Recipient recipient : recipientList) {
            recipientSet.add(recipient.getRecipientId());
        }

        long[] recipientArray = new long[recipientSet.size()];
        int i = 0;

        for (Long recipientId : recipientSet) {
            recipientArray[i++] = recipientId;
        }

        Arrays.sort(recipientArray);

        return recipientArray;
    }

    private String getRecipientsAsString(long[] recipientIds) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < recipientIds.length; i++) {
            if (i != 0) sb.append(' ');
            sb.append(recipientIds[i]);
        }

        return sb.toString();
    }

    private long createThreadForRecipients(String recipients, int recipientCount, int distributionType) {
        ContentValues contentValues = new ContentValues(4);
        long date = System.currentTimeMillis();

        contentValues.put(DATE, date - date % 1000);
        contentValues.put(RECIPIENT_IDS, recipients);

        if (recipientCount > 1) {
            contentValues.put(TYPE, distributionType);
        }

        contentValues.put(MESSAGE_COUNT, 0);

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        return db.insert(TABLE_NAME, null, contentValues);
    }

    private void updateThread(
            long threadId,
            long count,
            String body,
            @Nullable Uri attachment,
            long date,
            int status,
            long type,
            boolean unarchive) {
        ContentValues contentValues = new ContentValues(6);
        contentValues.put(DATE, date - date % 1000);
        contentValues.put(MESSAGE_COUNT, count);
        contentValues.put(SNIPPET, body);
        contentValues.put(SNIPPET_URI, attachment == null ? null : attachment.toString());
        contentValues.put(SNIPPET_TYPE, type);
        contentValues.put(STATUS, status);

        if (unarchive) contentValues.put(ARCHIVED, 0);

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.update(TABLE_NAME, contentValues, ID + " = ?", new String[]{String.valueOf(threadId)});
        notifyConversationListListeners();
    }

    public void updateSnippet(long threadId, String snippet, @Nullable Uri attachment, long date, long type, boolean unarchive) {
        ContentValues contentValues = new ContentValues(4);

        contentValues.put(DATE, date - date % 1000);
        contentValues.put(SNIPPET, snippet);
        contentValues.put(SNIPPET_TYPE, type);
        contentValues.put(SNIPPET_URI, attachment == null ? null : attachment.toString());

        if (unarchive) {
            contentValues.put(ARCHIVED, 0);
        }

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.update(TABLE_NAME, contentValues, ID + " = ?", new String[]{String.valueOf(threadId)});
        notifyConversationListListeners();
    }

    private void deleteThread(long threadId) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.delete(TABLE_NAME, ID_WHERE, new String[]{String.valueOf(threadId)});
        notifyConversationListListeners();
    }

    private void deleteThreads(Set<Long> threadIds) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        StringBuilder where = new StringBuilder();

        for (long threadId : threadIds) {
            where.append(ID + " = '").append(threadId).append("' OR ");
        }

        where = new StringBuilder(where.substring(0, where.length() - 4));

        db.delete(TABLE_NAME, where.toString(), null);
        notifyConversationListListeners();
    }

    private void deleteAllThreads() {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.delete(TABLE_NAME, null, null);
        notifyConversationListListeners();
    }

    public void trimAllThreads(int length, ProgressListener listener) {
        Cursor cursor = null;
        int threadCount = 0;
        int complete = 0;

        try {
            cursor = this.getConversationList();

            if (cursor != null) {
                threadCount = cursor.getCount();
            }

            while (cursor != null && cursor.moveToNext()) {
                long threadId = cursor.getLong(cursor.getColumnIndexOrThrow(ID));
                trimThread(threadId, length);

                listener.onProgress(++complete, threadCount);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void trimThread(long threadId, int length) {
        Log.w("ThreadDatabase", "Trimming thread: " + threadId + " to: " + length);
        Cursor cursor = null;

        try {
            cursor = DatabaseFactory.getMmsSmsDatabase(context).getConversation(threadId);

            if (cursor != null && length > 0 && cursor.getCount() > length) {
                Log.w("ThreadDatabase", "Cursor count is greater than length!");
                cursor.moveToPosition(length - 1);

                long lastTweetDate = cursor.getLong(cursor.getColumnIndexOrThrow(MmsSmsColumns.NORMALIZED_DATE_RECEIVED));

                Log.w("ThreadDatabase", "Cut off tweet date: " + lastTweetDate);

                DatabaseFactory.getSmsDatabase(context).deleteMessagesInThreadBeforeDate(threadId, lastTweetDate);
                DatabaseFactory.getMmsDatabase(context).deleteMessagesInThreadBeforeDate(threadId, lastTweetDate);

                update(threadId, false);
                notifyConversationListeners(threadId);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void setAllThreadsRead() {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues(1);
        contentValues.put(READ, 1);

        db.update(TABLE_NAME, contentValues, null, null);

        DatabaseFactory.getSmsDatabase(context).setAllMessagesRead();
        DatabaseFactory.getMmsDatabase(context).setAllMessagesRead();
        notifyConversationListListeners();
    }

    public void setRead(long threadId) {
        ContentValues contentValues = new ContentValues(1);
        contentValues.put(READ, 1);

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.update(TABLE_NAME, contentValues, ID_WHERE, new String[]{String.valueOf(threadId)});

        DatabaseFactory.getSmsDatabase(context).setMessagesRead(threadId);
        DatabaseFactory.getMmsDatabase(context).setMessagesRead(threadId);
        notifyConversationListListeners();
    }

    public void setUnread(long threadId) {
        ContentValues contentValues = new ContentValues(1);
        contentValues.put(READ, 0);

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.update(TABLE_NAME, contentValues, ID_WHERE, new String[]{String.valueOf(threadId)});
        notifyConversationListListeners();
    }

    public void setDistributionType(long threadId, int distributionType) {
        ContentValues contentValues = new ContentValues(1);
        contentValues.put(TYPE, distributionType);

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.update(TABLE_NAME, contentValues, ID_WHERE, new String[]{String.valueOf(threadId)});
        notifyConversationListListeners();
    }

    /**
     * RETURNS WRONG RESULTS SOMETIMES
     */
    public Cursor getFilteredConversationList(List<String> query) {
        if (query == null || query.size() == 0) {
            return null;
        }

        List<Long> rawRecipientIds = DatabaseFactory.getAddressDatabase(context).getCanonicalAddressIds(query);

        if (rawRecipientIds.size() == 0) return null;

        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        List<List<Long>> partitionedRecipientIds = Util.partition(rawRecipientIds, 900);
        List<Cursor> cursors = new LinkedList<>();

        for (List<Long> recipientIds : partitionedRecipientIds) {
            StringBuilder selection = new StringBuilder(RECIPIENT_IDS + " = ?");
            String[] selectionArgs = new String[recipientIds.size()];

            for (int i = 0; i < recipientIds.size() - 1; i++)
                selection.append(" OR " + RECIPIENT_IDS + " = ?");

            int i = 0;
            for (long id : recipientIds) {
                selectionArgs[i++] = String.valueOf(id);
            }

            cursors.add(db.query(TABLE_NAME, null, selection.toString(), selectionArgs, null, null, ORDER_BY_CLAUSE));
        }

        Cursor cursor = cursors.size() > 1 ? new MergeCursor(cursors.toArray(new Cursor[cursors.size()])) : cursors.get(0);

        setNotifyConverationListListeners(cursor);
        return cursor;
    }

    public Cursor getConversationList() {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, ARCHIVED + " = ?", new String[]{"0"}, null, null, ORDER_BY_CLAUSE);

        setNotifyConverationListListeners(cursor);

        return cursor;
    }

    public Cursor getArchivedConversationList() {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, ARCHIVED + " = ?", new String[]{"1"}, null, null, DATE + " DESC");

        setNotifyConverationListListeners(cursor);

        return cursor;
    }

    public Cursor getDirectShareList() {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        return db.query(TABLE_NAME, null, null, null, null, null, DATE + " DESC");
    }

    public int getArchivedConversationListCount() {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(TABLE_NAME, new String[]{"COUNT(*)"}, ARCHIVED + " = ?", new String[]{"1"}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }

        } finally {
            if (cursor != null) cursor.close();
        }

        return 0;
    }

    public void archiveConversation(long threadId) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues(1);
        contentValues.put(ARCHIVED, 1);

        db.update(TABLE_NAME, contentValues, ID_WHERE, new String[]{String.valueOf(threadId)});
        notifyConversationListListeners();
    }

    public void unarchiveConversation(long threadId) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues(1);
        contentValues.put(ARCHIVED, 0);

        db.update(TABLE_NAME, contentValues, ID_WHERE, new String[]{String.valueOf(threadId)});
        notifyConversationListListeners();
    }

    public boolean isThreadPinned(long threadId) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        try (Cursor cursor = db.rawQuery(
                "SELECT " + PINNED + " FROM " + TABLE_NAME + " WHERE " + ID + " = " + threadId,
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(0) != 0;
            }
        }
        return false;
    }

    public Cursor loadAllDistinctThreads() {
        final SQLiteDatabase db = databaseHelper.getReadableDatabase();
        return db.rawQuery("SELECT DISTINCT " + ID + ", " + ARCHIVED + " FROM " + TABLE_NAME + " WHERE " + ARCHIVED + " = 0", null);
    }

    private List<Long> resolveIdsFromThreads(@NonNull Cursor cursor) {
        final List<Long> ids = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            ids.add(cursor.getLong(cursor.getColumnIndexOrThrow(ID)));
            cursor.moveToNext();
        }
        cursor.close();
        return ids;
    }

    private String buildEnhancedFilterSQL(Map<Long, Long> matches, int limit) {
        // Selection
        StringBuilder THREADS_IDS = new StringBuilder();
        StringBuilder MESSAGES_IDS = new StringBuilder();
        for (Long msgId : matches.keySet()) {
            THREADS_IDS.append(matches.get(msgId)).append(",");
            MESSAGES_IDS.append(msgId).append(",");
        }
        // delete the comma at the end of statement
        THREADS_IDS.deleteCharAt(THREADS_IDS.length() - 1);
        MESSAGES_IDS.deleteCharAt(MESSAGES_IDS.length() - 1);

        // Limit
        String LIMIT_CLAUSE = "";
        if (limit > 0) LIMIT_CLAUSE = " LIMIT " + limit;

        return "SELECT thr." + ID + ", " +
                "thr." + DATE + ", " +
                "thr." + MESSAGE_COUNT + ", " +
                "thr." + RECIPIENT_IDS + ", " +
                "thr." + SNIPPET_CHARSET + ", " +
                "thr." + READ + ", " +
                "thr." + TYPE + ", " +
                "thr." + ERROR + ", " +
                "thr." + SNIPPET_TYPE + ", " +
                "thr." + SNIPPET_URI + ", " +
                "thr." + ARCHIVED + ", " +
                "thr." + PINNED + ", " +
                "thr." + STATUS + ", " +
                "thr." + LAST_SEEN + ", " +
                "CASE WHEN thr." + ID + " = msg." + SmsDatabase.THREAD_ID + " THEN msg." + SmsDatabase.BODY + " " +
                "ELSE thr." + SNIPPET + " END AS " + SNIPPET + ", " +
                "CASE WHEN thr." + ID + " = msg." + SmsDatabase.THREAD_ID + " THEN msg." + SmsDatabase.DATE_RECEIVED + " " +
                "ELSE thr." + DATE + " END AS " + DATE + " " +
                "FROM " + ThreadDatabase.TABLE_NAME + " thr " +
                "LEFT JOIN " + SmsDatabase.TABLE_NAME + " msg ON thr." + ID + " = msg." + SmsDatabase.THREAD_ID + " " +
                "WHERE thr." + ID + " IN " + "(SELECT " + SmsDatabase.THREAD_ID + " FROM " +
                SmsDatabase.TABLE_NAME + " WHERE " + SmsDatabase.THREAD_ID + " IN (" + THREADS_IDS + ")) AND thr." + ARCHIVED + " = 0 AND msg." + SmsDatabase.ID + " IN (" + MESSAGES_IDS + ") ORDER BY " + SmsDatabase.THREAD_ID + LIMIT_CLAUSE + ";";
    }

    public Cursor enhancedFilterThreads(Locale locale, MasterSecret secret, String ready_query, int messagesLimit) {
        if (ready_query == null || ready_query.trim().length() == 0) return null;
//        Log.i(TAG, "enhancedFilterThreads: Query => " + ready_query + " | limit => " + messagesLimit);

        final SQLiteDatabase db = databaseHelper.getReadableDatabase();

        // Load all distinct threads and resolve their ids
        final Cursor threadsCursor = loadAllDistinctThreads();
        final List<Long> idsOfThreads = resolveIdsFromThreads(threadsCursor);
//        Log.d(TAG, "enhancedFilterThreads: Resolved " + idsOfThreads.size() + " thread from " + threadsCursor.getCount() + " thread.");

        //Load all messages of each thread (CARING TO MESSAGES LIMIT FOR EACH THREAD OF COURSE)
        final List<Cursor> messagesCursors = new ArrayList<>();
        for (long threadId : idsOfThreads) {
            final Cursor msgsCursor = DatabaseFactory.getMmsSmsDatabase(context).getConversation(threadId, messagesLimit);
            messagesCursors.add(msgsCursor);
        }
//        Log.d(TAG, "enhancedFilterThreads: Retrieved " + messagesCursors.size() + " conversation from resolved threads.");

        // Filter messages
        final Map<Long, Long> matchingMessagesIds = new HashMap<>();
        for (Cursor messagesCursor : messagesCursors) {
            final EncryptingSmsDatabase.DecryptingReader msgReader = (EncryptingSmsDatabase.DecryptingReader) DatabaseFactory.getEncryptingSmsDatabase(context).readerFor(secret, messagesCursor);
            if (!messagesCursor.isClosed() && msgReader != null) {
                MessageRecord message;
                while ((message = msgReader.getNext()) != null) {
                    long threadId = message.getThreadId();
                    boolean found_match = message.getBody().getBody().toLowerCase(locale).contains(ready_query);
                    if (message.getBody().isPlaintext() && found_match) {
                        matchingMessagesIds.put(message.getId(), threadId);
//                        Log.i(TAG, "enhancedFilterThreads: Found a message that matches query with id " + message.getId() + " at thread " + threadId + " | content => " + message.getBody().getBody().replaceAll("\n", " ") + "]");
                    }
                }
            }
            messagesCursor.close();
        }
        threadsCursor.close();
//        Log.i(TAG, "enhancedFilterThreads: Filtered messages and results are contained " + matchingMessagesIds.size() + " threads with IDs [" + Arrays.toString(matchingMessagesIds.values().toArray()) + "]");

        // Get all messages by their threadIds
        if (!matchingMessagesIds.isEmpty()) {
            final String sql = buildEnhancedFilterSQL(matchingMessagesIds, 500);
            final Cursor resultCursor = db.rawQuery(sql, null);
//            Log.d(TAG, "enhancedFilterThreads: SQL => " + sql);

            setNotifyConverationListListeners(resultCursor);
            return resultCursor;

        } else return null; // No matches found.
    }

    public Cursor loadThreadMessages(long threadId, int limit) {
        return DatabaseFactory.getMmsSmsDatabase(context).getConversation(threadId, limit);
    }

    public void setThreadPin(long threadId, boolean pin) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues(1);
        contentValues.put(PINNED, pin ? 1 : 0);

        db.update(TABLE_NAME, contentValues, ID_WHERE, new String[]{String.valueOf(threadId)});
        notifyConversationListListeners();
    }

    public void toggleThreadPin(long threadId) {
        setThreadPin(threadId, !isThreadPinned(threadId));
    }

    public void setLastSeen(long threadId) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues(1);
        contentValues.put(LAST_SEEN, System.currentTimeMillis());

        db.update(TABLE_NAME, contentValues, ID_WHERE, new String[]{String.valueOf(threadId)});
        notifyConversationListListeners();
    }

    public long getLastSeen(long threadId) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        try (Cursor cursor = db.query(TABLE_NAME, new String[]{LAST_SEEN}, ID_WHERE, new String[]{String.valueOf(threadId)}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
            return -1;
        }

    }

    public void deleteConversation(long threadId) {
        DatabaseFactory.getSmsDatabase(context).deleteThread(threadId);
        DatabaseFactory.getMmsDatabase(context).deleteThread(threadId);
        DatabaseFactory.getDraftDatabase(context).clearDrafts(threadId);
        deleteThread(threadId);
        notifyConversationListeners(threadId);
        notifyConversationListListeners();
    }

    public void deleteConversations(Set<Long> selectedConversations) {
        DatabaseFactory.getSmsDatabase(context).deleteThreads(selectedConversations);
        DatabaseFactory.getMmsDatabase(context).deleteThreads(selectedConversations);
        DatabaseFactory.getDraftDatabase(context).clearDrafts(selectedConversations);
        deleteThreads(selectedConversations);
        notifyConversationListeners(selectedConversations);
        notifyConversationListListeners();
    }

    public void deleteAllConversations() {
        DatabaseFactory.getSmsDatabase(context).deleteAllThreads();
        DatabaseFactory.getMmsDatabase(context).deleteAllThreads();
        DatabaseFactory.getDraftDatabase(context).clearAllDrafts();
        deleteAllThreads();
    }

    public long getThreadIdIfExistsFor(Recipients recipients) {
        long[] recipientIds = getRecipientIds(recipients);
        String recipientsList = getRecipientsAsString(recipientIds);
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        String where = RECIPIENT_IDS + " = ?";
        String[] recipientsArg = new String[]{recipientsList};
        Cursor cursor = null;

        try {
            cursor = db.query(TABLE_NAME, new String[]{ID}, where, recipientsArg, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(ID));
            } else {
                return -1L;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public long getThreadIdFor(Recipients recipients) {
        return getThreadIdFor(recipients, DistributionTypes.DEFAULT);
    }

    public long getThreadIdFor(Recipients recipients, int distributionType) {
        long[] recipientIds = getRecipientIds(recipients);
        String recipientsList = getRecipientsAsString(recipientIds);
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        String where = RECIPIENT_IDS + " = ?";
        String[] recipientsArg = new String[]{recipientsList};
        Cursor cursor = null;

        try {
            cursor = db.query(TABLE_NAME, new String[]{ID}, where, recipientsArg, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(ID));
            } else {
                return createThreadForRecipients(recipientsList, recipientIds.length, distributionType);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public @Nullable Recipients getRecipientsForThreadId(long threadId) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(TABLE_NAME, null, ID + " = ?", new String[]{String.valueOf(threadId)}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                String recipientIds = cursor.getString(cursor.getColumnIndexOrThrow(RECIPIENT_IDS));
                return RecipientFactory.getRecipientsForIds(context, recipientIds, false);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return null;
    }

    public boolean update(long threadId, boolean unarchive) {
        MmsSmsDatabase mmsSmsDatabase = DatabaseFactory.getMmsSmsDatabase(context);
        long count = mmsSmsDatabase.getConversationCount(threadId);

        if (count == 0) {
            deleteThread(threadId);
            notifyConversationListListeners();
            return true;
        }

        MmsSmsDatabase.Reader reader = null;

        try {
            reader = mmsSmsDatabase.readerFor(mmsSmsDatabase.getConversationSnippet(threadId));
            MessageRecord record;

            if (reader != null && (record = reader.getNext()) != null) {
                updateThread(
                        threadId,
                        count,
                        record.getBody().getBody(),
                        getAttachmentUriFor(record),
                        record.getTimestamp(),
                        record.getDeliveryStatus(),
                        record.getType(),
                        unarchive);
                notifyConversationListListeners();
                return false;
            } else {
                deleteThread(threadId);
                notifyConversationListListeners();
                return true;
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    private @Nullable Uri getAttachmentUriFor(MessageRecord record) {
        if (!record.isMms() || record.isMmsNotification() || record.isGroupAction()) return null;

        SlideDeck slideDeck = ((MediaMmsMessageRecord) record).getSlideDeck();
        Slide thumbnail = slideDeck.getThumbnailSlide();

        return thumbnail != null ? thumbnail.getThumbnailUri() : null;
    }

    public Reader readerFor(Cursor cursor, MasterCipher masterCipher) {
        return new Reader(cursor, masterCipher);
    }

    public interface ProgressListener {
        void onProgress(int complete, int total);
    }

    public static class DistributionTypes {
        public static final int DEFAULT = 2;
        public static final int BROADCAST = 1;
        public static final int CONVERSATION = 2;
        public static final int ARCHIVE = 3;
    }

    public class Reader {

        private final Cursor cursor;
        private final MasterCipher masterCipher;

        public Reader(Cursor cursor, MasterCipher masterCipher) {
            this.cursor = cursor;
            this.masterCipher = masterCipher;
        }

        public ThreadRecord getNext() {
            if (cursor == null || !cursor.moveToNext()) {
                return null;
            }

            return getCurrent();
        }

        public ThreadRecord getCurrent() {
            long threadId = cursor.getLong(cursor.getColumnIndexOrThrow(ThreadDatabase.ID));
            String recipientId = cursor.getString(cursor.getColumnIndexOrThrow(ThreadDatabase.RECIPIENT_IDS));
            Recipients recipients = RecipientFactory.getRecipientsForIds(context, recipientId, true);

            DisplayRecord.Body body = getPlaintextBody(cursor);
            long date = cursor.getLong(cursor.getColumnIndexOrThrow(ThreadDatabase.DATE));
            long count = cursor.getLong(cursor.getColumnIndexOrThrow(ThreadDatabase.MESSAGE_COUNT));
            long read = cursor.getLong(cursor.getColumnIndexOrThrow(ThreadDatabase.READ));
            long type = cursor.getLong(cursor.getColumnIndexOrThrow(ThreadDatabase.SNIPPET_TYPE));
            int distributionType = cursor.getInt(cursor.getColumnIndexOrThrow(ThreadDatabase.TYPE));
            boolean archived = cursor.getInt(Math.max(cursor.getColumnIndex(ThreadDatabase.ARCHIVED), 0)) != 0;
            boolean pinned = cursor.getInt(Math.max(cursor.getColumnIndex(ThreadDatabase.PINNED), 0)) != 0;
            int status = cursor.getInt(cursor.getColumnIndexOrThrow(ThreadDatabase.STATUS));
            long lastSeen = cursor.getLong(cursor.getColumnIndexOrThrow(ThreadDatabase.LAST_SEEN));
            Uri snippetUri = getSnippetUri(cursor);

            return new ThreadRecord(context, body, snippetUri, recipients, date, count, read == 1,
                    threadId, status, type, distributionType, archived, pinned, lastSeen);
        }

        private DisplayRecord.Body getPlaintextBody(Cursor cursor) {
            try {
                long type = cursor.getLong(cursor.getColumnIndexOrThrow(ThreadDatabase.SNIPPET_TYPE));
                String body = cursor.getString(cursor.getColumnIndexOrThrow(SNIPPET));

                if (!TextUtils.isEmpty(body) && masterCipher != null && MmsSmsColumns.Types.isSymmetricEncryption(type)) {
                    return new DisplayRecord.Body(masterCipher.decryptBody(body), true);
                } else if (!TextUtils.isEmpty(body) && masterCipher == null && MmsSmsColumns.Types.isSymmetricEncryption(type)) {
                    return new DisplayRecord.Body(body, false);
                } else {
                    return new DisplayRecord.Body(body, true);
                }
            } catch (InvalidMessageException e) {
                Log.w("ThreadDatabase", e);
                return new DisplayRecord.Body(context.getString(R.string.EncryptingSmsDatabase_error_decrypting_message), true);
            }
        }

        private @Nullable Uri getSnippetUri(Cursor cursor) {
            if (cursor.isNull(cursor.getColumnIndexOrThrow(ThreadDatabase.SNIPPET_URI))) {
                return null;
            }

            try {
                return Uri.parse(cursor.getString(cursor.getColumnIndexOrThrow(ThreadDatabase.SNIPPET_URI)));
            } catch (IllegalArgumentException e) {
                Log.w(TAG, e);
                return null;
            }
        }

        public void close() {
            cursor.close();
        }
    }
}
