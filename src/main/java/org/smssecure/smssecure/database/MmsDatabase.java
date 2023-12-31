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
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.google.android.mms.pdu_alt.NotificationInd;
import com.google.android.mms.pdu_alt.PduHeaders;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

import org.smssecure.smssecure.ApplicationContext;
import org.smssecure.smssecure.R;
import org.smssecure.smssecure.attachments.Attachment;
import org.smssecure.smssecure.attachments.DatabaseAttachment;
import org.smssecure.smssecure.attachments.MmsNotificationAttachment;
import org.smssecure.smssecure.crypto.MasterCipher;
import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.database.documents.IdentityKeyMismatch;
import org.smssecure.smssecure.database.documents.IdentityKeyMismatchList;
import org.smssecure.smssecure.database.documents.NetworkFailure;
import org.smssecure.smssecure.database.documents.NetworkFailureList;
import org.smssecure.smssecure.database.model.DisplayRecord;
import org.smssecure.smssecure.database.model.MediaMmsMessageRecord;
import org.smssecure.smssecure.database.model.MessageRecord;
import org.smssecure.smssecure.database.model.NotificationMmsMessageRecord;
import org.smssecure.smssecure.jobs.TrimThreadJob;
import org.smssecure.smssecure.mms.IncomingMediaMessage;
import org.smssecure.smssecure.mms.MmsException;
import org.smssecure.smssecure.mms.OutgoingMediaMessage;
import org.smssecure.smssecure.mms.OutgoingSecureMediaMessage;
import org.smssecure.smssecure.mms.SlideDeck;
import org.smssecure.smssecure.recipients.Recipient;
import org.smssecure.smssecure.recipients.RecipientFactory;
import org.smssecure.smssecure.recipients.RecipientFormattingException;
import org.smssecure.smssecure.recipients.Recipients;
import org.smssecure.smssecure.util.JsonUtils;
import org.smssecure.smssecure.util.ServiceUtil;
import org.smssecure.smssecure.util.SilencePreferences;
import org.smssecure.smssecure.util.Util;
import org.whispersystems.jobqueue.JobManager;
import org.whispersystems.libsignal.InvalidMessageException;
import org.whispersystems.libsignal.util.guava.Optional;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class MmsDatabase extends MessagingDatabase {

    public static final String TABLE_NAME = "mms";
    public static final String MESSAGE_BOX = "msg_box";
    public static final String MESSAGE_TYPE = "m_type";
    static final String DATE_SENT = "date";
    static final String DATE_RECEIVED = "date_received";
    public static final String[] CREATE_INDEXS = {
            "CREATE INDEX IF NOT EXISTS mms_thread_id_index ON " + TABLE_NAME + " (" + THREAD_ID + ");",
            "CREATE INDEX IF NOT EXISTS mms_read_index ON " + TABLE_NAME + " (" + READ + ");",
            "CREATE INDEX IF NOT EXISTS mms_read_and_notified_and_thread_id_index ON " + TABLE_NAME + "(" + READ + "," + NOTIFIED + "," + THREAD_ID + ");",
            "CREATE INDEX IF NOT EXISTS mms_message_box_index ON " + TABLE_NAME + " (" + MESSAGE_BOX + ");",
            "CREATE INDEX IF NOT EXISTS mms_date_sent_index ON " + TABLE_NAME + " (" + DATE_SENT + ");",
            "CREATE INDEX IF NOT EXISTS mms_thread_date_index ON " + TABLE_NAME + " (" + THREAD_ID + ", " + DATE_RECEIVED + ");"
    };
    static final String CONTENT_LOCATION = "ct_l";
    static final String EXPIRY = "exp";
    static final String MESSAGE_SIZE = "m_size";
    static final String STATUS = "st";
    static final String TRANSACTION_ID = "tr_id";
    static final String PART_COUNT = "part_count";
    static final String NETWORK_FAILURE = "network_failures";

    public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY, " +
            THREAD_ID + " INTEGER, " + DATE_SENT + " INTEGER, " + DATE_RECEIVED + " INTEGER, " + MESSAGE_BOX + " INTEGER, " +
            READ + " INTEGER DEFAULT 0, " + "m_id" + " TEXT, " + "sub" + " TEXT, " +
            "sub_cs" + " INTEGER, " + BODY + " TEXT, " + PART_COUNT + " INTEGER, " +
            "ct_t" + " TEXT, " + CONTENT_LOCATION + " TEXT, " + ADDRESS + " TEXT, " +
            ADDRESS_DEVICE_ID + " INTEGER, " +
            EXPIRY + " INTEGER, " + "m_cls" + " TEXT, " + MESSAGE_TYPE + " INTEGER, " +
            "v" + " INTEGER, " + MESSAGE_SIZE + " INTEGER, " + "pri" + " INTEGER, " +
            "rr" + " INTEGER, " + "rpt_a" + " INTEGER, " + "resp_st" + " INTEGER, " +
            STATUS + " INTEGER, " + TRANSACTION_ID + " TEXT, " + "retr_st" + " INTEGER, " +
            "retr_txt" + " TEXT, " + "retr_txt_cs" + " INTEGER, " + "read_status" + " INTEGER, " +
            "ct_cls" + " INTEGER, " + "resp_txt" + " TEXT, " + "d_tm" + " INTEGER, " +
            DATE_DELIVERY_RECEIVED + " INTEGER DEFAULT 0, " + MISMATCHED_IDENTITIES + " TEXT DEFAULT NULL, " +
            NETWORK_FAILURE + " TEXT DEFAULT NULL," + "d_rpt" + " INTEGER, " +
            SUBSCRIPTION_ID + " INTEGER DEFAULT -1, " + NOTIFIED + " INTEGER DEFAULT 0);";
    private static final String TAG = MmsDatabase.class.getSimpleName();
    private static final String[] MMS_PROJECTION = new String[]{
            MmsDatabase.TABLE_NAME + "." + ID + " AS " + ID,
            THREAD_ID, DATE_SENT + " AS " + NORMALIZED_DATE_SENT,
            DATE_RECEIVED + " AS " + NORMALIZED_DATE_RECEIVED,
            MESSAGE_BOX, READ,
            CONTENT_LOCATION, EXPIRY, MESSAGE_TYPE,
            MESSAGE_SIZE, STATUS, TRANSACTION_ID,
            BODY, PART_COUNT, ADDRESS, ADDRESS_DEVICE_ID,
            DATE_DELIVERY_RECEIVED, MISMATCHED_IDENTITIES, NETWORK_FAILURE, SUBSCRIPTION_ID,
            NOTIFIED,
            AttachmentDatabase.TABLE_NAME + "." + AttachmentDatabase.ROW_ID + " AS " + AttachmentDatabase.ATTACHMENT_ID_ALIAS,
            AttachmentDatabase.UNIQUE_ID,
            AttachmentDatabase.MMS_ID,
            AttachmentDatabase.SIZE,
            AttachmentDatabase.DATA,
            AttachmentDatabase.THUMBNAIL,
            AttachmentDatabase.CONTENT_TYPE,
            AttachmentDatabase.CONTENT_LOCATION,
            AttachmentDatabase.DIGEST,
            AttachmentDatabase.CONTENT_DISPOSITION,
            AttachmentDatabase.NAME,
            AttachmentDatabase.TRANSFER_STATE
    };

    private static final String RAW_ID_WHERE = TABLE_NAME + "._id = ?";

    private final JobManager jobManager;

    public MmsDatabase(Context context, SQLiteOpenHelper databaseHelper) {
        super(context, databaseHelper);
        this.jobManager = ApplicationContext.getInstance(context).getJobManager();
    }

    @Override
    protected String getTableName() {
        return TABLE_NAME;
    }

    public int getMessageCountForThread(long threadId) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(TABLE_NAME, new String[]{"COUNT(*)"}, THREAD_ID + " = ?", new String[]{String.valueOf(threadId)}, null, null, null);

            if (cursor != null && cursor.moveToFirst())
                return cursor.getInt(0);
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return 0;
    }

    public void addFailures(long messageId, List<NetworkFailure> failure) {
        try {
            addToDocument(messageId, NETWORK_FAILURE, failure, NetworkFailureList.class);
        } catch (IOException e) {
            Log.w(TAG, e);
        }
    }

    public void removeFailure(long messageId, NetworkFailure failure) {
        try {
            removeFromDocument(messageId, NETWORK_FAILURE, failure, NetworkFailureList.class);
        } catch (IOException e) {
            Log.w(TAG, e);
        }
    }

    public long getThreadIdForMessage(long id) {
        String sql = "SELECT " + THREAD_ID + " FROM " + TABLE_NAME + " WHERE " + ID + " = ?";
        String[] sqlArgs = new String[]{String.valueOf(id)};
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor cursor = null;

        try {
            cursor = db.rawQuery(sql, sqlArgs);
            if (cursor != null && cursor.moveToFirst())
                return cursor.getLong(0);
            else
                return -1;
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    private long getThreadIdFor(IncomingMediaMessage retrieved) throws RecipientFormattingException, MmsException {
        if (retrieved.getGroupId() != null) {
            Recipients groupRecipients = RecipientFactory.getRecipientsFromString(context, retrieved.getGroupId(), true);
            return DatabaseFactory.getThreadDatabase(context).getThreadIdFor(groupRecipients);
        }

        String localNumber;
        Set<String> group = new HashSet<>();

        if (retrieved.getAddresses().getFrom() == null) {
            throw new MmsException("FROM value in PduHeaders did not exist.");
        }

        group.add(retrieved.getAddresses().getFrom());

        if (SilencePreferences.isPushRegistered(context)) {
            localNumber = SilencePreferences.getLocalNumber(context);
        } else {
            localNumber = ServiceUtil.getTelephonyManager(context).getLine1Number();
        }

        for (String cc : retrieved.getAddresses().getCc()) {
            PhoneNumberUtil.MatchType match;

            if (localNumber == null) match = PhoneNumberUtil.MatchType.NO_MATCH;
            else match = PhoneNumberUtil.getInstance().isNumberMatch(localNumber, cc);

            if (match == PhoneNumberUtil.MatchType.NO_MATCH ||
                    match == PhoneNumberUtil.MatchType.NOT_A_NUMBER) {
                group.add(cc);
            }
        }

        if (retrieved.getAddresses().getTo().size() > 1) {
            for (String to : retrieved.getAddresses().getTo()) {
                PhoneNumberUtil.MatchType match;

                if (localNumber == null) match = PhoneNumberUtil.MatchType.NO_MATCH;
                else match = PhoneNumberUtil.getInstance().isNumberMatch(localNumber, to);

                if (match == PhoneNumberUtil.MatchType.NO_MATCH ||
                        match == PhoneNumberUtil.MatchType.NOT_A_NUMBER) {
                    group.add(to);
                }

            }
        }

        String recipientsList = Util.join(group, ",");
        Recipients recipients = RecipientFactory.getRecipientsFromString(context, recipientsList, false);

        return DatabaseFactory.getThreadDatabase(context).getThreadIdFor(recipients);
    }

    private long getThreadIdFor(@NonNull NotificationInd notification) {
        String fromString = notification.getFrom() != null && notification.getFrom().getTextString() != null
                ? Util.toIsoString(notification.getFrom().getTextString())
                : "";
        Recipients recipients = RecipientFactory.getRecipientsFromString(context, fromString, false);
        if (recipients.isEmpty())
            recipients = RecipientFactory.getRecipientsFor(context, Recipient.getUnknownRecipient(), false);
        return DatabaseFactory.getThreadDatabase(context).getThreadIdFor(recipients);
    }

    private Cursor rawQuery(@NonNull String where, @Nullable String[] arguments) {
        SQLiteDatabase database = databaseHelper.getReadableDatabase();
        return database.rawQuery("SELECT " + Util.join(MMS_PROJECTION, ",") +
                " FROM " + MmsDatabase.TABLE_NAME + " LEFT OUTER JOIN " + AttachmentDatabase.TABLE_NAME +
                " ON (" + MmsDatabase.TABLE_NAME + "." + MmsDatabase.ID + " = " + AttachmentDatabase.TABLE_NAME + "." + AttachmentDatabase.MMS_ID + ")" +
                " WHERE " + where, arguments);
    }

    public Cursor getMessage(long messageId) {
        Cursor cursor = rawQuery(RAW_ID_WHERE, new String[]{String.valueOf(messageId)});
        setNotifyConverationListeners(cursor, getThreadIdForMessage(messageId));
        return cursor;
    }

    private void updateMailboxBitmask(long id, long maskOff, long maskOn) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_NAME +
                " SET " + MESSAGE_BOX + " = (" + MESSAGE_BOX + " & " + (Types.TOTAL_MASK - maskOff) + " | " + maskOn + " )" +
                " WHERE " + ID + " = ?", new String[]{String.valueOf(id)});

        DatabaseFactory.getThreadDatabase(context).update(getThreadIdForMessage(id), false);
    }

    public void markAsOutbox(long messageId) {
        updateMailboxBitmask(messageId, Types.BASE_TYPE_MASK, Types.BASE_OUTBOX_TYPE);
        notifyConversationListeners(getThreadIdForMessage(messageId));
    }

    public void markAsForcedSms(long messageId) {
        updateMailboxBitmask(messageId, Types.PUSH_MESSAGE_BIT, Types.MESSAGE_FORCE_SMS_BIT);
        notifyConversationListeners(getThreadIdForMessage(messageId));
    }

