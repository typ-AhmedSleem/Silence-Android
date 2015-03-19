package org.SecuredText.SecuredText.jobs;

import android.content.Context;
import android.util.Log;

import org.SecuredText.SecuredText.crypto.MasterSecret;
import org.SecuredText.SecuredText.database.DatabaseFactory;
import org.SecuredText.SecuredText.database.SecuredTextDirectory;
import org.SecuredText.SecuredText.jobs.requirements.MasterSecretRequirement;
import org.SecuredText.SecuredText.mms.PartAuthority;
import org.SecuredText.SecuredText.notifications.MessageNotifier;
import org.SecuredText.SecuredText.recipients.Recipients;
import org.SecuredText.SecuredText.util.GroupUtil;
import org.SecuredText.SecuredText.util.SecuredTextPreferences;
import org.SecuredText.SecuredText.util.Util;
import org.whispersystems.jobqueue.JobParameters;
import org.whispersystems.jobqueue.requirements.NetworkRequirement;
import org.whispersystems.libaxolotl.util.guava.Optional;
import org.whispersystems.textsecure.api.messages.TextSecureAttachment;
import org.whispersystems.textsecure.api.messages.TextSecureAttachmentStream;
import org.whispersystems.textsecure.api.push.TextSecureAddress;
import org.whispersystems.textsecure.api.util.InvalidNumberException;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import ws.com.google.android.mms.ContentType;
import ws.com.google.android.mms.pdu.PduPart;
import ws.com.google.android.mms.pdu.SendReq;

public abstract class PushSendJob extends SendJob {

  private static final String TAG = PushSendJob.class.getSimpleName();

  protected PushSendJob(Context context, JobParameters parameters) {
    super(context, parameters);
  }

  protected static JobParameters constructParameters(Context context, String destination, boolean media) {
    JobParameters.Builder builder = JobParameters.newBuilder();
    builder.withPersistence();
    builder.withGroupId(destination);
    builder.withRequirement(new MasterSecretRequirement(context));

    if (!isSmsFallbackSupported(context, destination, media)) {
      builder.withRequirement(new NetworkRequirement(context));
      builder.withRetryCount(5);
    }

    return builder.create();
  }

  protected static boolean isSmsFallbackSupported(Context context, String destination, boolean media) {
    try {
      String e164number = Util.canonicalizeNumber(context, destination);

      if (GroupUtil.isEncodedGroup(e164number)) {
        return false;
      }

      if (!SecuredTextPreferences.isFallbackSmsAllowed(context)) {
        return false;
      }

      if (media && !SecuredTextPreferences.isFallbackMmsEnabled(context)) {
        return false;
      }

      SecuredTextDirectory directory = SecuredTextDirectory.getInstance(context);
      return directory.isSmsFallbackSupported(e164number);
    } catch (InvalidNumberException e) {
      Log.w(TAG, e);
      return false;
    }
  }

  protected TextSecureAddress getPushAddress(String number) throws InvalidNumberException {
    String e164number = Util.canonicalizeNumber(context, number);
    String relay      = SecuredTextDirectory.getInstance(context).getRelay(e164number);
    return new TextSecureAddress(e164number, Optional.fromNullable(relay));
  }

  protected boolean isSmsFallbackApprovalRequired(String destination, boolean media) {
    return (isSmsFallbackSupported(context, destination, media) && SecuredTextPreferences.isFallbackSmsAskRequired(context));
  }

  protected List<TextSecureAttachment> getAttachments(final MasterSecret masterSecret, final SendReq message) {
    List<TextSecureAttachment> attachments = new LinkedList<>();

    for (int i=0;i<message.getBody().getPartsNum();i++) {
      PduPart part = message.getBody().getPart(i);
      String contentType = Util.toIsoString(part.getContentType());
      if (ContentType.isImageType(contentType) ||
          ContentType.isAudioType(contentType) ||
          ContentType.isVideoType(contentType))
      {

        try {
          InputStream is = PartAuthority.getPartStream(context, masterSecret, part.getDataUri());
          attachments.add(new TextSecureAttachmentStream(is, contentType, part.getDataSize()));
        } catch (IOException ioe) {
          Log.w(TAG, "Couldn't open attachment", ioe);
        }
      }
    }

    return attachments;
  }

  protected void notifyMediaMessageDeliveryFailed(Context context, long messageId) {
    long       threadId   = DatabaseFactory.getMmsDatabase(context).getThreadIdForMessage(messageId);
    Recipients recipients = DatabaseFactory.getThreadDatabase(context).getRecipientsForThreadId(threadId);

    MessageNotifier.notifyMessageDeliveryFailed(context, recipients, threadId);
  }
}
