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
package org.smssecure.smssecure.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Telephony;
import android.util.Log;

import com.google.android.mms.pdu_alt.GenericPdu;
import com.google.android.mms.pdu_alt.NotificationInd;
import com.google.android.mms.pdu_alt.PduHeaders;
import com.google.android.mms.pdu_alt.PduParser;

import org.smssecure.smssecure.ApplicationContext;
import org.smssecure.smssecure.jobs.MmsReceiveJob;
import org.smssecure.smssecure.protocol.WirePrefix;
import org.smssecure.smssecure.util.SilencePreferences;
import org.smssecure.smssecure.util.Util;

public class MmsListener extends BroadcastReceiver {

    private static final String TAG = MmsListener.class.getSimpleName();

    private boolean isRelevant(Context context, Intent intent) {

        if (!ApplicationMigrationService.isDatabaseImported(context)) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
                Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION.equals(intent.getAction()) &&
                Util.isDefaultSmsProvider(context)) {
            return false;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT &&
                SilencePreferences.isInterceptAllMmsEnabled(context)) {
            return true;
        }

        byte[] mmsData = intent.getByteArrayExtra("data");
        PduParser parser = new PduParser(mmsData);
        GenericPdu pdu = parser.parse();

        if (pdu == null || pdu.getMessageType() != PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND) {
            Log.w(TAG, "Received Invalid notification PDU");
            return false;
        }

        NotificationInd notificationPdu = (NotificationInd) pdu;

        if (notificationPdu.getSubject() == null)
            return false;

        return WirePrefix.isEncryptedMmsSubject(notificationPdu.getSubject().getString());
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.w(TAG, "Got MMS broadcast..." + intent.getAction());

        if ((Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION.equals(intent.getAction()) &&
                Util.isDefaultSmsProvider(context)) ||
                (Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION.equals(intent.getAction()) &&
                        isRelevant(context, intent))) {
            Log.w(TAG, "Relevant!");
            int subscriptionId = intent.getExtras().getInt("subscription", -1);

            ApplicationContext.getInstance(context)
                    .getJobManager()
                    .add(new MmsReceiveJob(context, intent.getByteArrayExtra("data"), subscriptionId));

            abortBroadcast();
        }
    }


}
