package org.SecuredText.SecuredText.jobs;

import android.app.Activity;
import android.content.Context;
import android.telephony.SmsManager;
import android.util.Log;

import org.SecuredText.SecuredText.ApplicationContext;
import org.SecuredText.SecuredText.crypto.MasterSecret;
import org.SecuredText.SecuredText.crypto.SecurityEvent;
import org.SecuredText.SecuredText.crypto.storage.SecuredTextSessionStore;
import org.SecuredText.SecuredText.database.DatabaseFactory;
import org.SecuredText.SecuredText.database.EncryptingSmsDatabase;
import org.SecuredText.SecuredText.database.NoSuchMessageException;
import org.SecuredText.SecuredText.database.model.SmsMessageRecord;
import org.SecuredText.SecuredText.jobs.requirements.MasterSecretRequirement;
import org.SecuredText.SecuredText.notifications.MessageNotifier;
import org.SecuredText.SecuredText.service.SmsDeliveryListener;
import org.whispersystems.jobqueue.JobParameters;
import org.whispersystems.libaxolotl.state.SessionStore;

public class SmsSentJob extends MasterSecretJob {

  private static final String TAG = SmsSentJob.class.getSimpleName();

  private final long   messageId;
  private final String action;
  private final int    result;

  public SmsSentJob(Context context, long messageId, String action, int result) {
    super(context, JobParameters.newBuilder()
                                .withPersistence()
                                .withRequirement(new MasterSecretRequirement(context))
                                .create());

    this.messageId = messageId;
    this.action    = action;
    this.result    = result;
  }

  @Override
  public void onAdded() {

  }

  @Override
  public void onRun(MasterSecret masterSecret) {
    Log.w(TAG, "Got SMS callback: " + action + " , " + result);

    switch (action) {
      case SmsDeliveryListener.SENT_SMS_ACTION:
        handleSentResult(masterSecret, messageId, result);
        break;
      case SmsDeliveryListener.DELIVERED_SMS_ACTION:
        handleDeliveredResult(messageId, result);
        break;
    }
  }

  @Override
  public boolean onShouldRetryThrowable(Exception throwable) {
    return false;
  }

  @Override
  public void onCanceled() {

  }

  private void handleDeliveredResult(long messageId, int result) {
    DatabaseFactory.getEncryptingSmsDatabase(context).markStatus(messageId, result);
  }

  private void handleSentResult(MasterSecret masterSecret, long messageId, int result) {
    try {
      EncryptingSmsDatabase database = DatabaseFactory.getEncryptingSmsDatabase(context);
      SmsMessageRecord      record   = database.getMessage(masterSecret, messageId);

      switch (result) {
        case Activity.RESULT_OK:
          database.markAsSent(messageId);

          if (record != null && record.isEndSession()) {
            Log.w(TAG, "Ending session...");
            SessionStore sessionStore = new SecuredTextSessionStore(context, masterSecret);
            sessionStore.deleteAllSessions(record.getIndividualRecipient().getNumber());
            SecurityEvent.broadcastSecurityUpdateEvent(context, record.getThreadId());
          }

          break;
        case SmsManager.RESULT_ERROR_NO_SERVICE:
        case SmsManager.RESULT_ERROR_RADIO_OFF:
          Log.w(TAG, "Service connectivity problem, requeuing...");
          ApplicationContext.getInstance(context)
              .getJobManager()
              .add(new SmsSendJob(context, messageId, record.getIndividualRecipient().getNumber()));

          break;
        default:
          database.markAsSentFailed(messageId);
          MessageNotifier.notifyMessageDeliveryFailed(context, record.getRecipients(), record.getThreadId());
      }
    } catch (NoSuchMessageException e) {
      Log.w(TAG, e);
    }
  }
}
