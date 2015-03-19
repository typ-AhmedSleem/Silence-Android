package org.SecuredText.SecuredText.jobs;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.SecuredText.SecuredText.Release;
import org.SecuredText.SecuredText.crypto.MasterSecret;
import org.SecuredText.SecuredText.database.DatabaseFactory;
import org.SecuredText.SecuredText.database.GroupDatabase;
import org.SecuredText.SecuredText.jobs.requirements.MasterSecretRequirement;
import org.SecuredText.SecuredText.push.SecuredTextPushTrustStore;
import org.SecuredText.SecuredText.recipients.Recipient;
import org.SecuredText.SecuredText.recipients.RecipientFactory;
import org.SecuredText.SecuredText.recipients.RecipientFormattingException;
import org.SecuredText.SecuredText.util.BitmapDecodingException;
import org.SecuredText.SecuredText.util.BitmapUtil;
import org.SecuredText.SecuredText.util.GroupUtil;
import org.SecuredText.SecuredText.util.SecuredTextPreferences;
import org.whispersystems.jobqueue.JobParameters;
import org.whispersystems.jobqueue.requirements.NetworkRequirement;
import org.whispersystems.libaxolotl.InvalidMessageException;
import org.whispersystems.textsecure.api.crypto.AttachmentCipherInputStream;
import org.whispersystems.textsecure.internal.push.PushServiceSocket;
import org.whispersystems.textsecure.api.push.exceptions.NonSuccessfulResponseCodeException;
import org.whispersystems.textsecure.internal.util.StaticCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class AvatarDownloadJob extends MasterSecretJob {

  private static final String TAG = AvatarDownloadJob.class.getSimpleName();

  private final byte[] groupId;

  public AvatarDownloadJob(Context context, byte[] groupId) {
    super(context, JobParameters.newBuilder()
                                .withRequirement(new MasterSecretRequirement(context))
                                .withRequirement(new NetworkRequirement(context))
                                .withPersistence()
                                .create());

    this.groupId = groupId;
  }

  @Override
  public void onAdded() {}

  @Override
  public void onRun(MasterSecret masterSecret) throws IOException {
    GroupDatabase             database   = DatabaseFactory.getGroupDatabase(context);
    GroupDatabase.GroupRecord record     = database.getGroup(groupId);
    File                      attachment = null;

    try {
      if (record != null) {
        long   avatarId = record.getAvatarId();
        byte[] key      = record.getAvatarKey();
        String relay    = record.getRelay();

        if (avatarId == -1 || key == null) {
          return;
        }

        attachment = downloadAttachment(relay, avatarId);

        InputStream scaleInputStream   = new AttachmentCipherInputStream(attachment, key);
        InputStream measureInputStream = new AttachmentCipherInputStream(attachment, key);
        Bitmap      avatar             = BitmapUtil.createScaledBitmap(measureInputStream, scaleInputStream, 500, 500);

        database.updateAvatar(groupId, avatar);

        Recipient groupRecipient = RecipientFactory.getRecipientsFromString(context, GroupUtil.getEncodedId(groupId), true)
                                                   .getPrimaryRecipient();
        groupRecipient.setContactPhoto(avatar);
      }
    } catch (InvalidMessageException | BitmapDecodingException | NonSuccessfulResponseCodeException e) {
      Log.w(TAG, e);
    } finally {
      if (attachment != null)
        attachment.delete();
    }
  }

  @Override
  public void onCanceled() {}

  @Override
  public boolean onShouldRetryThrowable(Exception exception) {
    if (exception instanceof IOException) return true;
    return false;
  }

  private File downloadAttachment(String relay, long contentLocation) throws IOException {
    PushServiceSocket socket = new PushServiceSocket(Release.PUSH_URL,
                                                     new SecuredTextPushTrustStore(context),
                                                     new StaticCredentialsProvider(SecuredTextPreferences.getLocalNumber(context),
                                                                                   SecuredTextPreferences.getPushServerPassword(context),
                                                                                   null));

    File destination = File.createTempFile("avatar", "tmp");

    destination.deleteOnExit();

    socket.retrieveAttachment(relay, contentLocation, destination);

    return destination;
  }

}
