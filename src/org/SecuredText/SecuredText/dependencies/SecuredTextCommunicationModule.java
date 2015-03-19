package org.SecuredText.SecuredText.dependencies;

import android.content.Context;

import org.SecuredText.SecuredText.Release;
import org.SecuredText.SecuredText.crypto.MasterSecret;
import org.SecuredText.SecuredText.crypto.storage.SecuredTextAxolotlStore;
import org.SecuredText.SecuredText.jobs.AttachmentDownloadJob;
import org.SecuredText.SecuredText.jobs.CleanPreKeysJob;
import org.SecuredText.SecuredText.jobs.CreateSignedPreKeyJob;
import org.SecuredText.SecuredText.jobs.DeliveryReceiptJob;
import org.SecuredText.SecuredText.jobs.PushGroupSendJob;
import org.SecuredText.SecuredText.jobs.PushMediaSendJob;
import org.SecuredText.SecuredText.jobs.PushTextSendJob;
import org.SecuredText.SecuredText.jobs.RefreshPreKeysJob;
import org.SecuredText.SecuredText.push.SecurityEventListener;
import org.SecuredText.SecuredText.push.SecuredTextPushTrustStore;
import org.SecuredText.SecuredText.recipients.Recipient;
import org.SecuredText.SecuredText.recipients.RecipientFactory;
import org.SecuredText.SecuredText.recipients.RecipientFormattingException;
import org.SecuredText.SecuredText.service.MessageRetrievalService;
import org.SecuredText.SecuredText.util.SecuredTextPreferences;
import org.whispersystems.libaxolotl.util.guava.Optional;
import org.whispersystems.textsecure.api.TextSecureAccountManager;
import org.whispersystems.textsecure.api.TextSecureMessageReceiver;
import org.whispersystems.textsecure.api.TextSecureMessageSender;
import org.whispersystems.textsecure.api.util.CredentialsProvider;

import dagger.Module;
import dagger.Provides;

@Module(complete = false, injects = {CleanPreKeysJob.class,
                                     CreateSignedPreKeyJob.class,
                                     DeliveryReceiptJob.class,
                                     PushGroupSendJob.class,
                                     PushTextSendJob.class,
                                     PushMediaSendJob.class,
                                     AttachmentDownloadJob.class,
                                     RefreshPreKeysJob.class,
                                     MessageRetrievalService.class})
public class SecuredTextCommunicationModule {

  private final Context context;

  public SecuredTextCommunicationModule(Context context) {
    this.context = context;
  }

  @Provides TextSecureAccountManager provideTextSecureAccountManager() {
    return new TextSecureAccountManager(Release.PUSH_URL,
                                        new SecuredTextPushTrustStore(context),
                                        SecuredTextPreferences.getLocalNumber(context),
                                        SecuredTextPreferences.getPushServerPassword(context));
  }

  @Provides TextSecureMessageSenderFactory provideTextSecureMessageSenderFactory() {
    return new TextSecureMessageSenderFactory() {
      @Override
      public TextSecureMessageSender create(MasterSecret masterSecret) {
        return new TextSecureMessageSender(Release.PUSH_URL,
                                           new SecuredTextPushTrustStore(context),
                                           SecuredTextPreferences.getLocalNumber(context),
                                           SecuredTextPreferences.getPushServerPassword(context),
                                           new SecuredTextAxolotlStore(context, masterSecret),
                                           Optional.of((TextSecureMessageSender.EventListener)
                                                           new SecurityEventListener(context)));
      }
    };
  }

  @Provides TextSecureMessageReceiver provideTextSecureMessageReceiver() {
    return new TextSecureMessageReceiver(Release.PUSH_URL,
                                         new SecuredTextPushTrustStore(context),
                                         new DynamicCredentialsProvider(context));
  }

  public static interface TextSecureMessageSenderFactory {
    public TextSecureMessageSender create(MasterSecret masterSecret);
  }

  private static class DynamicCredentialsProvider implements CredentialsProvider {

    private final Context context;

    private DynamicCredentialsProvider(Context context) {
      this.context = context.getApplicationContext();
    }

    @Override
    public String getUser() {
      return SecuredTextPreferences.getLocalNumber(context);
    }

    @Override
    public String getPassword() {
      return SecuredTextPreferences.getPushServerPassword(context);
    }

    @Override
    public String getSignalingKey() {
      return SecuredTextPreferences.getSignalingKey(context);
    }
  }

}
