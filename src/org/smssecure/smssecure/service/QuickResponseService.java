package org.smssecure.smssecure.service;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.smssecure.smssecure.R;
import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.database.ThreadDatabase;
import org.smssecure.smssecure.mms.OutgoingMediaMessage;
import org.smssecure.smssecure.mms.SlideDeck;
import org.smssecure.smssecure.recipients.RecipientFactory;
import org.smssecure.smssecure.recipients.Recipients;
import org.smssecure.smssecure.sms.MessageSender;
import org.smssecure.smssecure.sms.OutgoingTextMessage;
import org.smssecure.smssecure.util.Rfc5724Uri;

import java.net.URISyntaxException;

public class QuickResponseService extends MasterSecretIntentService {

  private static final String TAG = QuickResponseService.class.getSimpleName();

  public QuickResponseService() {
    super("QuickResponseService");
  }

  @Override
  protected void onHandleIntent(Intent intent, @Nullable MasterSecret masterSecret) {
    if (!TelephonyManager.ACTION_RESPOND_VIA_MESSAGE.equals(intent.getAction())) {
      Log.w(TAG, "Received unknown intent: " + intent.getAction());
      return;
    }

    if (masterSecret == null) {
      Log.w(TAG, "Got quick response request when locked...");
      Toast.makeText(this, R.string.QuickResponseService_quick_response_unavailable_when_SMSSecure_is_locked, Toast.LENGTH_LONG).show();
      return;
    }

    try {
      Rfc5724Uri uri        = new Rfc5724Uri(intent.getDataString());
      String     content    = intent.getStringExtra(Intent.EXTRA_TEXT);
      Recipients recipients = RecipientFactory.getRecipientsFromString(this, uri.getPath(), false);

      if (!TextUtils.isEmpty(content)) {
        if (recipients.isSingleRecipient()) {
          MessageSender.send(this, masterSecret, new OutgoingTextMessage(recipients, content), -1, false);
        } else {
          MessageSender.send(this, masterSecret, new OutgoingMediaMessage(this, recipients, new SlideDeck(), content,
                                                                          ThreadDatabase.DistributionTypes.DEFAULT), -1, false);
        }
      }
    } catch (URISyntaxException e) {
      Toast.makeText(this, R.string.QuickResponseService_problem_sending_message, Toast.LENGTH_LONG).show();
      Log.w(TAG, e);
    }
  }
}