//  public void markAsSending(long messageId) {
//    updateMailboxBitmask(messageId, Types.BASE_TYPE_MASK, Types.BASE_SENDING_TYPE);
//    notifyConversationListeners(getThreadIdForMessage(messageId));
//  }

    public void markAsSentFailed(long messageId) {
        updateMailboxBitmask(messageId, Types.BASE_TYPE_MASK, Types.BASE_SENT_FAILED_TYPE);
        notifyConversationListeners(getThreadIdForMessage(messageId));
    }

    public void markAsSent(long messageId, boolean secure) {
        updateMailboxBitmask(messageId, Types.BASE_TYPE_MASK, Types.BASE_SENT_TYPE | (secure ? Types.SECURE_MESSAGE_BIT : 0));
        notifyConversationListeners(getThreadIdForMessage(messageId));
    }

    public void markDownloadState(long messageId, long state) {
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(STATUS, state);

        database.update(TABLE_NAME, contentValues, ID_WHERE, new String[]{String.valueOf(messageId)});
        notifyConversationListeners(getThreadIdForMessage(messageId));
    }

    public void markDeliveryStatus(long messageId, int status) {
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(STATUS, status);

        database.update(TABLE_NAME, contentValues, ID_WHERE, new String[]{String.valueOf(messageId)});
        notifyConversationListeners(getThreadIdForMessage(messageId));
    }

    public void markAsNoSession(long messageId, long threadId) {
        updateMailboxBitmask(messageId, Types.ENCRYPTION_MASK, Types.ENCRYPTION_REMOTE_NO_SESSION_BIT);
        notifyConversationListeners(threadId);
    }

