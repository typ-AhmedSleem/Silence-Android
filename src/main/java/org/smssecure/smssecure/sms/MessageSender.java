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
package org.smssecure.smssecure.sms;

import android.content.Context;
import android.util.Log;

import org.smssecure.smssecure.ApplicationContext;
import org.smssecure.smssecure.attachments.Attachment;
import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.database.DatabaseFactory;
import org.smssecure.smssecure.database.EncryptingSmsDatabase;
import org.smssecure.smssecure.database.MmsDatabase;
import org.smssecure.smssecure.database.ThreadDatabase;
import org.smssecure.smssecure.database.model.MessageRecord;
import org.smssecure.smssecure.jobs.MmsSendJob;
import org.smssecure.smssecure.jobs.SmsSendJob;
import org.smssecure.smssecure.mms.MmsException;
import org.smssecure.smssecure.mms.OutgoingMediaMessage;
import org.smssecure.smssecure.mms.OutgoingSecureMediaMessage;
import org.smssecure.smssecure.recipients.Recipients;
import org.whispersystems.jobqueue.JobManager;

import java.util.LinkedList;
import java.util.List;

public class MessageSender {

    private static final String TAG = MessageSender.class.getSimpleName();

    public static long send(final Context context,
                            final MasterSecret masterSecret,
                            final OutgoingTextMessage message,
                            final long threadId,
                            final boolean forceSms) {
        EncryptingSmsDatabase database = DatabaseFactory.getEncryptingSmsDatabase(context);
        Recipients recipients = message.getRecipients();

        long allocatedThreadId;

        if (threadId == -1) {
            allocatedThreadId = DatabaseFactory.getThreadDatabase(context).getThreadIdFor(recipients);
        } else {
            allocatedThreadId = threadId;
        }

        long messageId = database.insertMessageOutbox(masterSecret, allocatedThreadId, message, forceSms, System.currentTimeMillis());

        sendTextMessage(context, recipients, messageId);

        return allocatedThreadId;
    }

    public static long send(final Context context,
                            final MasterSecret masterSecret,
                            final OutgoingMediaMessage message,
                            final long threadId,
                            final boolean forceSms) {
        try {
            ThreadDatabase threadDatabase = DatabaseFactory.getThreadDatabase(context);
            MmsDatabase database = DatabaseFactory.getMmsDatabase(context);

            long allocatedThreadId;

            if (threadId == -1) {
                allocatedThreadId = threadDatabase.getThreadIdFor(message.getRecipients(), message.getDistributionType());
            } else {
                allocatedThreadId = threadId;
            }

            Recipients recipients = message.getRecipients();
            long messageId = database.insertMessageOutbox(masterSecret, message, allocatedThreadId, forceSms);

            sendMediaMessage(context, messageId);

            return allocatedThreadId;
        } catch (MmsException e) {
            Log.w(TAG, e);
            return threadId;
        }
    }

    public static void resend(Context context, MasterSecret masterSecret, MessageRecord messageRecord) {
        long messageId = messageRecord.getId();
        boolean isSecure = messageRecord.isSecure();
        long threadId = messageRecord.getThreadId();
        String body = messageRecord.getBody().getBody();
        int subscriptionId = messageRecord.getSubscriptionId();

        if (messageRecord.isMms()) {
            Recipients recipients = DatabaseFactory.getMmsAddressDatabase(context).getRecipientsForId(messageId);
            long sentTimeMillis = System.currentTimeMillis();
            List<Attachment> attachments = new LinkedList<Attachment>(DatabaseFactory.getAttachmentDatabase(context).getAttachmentsForMessage(messageId));

            OutgoingMediaMessage newMessage = new OutgoingMediaMessage(recipients,
                    body,
                    attachments,
                    sentTimeMillis,
                    subscriptionId,
                    ThreadDatabase.DistributionTypes.BROADCAST);

            if (isSecure) {
                send(context, masterSecret, new OutgoingSecureMediaMessage(newMessage), threadId, true);
            } else {
                send(context, masterSecret, newMessage, threadId, true);
            }

            DatabaseFactory.getMmsDatabase(context).delete(messageId);
        } else {
            Recipients recipients = messageRecord.getRecipients();
            OutgoingTextMessage newMessage;

            if (isSecure) {
                newMessage = new OutgoingTextMessage(recipients, body, subscriptionId);
            } else {
                newMessage = new OutgoingEncryptedMessage(recipients, body, subscriptionId);
            }

            send(context, masterSecret, newMessage, threadId, true);
            DatabaseFactory.getSmsDatabase(context).deleteMessage(messageId);
        }
    }

    private static void sendMediaMessage(Context context, long messageId)
            throws MmsException {
        JobManager jobManager = ApplicationContext.getInstance(context).getJobManager();
        jobManager.add(new MmsSendJob(context, messageId));
    }

    private static void sendTextMessage(Context context, Recipients recipients, long messageId) {
        JobManager jobManager = ApplicationContext.getInstance(context).getJobManager();
        jobManager.add(new SmsSendJob(context, messageId, recipients.getPrimaryRecipient().getName()));
    }
}
