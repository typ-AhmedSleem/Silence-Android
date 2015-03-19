package org.SecuredText.SecuredText.jobs;

import android.content.Context;
import android.util.Log;

import org.SecuredText.SecuredText.ApplicationContext;
import org.SecuredText.SecuredText.crypto.MasterSecret;
import org.SecuredText.SecuredText.crypto.SessionUtil;
import org.SecuredText.SecuredText.database.DatabaseFactory;
import org.SecuredText.SecuredText.database.EncryptingSmsDatabase;
import org.SecuredText.SecuredText.database.NoSuchMessageException;
import org.SecuredText.SecuredText.database.SmsDatabase;
import org.SecuredText.SecuredText.database.model.SmsMessageRecord;
import org.SecuredText.SecuredText.dependencies.InjectableType;
import org.SecuredText.SecuredText.notifications.MessageNotifier;
import org.SecuredText.SecuredText.recipients.Recipient;
import org.SecuredText.SecuredText.recipients.RecipientFactory;
import org.SecuredText.SecuredText.recipients.Recipients;
import org.SecuredText.SecuredText.transport.InsecureFallbackApprovalException;
import org.SecuredText.SecuredText.transport.RetryLaterException;
import org.SecuredText.SecuredText.transport.SecureFallbackApprovalException;
import org.whispersystems.textsecure.api.TextSecureMessageSender;
import org.whispersystems.textsecure.api.crypto.UntrustedIdentityException;
import org.whispersystems.textsecure.api.messages.TextSecureMessage;
import org.whispersystems.textsecure.api.push.TextSecureAddress;
import org.whispersystems.textsecure.api.push.exceptions.UnregisteredUserException;
import org.whispersystems.textsecure.api.util.InvalidNumberException;

import java.io.IOException;

import javax.inject.Inject;

import static org.SecuredText.SecuredText.dependencies.SecuredTextCommunicationModule.TextSecureMessageSenderFactory;

public class PushTextSendJob extends PushSendJob implements InjectableType {

  private static final String TAG = PushTextSendJob.class.getSimpleName();

  @Inject transient TextSecureMessageSenderFactory messageSenderFactory;

  private final long messageId;

  public PushTextSendJob(Context context, long messageId, String destination) {
    super(context, constructParameters(context, destination, false));
    this.messageId = messageId;
  }

  @Override
  public void onAdded() {
    SmsDatabase smsDatabase = DatabaseFactory.getSmsDatabase(context);
    smsDatabase.markAsSending(messageId);
    smsDatabase.markAsPush(messageId);
  }

  @Override
  public void onSend(MasterSecret masterSecret) throws NoSuchMessageException, RetryLaterException {
    EncryptingSmsDatabase database    = DatabaseFactory.getEncryptingSmsDatabase(context);
    SmsMessageRecord      record      = database.getMessage(masterSecret, messageId);
    String                destination = record.getIndividualRecipient().getNumber();

    try {
      Log.w(TAG, "Sending message: " + messageId);

      if (deliver(masterSecret, record, destination)) {
        database.markAsPush(messageId);
        database.markAsSecure(messageId);
        database.markAsSent(messageId);
      }
    } catch (InsecureFallbackApprovalException e) {
      Log.w(TAG, e);
      database.markAsPendingInsecureSmsFallback(record.getId());
      MessageNotifier.notifyMessageDeliveryFailed(context, record.getRecipients(), record.getThreadId());
    } catch (SecureFallbackApprovalException e) {
      Log.w(TAG, e);
      database.markAsPendingSecureSmsFallback(record.getId());
      MessageNotifier.notifyMessageDeliveryFailed(context, record.getRecipients(), record.getThreadId());
    } catch (UntrustedIdentityException e) {
      Log.w(TAG, e);
      Recipients recipients  = RecipientFactory.getRecipientsFromString(context, e.getE164Number(), false);
      long       recipientId = recipients.getPrimaryRecipient().getRecipientId();

      database.addMismatchedIdentity(record.getId(), recipientId, e.getIdentityKey());
      database.markAsSentFailed(record.getId());
      database.markAsPush(record.getId());
    }
  }

  @Override
  public boolean onShouldRetryThrowable(Exception exception) {
    if (exception instanceof RetryLaterException) return true;

    return false;
  }

  @Override
  public void onCanceled() {
    DatabaseFactory.getSmsDatabase(context).markAsSentFailed(messageId);

    long       threadId   = DatabaseFactory.getSmsDatabase(context).getThreadIdForMessage(messageId);
    Recipients recipients = DatabaseFactory.getThreadDatabase(context).getRecipientsForThreadId(threadId);

    MessageNotifier.notifyMessageDeliveryFailed(context, recipients, threadId);
  }

  private boolean deliver(MasterSecret masterSecret, SmsMessageRecord message, String destination)
      throws UntrustedIdentityException, SecureFallbackApprovalException,
             InsecureFallbackApprovalException, RetryLaterException
  {
    boolean isSmsFallbackSupported = isSmsFallbackSupported(context, destination, false);

    try {
      TextSecureAddress       address       = getPushAddress(message.getIndividualRecipient().getNumber());
      TextSecureMessageSender messageSender = messageSenderFactory.create(masterSecret);

      if (message.isEndSession()) {
        messageSender.sendMessage(address, new TextSecureMessage(message.getDateSent(), null,
                                                                 null, null, true, true));
      } else {
        messageSender.sendMessage(address, new TextSecureMessage(message.getDateSent(), message.getBody().getBody()));
      }

      return true;
    } catch (InvalidNumberException | UnregisteredUserException e) {
      Log.w(TAG, e);
      if (isSmsFallbackSupported) fallbackOrAskApproval(masterSecret, message, destination);
      else                        DatabaseFactory.getSmsDatabase(context).markAsSentFailed(messageId);
    } catch (IOException e) {
      Log.w(TAG, e);
      if (isSmsFallbackSupported) fallbackOrAskApproval(masterSecret, message, destination);
      else                        throw new RetryLaterException(e);
    }

    return false;
  }

  private void fallbackOrAskApproval(MasterSecret masterSecret, SmsMessageRecord smsMessage, String destination)
      throws SecureFallbackApprovalException, InsecureFallbackApprovalException
  {
    Recipient recipient                     = smsMessage.getIndividualRecipient();
    boolean   isSmsFallbackApprovalRequired = isSmsFallbackApprovalRequired(destination, false);

    if (!isSmsFallbackApprovalRequired) {
      Log.w(TAG, "Falling back to SMS");
      DatabaseFactory.getSmsDatabase(context).markAsForcedSms(smsMessage.getId());
      ApplicationContext.getInstance(context).getJobManager().add(new SmsSendJob(context, messageId, destination));
    } else if (!SessionUtil.hasSession(context, masterSecret, recipient)) {
      Log.w(TAG, "Marking message as pending insecure fallback.");
      throw new InsecureFallbackApprovalException("Pending user approval for fallback to insecure SMS");
    } else {
      Log.w(TAG, "Marking message as pending secure fallback.");
      throw new SecureFallbackApprovalException("Pending user approval for fallback to secure SMS");
    }
  }


}
