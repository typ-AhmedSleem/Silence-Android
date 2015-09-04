package org.smssecure.smssecure.jobs;

import android.content.Context;
import android.util.Log;


import org.smssecure.smssecure.crypto.MasterCipher;
import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.database.DatabaseFactory;
import org.smssecure.smssecure.database.PartDatabase;
import org.smssecure.smssecure.database.PartDatabase.PartId;
import org.smssecure.smssecure.dependencies.InjectableType;
import org.smssecure.smssecure.jobs.requirements.MasterSecretRequirement;
import org.smssecure.smssecure.jobs.requirements.MediaNetworkRequirement;
import org.smssecure.smssecure.notifications.MessageNotifier;
import org.smssecure.smssecure.util.Base64;
import org.smssecure.smssecure.util.Util;
import org.whispersystems.jobqueue.JobParameters;
import org.whispersystems.jobqueue.requirements.NetworkRequirement;
import org.whispersystems.libaxolotl.InvalidMessageException;
import org.whispersystems.textsecure.api.TextSecureMessageReceiver;
import org.whispersystems.textsecure.api.messages.TextSecureAttachment.ProgressListener;
import org.whispersystems.textsecure.api.messages.TextSecureAttachmentPointer;
import org.whispersystems.textsecure.api.push.exceptions.NonSuccessfulResponseCodeException;
import org.whispersystems.textsecure.api.push.exceptions.PushNetworkException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import ws.com.google.android.mms.MmsException;
import ws.com.google.android.mms.pdu.PduPart;

public class AttachmentDownloadJob extends MasterSecretJob implements InjectableType {
  private static final long   serialVersionUID = 1L;
  private static final String TAG              = AttachmentDownloadJob.class.getSimpleName();

  @Inject transient TextSecureMessageReceiver messageReceiver;

  private final long messageId;
  private final long partRowId;
  private final long partUniqueId;

  public AttachmentDownloadJob(Context context, long messageId, PartId partId) {
    super(context, JobParameters.newBuilder()
                                .withGroupId(AttachmentDownloadJob.class.getCanonicalName())
                                .withRequirement(new MasterSecretRequirement(context))
                                .withRequirement(new NetworkRequirement(context))
                                .withRequirement(new MediaNetworkRequirement(context, messageId, partId))
                                .withPersistence()
                                .create());

    this.messageId    = messageId;
    this.partRowId    = partId.getRowId();
    this.partUniqueId = partId.getUniqueId();
  }

  @Override
  public void onAdded() {
  }

  @Override
  public void onRun(MasterSecret masterSecret) throws IOException {
    final PartId  partId = new PartId(partRowId, partUniqueId);
    final PduPart part   = DatabaseFactory.getPartDatabase(context).getPart(partId);

    Log.w(TAG, "Downloading push part " + partId);

    retrievePart(masterSecret, part, messageId);
    MessageNotifier.updateNotification(context, masterSecret);
  }

  @Override
  public void onCanceled() {
    final PartId  partId = new PartId(partRowId, partUniqueId);
    final PduPart part   = DatabaseFactory.getPartDatabase(context).getPart(partId);
    markFailed(messageId, part, part.getPartId());
  }

  @Override
  public boolean onShouldRetryThrowable(Exception exception) {
    return (exception instanceof PushNetworkException);
  }

  private void retrievePart(MasterSecret masterSecret, PduPart part, long messageId)
      throws IOException
  {

    PartDatabase database       = DatabaseFactory.getPartDatabase(context);
    File         attachmentFile = null;

    final PartId partId = part.getPartId();
    try {
      attachmentFile = createTempFile();

      TextSecureAttachmentPointer pointer    = createAttachmentPointer(masterSecret, part);
      InputStream                 attachment = messageReceiver.retrieveAttachment(pointer, attachmentFile, new ProgressListener() {
        @Override public void onAttachmentProgress(long total, long progress) {
          EventBus.getDefault().postSticky(new PartProgressEvent(partId, total, progress));
        }
      });

      database.updateDownloadedPart(masterSecret, messageId, partId, part, attachment);
    } catch (InvalidPartException | NonSuccessfulResponseCodeException | InvalidMessageException | MmsException e) {
      Log.w(TAG, e);
      markFailed(messageId, part, partId);
    } finally {
      if (attachmentFile != null)
        attachmentFile.delete();
    }
  }

  private TextSecureAttachmentPointer createAttachmentPointer(MasterSecret masterSecret, PduPart part)
      throws InvalidPartException
  {
    if (part.getContentLocation() == null) throw new InvalidPartException("null content location");

    try {
      MasterCipher masterCipher = new MasterCipher(masterSecret);
      long         id           = Long.parseLong(Util.toIsoString(part.getContentLocation()));
      byte[]       key          = masterCipher.decryptBytes(Base64.decode(Util.toIsoString(part.getContentDisposition())));
      String       relay        = null;

      if (part.getName() != null) {
        relay = Util.toIsoString(part.getName());
      }

      return new TextSecureAttachmentPointer(id, null, key, relay);
    } catch (InvalidMessageException | IOException e) {
      Log.w(TAG, e);
      throw new InvalidPartException(e);
    }
  }

  private File createTempFile() throws InvalidPartException {
    try {
      File file = File.createTempFile("push-attachment", "tmp", context.getCacheDir());
      file.deleteOnExit();

      return file;
    } catch (IOException e) {
      throw new InvalidPartException(e);
    }
  }

  private void markFailed(long messageId, PduPart part, PartDatabase.PartId partId) {
    try {
      PartDatabase database = DatabaseFactory.getPartDatabase(context);
      database.updateFailedDownloadedPart(messageId, partId, part);
    } catch (MmsException e) {
      Log.w(TAG, e);
    }
  }

  private static class InvalidPartException extends Exception {
    public InvalidPartException(String s) {super(s);}
    public InvalidPartException(Exception e) {super(e);}
  }

}