//  public void markAsSecure(long messageId) {
//    updateMailboxBitmask(messageId, 0, Types.SECURE_MESSAGE_BIT);
//  }

    public void markAsInsecure(long messageId) {
        updateMailboxBitmask(messageId, Types.SECURE_MESSAGE_BIT, 0);
    }

    public void markAsDecryptFailed(long messageId, long threadId) {
        updateMailboxBitmask(messageId, Types.ENCRYPTION_MASK, Types.ENCRYPTION_REMOTE_FAILED_BIT);
        notifyConversationListeners(threadId);
    }

    public void markAsDecryptDuplicate(long messageId, long threadId) {
        updateMailboxBitmask(messageId, Types.ENCRYPTION_MASK, Types.ENCRYPTION_REMOTE_DUPLICATE_BIT);
        notifyConversationListeners(threadId);
    }

    public void markAsLegacyVersion(long messageId, long threadId) {
        updateMailboxBitmask(messageId, Types.ENCRYPTION_MASK, Types.ENCRYPTION_REMOTE_LEGACY_BIT);
        notifyConversationListeners(threadId);
    }

    public void markAsNotified(long id) {
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(NOTIFIED, 1);

        database.update(TABLE_NAME, contentValues, ID_WHERE, new String[]{String.valueOf(id)});
    }

    public void setMessagesRead(long threadId) {
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(READ, 1);

        database.update(TABLE_NAME, contentValues, THREAD_ID + " = ?", new String[]{String.valueOf(threadId)});
    }

    public void setAllMessagesRead() {
        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(READ, 1);

        database.update(TABLE_NAME, contentValues, null, null);
    }

    public Optional<MmsNotificationInfo> getNotification(long messageId) {
        Cursor cursor = null;

        try {
            cursor = rawQuery(RAW_ID_WHERE, new String[]{String.valueOf(messageId)});

            if (cursor != null && cursor.moveToNext()) {
                return Optional.of(new MmsNotificationInfo(cursor.getString(cursor.getColumnIndexOrThrow(CONTENT_LOCATION)),
                        cursor.getString(cursor.getColumnIndexOrThrow(TRANSACTION_ID)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(SUBSCRIPTION_ID))));
            } else {
                return Optional.absent();
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    public OutgoingMediaMessage getOutgoingMessage(MasterSecret masterSecret, long messageId)
            throws MmsException, NoSuchMessageException {
        MmsAddressDatabase addr = DatabaseFactory.getMmsAddressDatabase(context);
        AttachmentDatabase attachmentDatabase = DatabaseFactory.getAttachmentDatabase(context);
        Cursor cursor = null;

        try {
            cursor = rawQuery(RAW_ID_WHERE, new String[]{String.valueOf(messageId)});

            if (cursor != null && cursor.moveToNext()) {
                long outboxType = cursor.getLong(cursor.getColumnIndexOrThrow(MESSAGE_BOX));
                String messageText = cursor.getString(cursor.getColumnIndexOrThrow(BODY));
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(NORMALIZED_DATE_SENT));
                int subscriptionId = cursor.getInt(cursor.getColumnIndexOrThrow(SUBSCRIPTION_ID));
                List<Attachment> attachments = new LinkedList<Attachment>(attachmentDatabase.getAttachmentsForMessage(messageId));
                MmsAddresses addresses = addr.getAddressesForId(messageId);
                List<String> destinations = new LinkedList<>();
                String body = getDecryptedBody(masterSecret, messageText, outboxType);

                destinations.addAll(addresses.getBcc());
                destinations.addAll(addresses.getCc());
                destinations.addAll(addresses.getTo());

                Recipients recipients = RecipientFactory.getRecipientsFromStrings(context, destinations, false);

                OutgoingMediaMessage message = new OutgoingMediaMessage(recipients, body, attachments, timestamp, subscriptionId,
                        !addresses.getBcc().isEmpty() ? ThreadDatabase.DistributionTypes.BROADCAST :
                                ThreadDatabase.DistributionTypes.DEFAULT);
                if (Types.isSecureType(outboxType)) {
                    return new OutgoingSecureMediaMessage(message);
                }

                return message;
            }

            throw new NoSuchMessageException("No record found for id: " + messageId);
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    public Reader getNotificationsWithDownloadState(MasterSecret masterSecret, long state) {
        SQLiteDatabase database = databaseHelper.getReadableDatabase();
        String selection = STATUS + " = ?";
        String[] selectionArgs = new String[]{String.valueOf(state)};

        Cursor cursor = database.query(TABLE_NAME, MMS_PROJECTION, selection, selectionArgs, null, null, null);
        return new Reader(masterSecret, cursor);
    }

    public long copyMessageInbox(MasterSecret masterSecret, long messageId) throws MmsException {
        try {
            OutgoingMediaMessage request = getOutgoingMessage(masterSecret, messageId);
            ContentValues contentValues = new ContentValues();
            contentValues.put(ADDRESS, request.getRecipients().getPrimaryRecipient().getNumber());
            contentValues.put(DATE_SENT, request.getSentTimeMillis());
            contentValues.put(MESSAGE_BOX, Types.BASE_INBOX_TYPE | Types.SECURE_MESSAGE_BIT | Types.ENCRYPTION_SYMMETRIC_BIT);
            contentValues.put(THREAD_ID, getThreadIdForMessage(messageId));
            contentValues.put(READ, 1);
            contentValues.put(DATE_RECEIVED, contentValues.getAsLong(DATE_SENT));

            List<Attachment> attachments = new LinkedList<>();

            for (Attachment attachment : request.getAttachments()) {
                DatabaseAttachment databaseAttachment = (DatabaseAttachment) attachment;
                attachments.add(new DatabaseAttachment(databaseAttachment.getAttachmentId(),
                        databaseAttachment.getMmsId(),
                        databaseAttachment.hasData(),
                        databaseAttachment.hasThumbnail(),
                        databaseAttachment.getContentType(),
                        AttachmentDatabase.TRANSFER_PROGRESS_DONE,
                        databaseAttachment.getSize(),
                        databaseAttachment.getLocation(),
                        databaseAttachment.getKey(),
                        databaseAttachment.getRelay(),
                        databaseAttachment.getDigest()));
            }

            return insertMediaMessage(masterSecret,
                    MmsAddresses.forTo(request.getRecipients().toNumberStringList(false)),
                    request.getBody(),
                    attachments,
                    contentValues);
        } catch (NoSuchMessageException e) {
            throw new MmsException(e);
        }
    }

    private Pair<Long, Long> insertMessageInbox(MasterSecret masterSecret, IncomingMediaMessage retrieved,
                                                String contentLocation, long threadId, long mailbox)
            throws MmsException {
        if (threadId == -1 || retrieved.isGroupMessage()) {
            try {
                threadId = getThreadIdFor(retrieved);
            } catch (RecipientFormattingException e) {
                Log.w("MmsDatabase", e);
                if (threadId == -1)
                    throw new MmsException(e);
            }
        }
        ContentValues contentValues = new ContentValues();

        contentValues.put(DATE_SENT, retrieved.getSentTimeMillis());
        contentValues.put(ADDRESS, retrieved.getAddresses().getFrom());

        contentValues.put(MESSAGE_BOX, mailbox);
        contentValues.put(MESSAGE_TYPE, PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF);
        contentValues.put(THREAD_ID, threadId);
        contentValues.put(CONTENT_LOCATION, contentLocation);
        contentValues.put(STATUS, Status.DOWNLOAD_INITIALIZED);
        contentValues.put(DATE_RECEIVED, generatePduCompatTimestamp());
        contentValues.put(PART_COUNT, retrieved.getAttachments().size());
        contentValues.put(SUBSCRIPTION_ID, retrieved.getSubscriptionId());
        contentValues.put(READ, 0);

        if (!contentValues.containsKey(DATE_SENT)) {
            contentValues.put(DATE_SENT, contentValues.getAsLong(DATE_RECEIVED));
        }

        long messageId = insertMediaMessage(masterSecret, retrieved.getAddresses(),
                retrieved.getBody(), retrieved.getAttachments(),
                contentValues);

        DatabaseFactory.getThreadDatabase(context).setUnread(threadId);
        DatabaseFactory.getThreadDatabase(context).update(threadId, true);
        notifyConversationListeners(threadId);
        jobManager.add(new TrimThreadJob(context, threadId));

        return new Pair<>(messageId, threadId);
    }

    public Pair<Long, Long> insertMessageInbox(MasterSecret masterSecret,
                                               IncomingMediaMessage retrieved,
                                               String contentLocation, long threadId)
            throws MmsException {
        return insertMessageInbox(masterSecret, retrieved, contentLocation, threadId,
                Types.BASE_INBOX_TYPE | Types.ENCRYPTION_SYMMETRIC_BIT |
                        (retrieved.isPushMessage() ? Types.PUSH_MESSAGE_BIT : 0));
    }

    public Pair<Long, Long> insertSecureMessageInbox(MasterSecret masterSecret,
                                                     IncomingMediaMessage retrieved,
                                                     String contentLocation, long threadId)
            throws MmsException {
        return insertMessageInbox(masterSecret, retrieved, contentLocation, threadId,
                Types.BASE_INBOX_TYPE | Types.SECURE_MESSAGE_BIT |
                        Types.ENCRYPTION_REMOTE_BIT);
    }

    public Pair<Long, Long> insertSecureDecryptedMessageInbox(MasterSecret masterSecret,
                                                              IncomingMediaMessage retrieved,
                                                              long threadId)
            throws MmsException {
        return insertMessageInbox(masterSecret, retrieved, "", threadId,
                Types.BASE_INBOX_TYPE | Types.SECURE_MESSAGE_BIT |
                        Types.ENCRYPTION_SYMMETRIC_BIT |
                        (retrieved.isPushMessage() ? Types.PUSH_MESSAGE_BIT : 0));
    }

    public Pair<Long, Long> insertMessageInbox(@NonNull NotificationInd notification, int subscriptionId) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        MmsAddressDatabase addressDatabase = DatabaseFactory.getMmsAddressDatabase(context);
        long threadId = getThreadIdFor(notification);
        ContentValues contentValues = new ContentValues();
        ContentValuesBuilder contentBuilder = new ContentValuesBuilder(contentValues);

        Log.w(TAG, "Message received type: " + notification.getMessageType());

        contentBuilder.add(CONTENT_LOCATION, notification.getContentLocation());
        contentBuilder.add(DATE_SENT, System.currentTimeMillis());
        contentBuilder.add(EXPIRY, notification.getExpiry());
        contentBuilder.add(MESSAGE_SIZE, notification.getMessageSize());
        contentBuilder.add(TRANSACTION_ID, notification.getTransactionId());
        contentBuilder.add(MESSAGE_TYPE, notification.getMessageType());

        if (notification.getFrom() != null) {
            contentBuilder.add(ADDRESS, notification.getFrom().getTextString());
        } else {
            contentBuilder.add(ADDRESS, null);
        }

        long dateReceived = generatePduCompatTimestamp();

        contentValues.put(MESSAGE_BOX, Types.BASE_INBOX_TYPE);
        contentValues.put(THREAD_ID, threadId);
        contentValues.put(STATUS, Status.DOWNLOAD_INITIALIZED);
        contentValues.put(DATE_RECEIVED, dateReceived);
        contentValues.put(READ, Util.isDefaultSmsProvider(context) ? 0 : 1);
        contentValues.put(SUBSCRIPTION_ID, subscriptionId);

        if (!contentValues.containsKey(DATE_SENT))
            contentValues.put(DATE_SENT, contentValues.getAsLong(DATE_RECEIVED));

        if (contentValues.getAsLong(DATE_SENT) <= 0)
            contentValues.put(DATE_SENT, dateReceived);

        long messageId = db.insert(TABLE_NAME, null, contentValues);

        if (notification.getFrom() != null) {
            addressDatabase.insertAddressesForId(messageId, MmsAddresses.forFrom(Util.toIsoString(notification.getFrom().getTextString())));
        }

        return new Pair<>(messageId, threadId);
    }

    public void markIncomingNotificationReceived(long threadId) {
        notifyConversationListeners(threadId);
        DatabaseFactory.getThreadDatabase(context).update(threadId, true);

        if (org.smssecure.smssecure.util.Util.isDefaultSmsProvider(context)) {
            DatabaseFactory.getThreadDatabase(context).setUnread(threadId);
        }

        jobManager.add(new TrimThreadJob(context, threadId));
    }

    public long insertMessageOutbox(MasterSecret masterSecret, OutgoingMediaMessage message,
                                    long threadId, boolean forceSms)
            throws MmsException {
        long type = Types.BASE_SENDING_TYPE | Types.ENCRYPTION_SYMMETRIC_BIT;

        if (message.isSecure()) type |= Types.SECURE_MESSAGE_BIT;
        if (forceSms) type |= Types.MESSAGE_FORCE_SMS_BIT;

        List<String> recipientNumbers = message.getRecipients().toNumberStringList(true);

        MmsAddresses addresses;

        if (!message.getRecipients().isSingleRecipient() &&
                message.getDistributionType() == ThreadDatabase.DistributionTypes.BROADCAST) {
            addresses = MmsAddresses.forBcc(recipientNumbers);
        } else {
            addresses = MmsAddresses.forTo(recipientNumbers);
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put(DATE_SENT, message.getSentTimeMillis());
        contentValues.put(MESSAGE_TYPE, PduHeaders.MESSAGE_TYPE_SEND_REQ);

        contentValues.put(MESSAGE_BOX, type);
        contentValues.put(THREAD_ID, threadId);
        contentValues.put(READ, 1);
        contentValues.put(DATE_RECEIVED, contentValues.getAsLong(DATE_SENT));
        contentValues.put(SUBSCRIPTION_ID, message.getSubscriptionId());
        contentValues.remove(ADDRESS);

        long messageId = insertMediaMessage(masterSecret, addresses, message.getBody(),
                message.getAttachments(), contentValues);

        DatabaseFactory.getThreadDatabase(context).setLastSeen(threadId);
        jobManager.add(new TrimThreadJob(context, threadId));

        return messageId;
    }

    private @Nullable String getDecryptedBody(@NonNull MasterSecret masterSecret,
                                              @Nullable String body, long outboxType) {
        try {
            if (!TextUtils.isEmpty(body) && Types.isSymmetricEncryption(outboxType)) {
                MasterCipher masterCipher = new MasterCipher(masterSecret);
                return masterCipher.decryptBody(body);
            } else {
                return body;
            }
        } catch (InvalidMessageException e) {
            Log.w(TAG, e);
        }

        return null;
    }

    private long insertMediaMessage(@NonNull MasterSecret masterSecret,
                                    @NonNull MmsAddresses addresses,
                                    @Nullable String body,
                                    @NonNull List<Attachment> attachments,
                                    @NonNull ContentValues contentValues)
            throws MmsException {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        AttachmentDatabase partsDatabase = DatabaseFactory.getAttachmentDatabase(context);
        MmsAddressDatabase addressDatabase = DatabaseFactory.getMmsAddressDatabase(context);

        if (Types.isSymmetricEncryption(contentValues.getAsLong(MESSAGE_BOX))) {
            if (!TextUtils.isEmpty(body)) {
                contentValues.put(BODY, new MasterCipher(masterSecret).encryptBody(body));
            }
        }

        contentValues.put(PART_COUNT, attachments.size());

        db.beginTransaction();
        try {
            long messageId = db.insert(TABLE_NAME, null, contentValues);

            addressDatabase.insertAddressesForId(messageId, addresses);
            partsDatabase.insertAttachmentsForMessage(masterSecret, messageId, attachments);

            db.setTransactionSuccessful();
            return messageId;
        } finally {
            db.endTransaction();

            notifyConversationListeners(contentValues.getAsLong(THREAD_ID));
            DatabaseFactory.getThreadDatabase(context).update(contentValues.getAsLong(THREAD_ID), true);
        }
    }

    public boolean delete(long messageId) {
        long threadId = getThreadIdForMessage(messageId);
        MmsAddressDatabase addrDatabase = DatabaseFactory.getMmsAddressDatabase(context);
        AttachmentDatabase attachmentDatabase = DatabaseFactory.getAttachmentDatabase(context);
        attachmentDatabase.deleteAttachmentsForMessage(messageId);
        addrDatabase.deleteAddressesForId(messageId);

        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        database.delete(TABLE_NAME, ID_WHERE, new String[]{String.valueOf(messageId)});
        boolean threadDeleted = DatabaseFactory.getThreadDatabase(context).update(threadId, false);
        notifyConversationListeners(threadId);
        return threadDeleted;
    }

    public void deleteThread(long threadId) {
        Set<Long> singleThreadSet = new HashSet<>();
        singleThreadSet.add(threadId);
        deleteThreads(singleThreadSet);
    }

    /*package*/ void deleteThreads(Set<Long> threadIds) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        StringBuilder where = new StringBuilder();
        Cursor cursor = null;

        for (long threadId : threadIds) {
            where.append(THREAD_ID + " = '").append(threadId).append("' OR ");
        }

        where = new StringBuilder(where.substring(0, where.length() - 4));

        try {
            cursor = db.query(TABLE_NAME, new String[]{ID}, where.toString(), null, null, null, null);

            while (cursor != null && cursor.moveToNext()) {
                delete(cursor.getLong(0));
            }

        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    /*package*/void deleteMessagesInThreadBeforeDate(long threadId, long date) {
        Cursor cursor = null;

        try {
            SQLiteDatabase db = databaseHelper.getReadableDatabase();
            StringBuilder where = new StringBuilder(THREAD_ID + " = ? AND (CASE (" + MESSAGE_BOX + " & " + Types.BASE_TYPE_MASK + ") ");

            for (long outgoingType : Types.OUTGOING_MESSAGE_TYPES) {
                where.append(" WHEN ").append(outgoingType).append(" THEN ").append(DATE_SENT).append(" < ").append(date);
            }

            where.append(" ELSE " + DATE_RECEIVED + " < ").append(date).append(" END)");

            Log.w("MmsDatabase", "Executing trim query: " + where);
            cursor = db.query(TABLE_NAME, new String[]{ID}, where.toString(), new String[]{String.valueOf(threadId)}, null, null, null);

            while (cursor != null && cursor.moveToNext()) {
                Log.w("MmsDatabase", "Trimming: " + cursor.getLong(0));
                delete(cursor.getLong(0));
            }

        } finally {
            if (cursor != null)
                cursor.close();
        }
    }


    public void deleteAllThreads() {
        DatabaseFactory.getAttachmentDatabase(context).deleteAllAttachments();
        DatabaseFactory.getMmsAddressDatabase(context).deleteAllAddresses();

        SQLiteDatabase database = databaseHelper.getWritableDatabase();
        database.delete(TABLE_NAME, null, null);
    }

    public Cursor getCarrierMmsInformation(String apn) {
        Uri uri = Uri.withAppendedPath(Uri.parse("content://telephony/carriers"), "current");
        String selection = TextUtils.isEmpty(apn) ? null : "apn = ?";
        String[] selectionArgs = TextUtils.isEmpty(apn) ? null : new String[]{apn.trim()};

        try {
            return context.getContentResolver().query(uri, null, selection, selectionArgs, null);
        } catch (NullPointerException npe) {
            // NOTE - This is dumb, but on some devices there's an NPE in the Android framework
            // for the provider of this call, which gets rethrown back to here through a binder
            // call.
            throw new IllegalArgumentException(npe);
        }
    }

    public Reader readerFor(MasterSecret masterSecret, Cursor cursor) {
        return new Reader(masterSecret, cursor);
    }

    private long generatePduCompatTimestamp() {
        final long time = System.currentTimeMillis();
        return time - (time % 1000);
    }

    public static class Status {
        public static final int DOWNLOAD_INITIALIZED = 1;
        public static final int DOWNLOAD_NO_CONNECTIVITY = 2;
        public static final int DOWNLOAD_CONNECTING = 3;
        public static final int DOWNLOAD_SOFT_FAILURE = 4;
        public static final int DOWNLOAD_HARD_FAILURE = 5;
        public static final int DOWNLOAD_APN_UNAVAILABLE = 6;
    }

    public static class MmsNotificationInfo {
        private final String contentLocation;
        private final String transactionId;
        private final int subscriptionId;

        public MmsNotificationInfo(String contentLocation, String transactionId, int subscriptionId) {
            this.contentLocation = contentLocation;
            this.transactionId = transactionId;
            this.subscriptionId = subscriptionId;
        }

        public String getContentLocation() {
            return contentLocation;
        }

        public String getTransactionId() {
            return transactionId;
        }

        public int getSubscriptionId() {
            return subscriptionId;
        }
    }

    public class Reader {

        private final Cursor cursor;
        private final MasterSecret masterSecret;
        private final MasterCipher masterCipher;

        public Reader(MasterSecret masterSecret, Cursor cursor) {
            this.cursor = cursor;
            this.masterSecret = masterSecret;

            if (masterSecret != null) masterCipher = new MasterCipher(masterSecret);
            else masterCipher = null;
        }

        public MessageRecord getNext() {
            if (cursor == null || !cursor.moveToNext())
                return null;

            return getCurrent();
        }

        public MessageRecord getCurrent() {
            long mmsType = cursor.getLong(cursor.getColumnIndexOrThrow(MmsDatabase.MESSAGE_TYPE));

            if (mmsType == PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND) {
                return getNotificationMmsMessageRecord(cursor);
            } else {
                return getMediaMmsMessageRecord(cursor);
            }
        }

        private NotificationMmsMessageRecord getNotificationMmsMessageRecord(Cursor cursor) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(MmsDatabase.ID));
            long dateSent = cursor.getLong(cursor.getColumnIndexOrThrow(MmsDatabase.NORMALIZED_DATE_SENT));
            long dateReceived = cursor.getLong(cursor.getColumnIndexOrThrow(MmsDatabase.NORMALIZED_DATE_RECEIVED));
            long threadId = cursor.getLong(cursor.getColumnIndexOrThrow(MmsDatabase.THREAD_ID));
            long mailbox = cursor.getLong(cursor.getColumnIndexOrThrow(MmsDatabase.MESSAGE_BOX));
            String address = cursor.getString(cursor.getColumnIndexOrThrow(MmsDatabase.ADDRESS));
            int addressDeviceId = cursor.getInt(cursor.getColumnIndexOrThrow(MmsDatabase.ADDRESS_DEVICE_ID));
            Recipients recipients = getRecipientsFor(address);

            String contentLocation = cursor.getString(cursor.getColumnIndexOrThrow(MmsDatabase.CONTENT_LOCATION));
            String transactionId = cursor.getString(cursor.getColumnIndexOrThrow(MmsDatabase.TRANSACTION_ID));
            long messageSize = cursor.getLong(cursor.getColumnIndexOrThrow(MmsDatabase.MESSAGE_SIZE));
            long expiry = cursor.getLong(cursor.getColumnIndexOrThrow(MmsDatabase.EXPIRY));
            int status = cursor.getInt(cursor.getColumnIndexOrThrow(MmsDatabase.STATUS));
            long dateDeliveryReceived = cursor.getLong(cursor.getColumnIndexOrThrow(MmsDatabase.DATE_DELIVERY_RECEIVED));
            int subscriptionId = cursor.getInt(cursor.getColumnIndexOrThrow(MmsDatabase.SUBSCRIPTION_ID));

            byte[] contentLocationBytes = null;
            byte[] transactionIdBytes = null;

            if (!TextUtils.isEmpty(contentLocation))
                contentLocationBytes = org.smssecure.smssecure.util.Util.toIsoBytes(contentLocation);

            if (!TextUtils.isEmpty(transactionId))
                transactionIdBytes = org.smssecure.smssecure.util.Util.toIsoBytes(transactionId);

            SlideDeck slideDeck = new SlideDeck(context, new MmsNotificationAttachment(status, messageSize));

            return new NotificationMmsMessageRecord(context, id, recipients, recipients.getPrimaryRecipient(),
                    addressDeviceId, dateSent, dateReceived, dateDeliveryReceived, threadId,
                    contentLocationBytes, messageSize, expiry, status,
                    transactionIdBytes, mailbox, subscriptionId, slideDeck);
        }

        private MediaMmsMessageRecord getMediaMmsMessageRecord(Cursor cursor) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(MmsDatabase.ID));
            long dateSent = cursor.getLong(cursor.getColumnIndexOrThrow(MmsDatabase.NORMALIZED_DATE_SENT));
            long dateReceived = cursor.getLong(cursor.getColumnIndexOrThrow(MmsDatabase.NORMALIZED_DATE_RECEIVED));
            long box = cursor.getLong(cursor.getColumnIndexOrThrow(MmsDatabase.MESSAGE_BOX));
            long threadId = cursor.getLong(cursor.getColumnIndexOrThrow(MmsDatabase.THREAD_ID));
            String address = cursor.getString(cursor.getColumnIndexOrThrow(MmsDatabase.ADDRESS));
            int addressDeviceId = cursor.getInt(cursor.getColumnIndexOrThrow(MmsDatabase.ADDRESS_DEVICE_ID));
            long dateDeliveryReceived = cursor.getLong(cursor.getColumnIndexOrThrow(MmsDatabase.DATE_DELIVERY_RECEIVED));
            DisplayRecord.Body body = getBody(cursor);
            int partCount = cursor.getInt(cursor.getColumnIndexOrThrow(MmsDatabase.PART_COUNT));
            String mismatchDocument = cursor.getString(cursor.getColumnIndexOrThrow(MmsDatabase.MISMATCHED_IDENTITIES));
            String networkDocument = cursor.getString(cursor.getColumnIndexOrThrow(MmsDatabase.NETWORK_FAILURE));
            int subscriptionId = cursor.getInt(cursor.getColumnIndexOrThrow(MmsDatabase.SUBSCRIPTION_ID));

            Recipients recipients = getRecipientsFor(address);
            List<IdentityKeyMismatch> mismatches = getMismatchedIdentities(mismatchDocument);
            List<NetworkFailure> networkFailures = getFailures(networkDocument);

            SlideDeck slideDeck = getSlideDeck(cursor);

            return new MediaMmsMessageRecord(context, id, recipients, recipients.getPrimaryRecipient(),
                    addressDeviceId, dateSent, dateReceived, dateDeliveryReceived,
                    threadId, body, slideDeck, partCount, box, mismatches,
                    networkFailures, subscriptionId);
        }

        private Recipients getRecipientsFor(String address) {
            if (TextUtils.isEmpty(address) || address.equals("insert-address-token")) {
                return RecipientFactory.getRecipientsFor(context, Recipient.getUnknownRecipient(), true);
            }

            Recipients recipients = RecipientFactory.getRecipientsFromString(context, address, true);

            if (recipients == null || recipients.isEmpty()) {
                return RecipientFactory.getRecipientsFor(context, Recipient.getUnknownRecipient(), true);
            }

            return recipients;
        }

        private List<IdentityKeyMismatch> getMismatchedIdentities(String document) {
            if (!TextUtils.isEmpty(document)) {
                try {
                    return JsonUtils.fromJson(document, IdentityKeyMismatchList.class).getList();
                } catch (IOException e) {
                    Log.w(TAG, e);
                }
            }

            return new LinkedList<>();
        }

        private List<NetworkFailure> getFailures(String document) {
            if (!TextUtils.isEmpty(document)) {
                try {
                    return JsonUtils.fromJson(document, NetworkFailureList.class).getList();
                } catch (IOException ioe) {
                    Log.w(TAG, ioe);
                }
            }

            return new LinkedList<>();
        }

        private DisplayRecord.Body getBody(Cursor cursor) {
            try {
                String body = cursor.getString(cursor.getColumnIndexOrThrow(MmsDatabase.BODY));
                long box = cursor.getLong(cursor.getColumnIndexOrThrow(MmsDatabase.MESSAGE_BOX));

                if (!TextUtils.isEmpty(body) && masterCipher != null && Types.isSymmetricEncryption(box)) {
                    return new DisplayRecord.Body(masterCipher.decryptBody(body), true);
                } else if (!TextUtils.isEmpty(body) && masterCipher == null && Types.isSymmetricEncryption(box)) {
                    return new DisplayRecord.Body(body, false);
                } else {
                    return new DisplayRecord.Body(body == null ? "" : body, true);
                }
            } catch (InvalidMessageException e) {
                Log.w("MmsDatabase", e);
                return new DisplayRecord.Body(context.getString(R.string.MmsDatabase_error_decrypting_message), true);
            }
        }

        private SlideDeck getSlideDeck(@NonNull Cursor cursor) {
            Attachment attachment = DatabaseFactory.getAttachmentDatabase(context).getAttachment(cursor);
            return new SlideDeck(context, attachment);
        }

        public void close() {
            cursor.close();
        }
    }
}
