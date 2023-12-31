/**
 * Copyright (C) 2012 Moxie Marlinspike
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
package org.smssecure.smssecure.database.model;

import android.content.Context;
import androidx.annotation.NonNull;
import android.text.SpannableString;

import org.smssecure.smssecure.R;
import org.smssecure.smssecure.database.MmsDatabase;
import org.smssecure.smssecure.database.SmsDatabase.Status;
import org.smssecure.smssecure.database.documents.IdentityKeyMismatch;
import org.smssecure.smssecure.database.documents.NetworkFailure;
import org.smssecure.smssecure.mms.Slide;
import org.smssecure.smssecure.mms.SlideDeck;
import org.smssecure.smssecure.recipients.Recipient;
import org.smssecure.smssecure.recipients.Recipients;

import java.util.List;

/**
 * Represents the message record model for MMS messages that contain
 * media (ie: they've been downloaded).
 *
 * @author Moxie Marlinspike
 *
 */

public class MediaMmsMessageRecord extends MmsMessageRecord {
    private final static String TAG = MediaMmsMessageRecord.class.getSimpleName();

    private final Context context;
    private final int partCount;

    public MediaMmsMessageRecord(Context context, long id, Recipients recipients,
                                 Recipient individualRecipient, int recipientDeviceId,
                                 long dateSent, long dateReceived, long dateDeliveryReceived,
                                 long threadId, Body body,
                                 @NonNull SlideDeck slideDeck,
                                 int partCount, long mailbox,
                                 List<IdentityKeyMismatch> mismatches,
                                 List<NetworkFailure> failures, int subscriptionId) {
        super(context, id, body, recipients, individualRecipient, recipientDeviceId, dateSent,
                dateReceived, threadId, Status.STATUS_NONE, dateDeliveryReceived, mailbox, mismatches, failures,
                subscriptionId, slideDeck);

        this.context = context.getApplicationContext();
        this.partCount = partCount;
    }

    public int getPartCount() {
        return partCount;
    }

    @Override
    public boolean isMmsNotification() {
        return false;
    }

    @Override
    public boolean isMediaPending() {
        for (Slide slide : getSlideDeck().getSlides()) {
            if (slide.isInProgress() || slide.isPendingDownload()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public SpannableString getDisplayBody() {
        if (MmsDatabase.Types.isDecryptInProgressType(type)) {
            return emphasisAdded(context.getString(R.string.MmsMessageRecord_decrypting_mms_please_wait));
        } else if (MmsDatabase.Types.isFailedDecryptType(type)) {
            return emphasisAdded(context.getString(R.string.MmsMessageRecord_bad_encrypted_mms_message));
        } else if (isDuplicateMessageType()) {
            return emphasisAdded(context.getString(R.string.SmsMessageRecord_duplicate_message));
        } else if (MmsDatabase.Types.isNoRemoteSessionType(type)) {
            return emphasisAdded(context.getString(R.string.MmsMessageRecord_mms_message_encrypted_for_non_existing_session));
        } else if (isLegacyMessage()) {
            return emphasisAdded(context.getString(R.string.MessageRecord_message_encrypted_with_a_legacy_protocol_version_that_is_no_longer_supported));
        } else if (!getBody().isPlaintext()) {
            return emphasisAdded(context.getString(R.string.MessageNotifier_encrypted_message));
        }

        return super.getDisplayBody();
    }
}
