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
import android.text.SpannableString;

import org.smssecure.smssecure.R;
import org.smssecure.smssecure.database.MmsSmsColumns;
import org.smssecure.smssecure.database.SmsDatabase;
import org.smssecure.smssecure.database.documents.IdentityKeyMismatch;
import org.smssecure.smssecure.database.documents.NetworkFailure;
import org.smssecure.smssecure.recipients.Recipient;
import org.smssecure.smssecure.recipients.Recipients;

import java.util.LinkedList;
import java.util.List;

/**
 * The message record model which represents standard SMS messages.
 *
 * @author Moxie Marlinspike
 *
 */

public class SmsMessageRecord extends MessageRecord {

    public SmsMessageRecord(Context context, long id,
                            Body body, Recipients recipients,
                            Recipient individualRecipient,
                            int recipientDeviceId,
                            long dateSent, long dateReceived,
                            long dateDeliveryReceived,
                            long type, long threadId,
                            int status, List<IdentityKeyMismatch> mismatches,
                            int subscriptionId) {
        super(context, id, body, recipients, individualRecipient, recipientDeviceId,
                dateSent, dateReceived, threadId, status, dateDeliveryReceived, type,
                mismatches, new LinkedList<NetworkFailure>(), subscriptionId);
    }

    public long getType() {
        return type;
    }

    @Override
    public SpannableString getDisplayBody() {
        if (SmsDatabase.Types.isFailedDecryptType(type)) {
            return emphasisAdded(context.getString(R.string.MessageDisplayHelper_bad_encrypted_message));
        } else if (isStaleKeyExchange()) {
            return emphasisAdded(context.getString(R.string.ConversationItem_error_received_stale_key_exchange_message));
        } else if (isCorruptedKeyExchange()) {
            return emphasisAdded(context.getString(R.string.SmsMessageRecord_received_corrupted_key_exchange_message));
        } else if (isInvalidVersionKeyExchange()) {
            return emphasisAdded(context.getString(R.string.SmsMessageRecord_received_key_exchange_message_for_invalid_protocol_version));
        } else if (isXmppExchange()) {
            return emphasisAdded(context.getString(R.string.ConversationItem_xmpp_address_update_silence));
        } else if (isLegacyMessage() && isKeyExchange() && !isProcessedKeyExchange()) {
            return emphasisAdded(context.getString(R.string.MessageRecord_the_exchange_message_you_got_came_from_a_version_of_silence_that_is_too_outdated));
        } else if (isLegacyMessage() && isKeyExchange() && isProcessedKeyExchange()) {
            return emphasisAdded(context.getString(R.string.MessageRecord_key_exchange_message));
        } else if (MmsSmsColumns.Types.isLegacyType(type)) {
            return emphasisAdded(context.getString(R.string.MessageRecord_message_encrypted_with_a_legacy_protocol_version_that_is_no_longer_supported));
        } else if (isBundleKeyExchange()) {
            return emphasisAdded(context.getString(R.string.SmsMessageRecord_received_message_with_unknown_identity_key_tap_to_process));
        } else if (isIdentityUpdate()) {
            return emphasisAdded(context.getString(R.string.SmsMessageRecord_received_updated_but_unknown_identity_information));
        } else if (isKeyExchange() && !isOutgoing() && !isProcessedKeyExchange()) {
            return emphasisAdded(context.getString(R.string.ConversationItem_received_key_exchange_message_tap_to_process));
        } else if (isDuplicateMessageType()) {
            return emphasisAdded(context.getString(R.string.SmsMessageRecord_duplicate_message));
        } else if (SmsDatabase.Types.isDecryptInProgressType(type)) {
            return emphasisAdded(context.getString(R.string.MessageDisplayHelper_decrypting_please_wait));
        } else if (SmsDatabase.Types.isNoRemoteSessionType(type)) {
            if (SmsDatabase.Types.isEndSessionType(type)) {
                return emphasisAdded(context.getString(R.string.MessageDisplayHelper_end_session_encrypted_for_non_existing_session));
            } else {
                return emphasisAdded(context.getString(R.string.MessageDisplayHelper_message_encrypted_for_non_existing_session));
            }
        } else if (!getBody().isPlaintext()) {
            return emphasisAdded(context.getString(R.string.MessageNotifier_encrypted_message));
        } else if (SmsDatabase.Types.isEndSessionType(type)) {
            return emphasisAdded(context.getString(R.string.SmsMessageRecord_secure_session_ended));
        } else {
            return super.getDisplayBody();
        }
    }

    @Override
    public boolean isMms() {
        return false;
    }

    @Override
    public boolean isMmsNotification() {
        return false;
    }
}
