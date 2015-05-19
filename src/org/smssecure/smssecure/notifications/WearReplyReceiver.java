/**
 * Copyright (C) 2011 Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.smssecure.smssecure.notifications;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.RemoteInput;
import android.util.Log;

import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.database.DatabaseFactory;
import org.smssecure.smssecure.recipients.Recipient;
import org.smssecure.smssecure.recipients.RecipientFactory;
import org.smssecure.smssecure.recipients.RecipientProvider;
import org.smssecure.smssecure.recipients.Recipients;
import org.smssecure.smssecure.sms.MessageSender;
import org.smssecure.smssecure.sms.OutgoingTextMessage;

/**
 * Get the response text from the Wearable Device and sends an message as a reply
 *
 * @author Alix Ducros (Ported to TextSecure-Codebase by Christoph Haefner)
 */
public class WearReplyReceiver extends BroadcastReceiver {

  public static final String TAG = WearReplyReceiver.class.getSimpleName();
  public static final String REPLY_ACTION = "org.smssecure.smssecure.notifications.WEAR_REPLY";

  @Override
  public void onReceive(final Context context, Intent intent) {
    if (!intent.getAction().equals(REPLY_ACTION))
      return;

    Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
    if (remoteInput == null)
      return;

    final long[] threadIds = intent.getLongArrayExtra("thread_ids");
    final MasterSecret masterSecret = intent.getParcelableExtra("master_secret");
    final long recipientId = intent.getLongExtra("recipient_id", -1);
    final CharSequence responseText = remoteInput.getCharSequence(MessageNotifier.EXTRA_VOICE_REPLY);

    final Recipients recipients = RecipientFactory.getRecipientsForIds(context, new long[]{recipientId}, false);

    if (threadIds != null && masterSecret != null) {

      ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
              .cancel(MessageNotifier.NOTIFICATION_ID);

      new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
          for (long threadId : threadIds) {
            Log.w(TAG, "Marking as read: " + threadId);
            DatabaseFactory.getThreadDatabase(context).setRead(threadId);
          }

          OutgoingTextMessage reply = new OutgoingTextMessage(recipients, responseText.toString());
          MessageSender.send(context, masterSecret, reply, threadIds[0], false);

          MessageNotifier.updateNotification(context, masterSecret);
          return null;
        }
      }.execute();
    }
  }
}
