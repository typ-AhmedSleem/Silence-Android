package org.BastienLQ.SecuredText.dependencies;

import android.content.Context;

import org.BastienLQ.SecuredText.Release;
import org.BastienLQ.SecuredText.crypto.MasterSecret;
import org.BastienLQ.SecuredText.crypto.storage.SecuredTextAxolotlStore;
import org.BastienLQ.SecuredText.jobs.AttachmentDownloadJob;
import org.BastienLQ.SecuredText.jobs.CleanPreKeysJob;
import org.BastienLQ.SecuredText.jobs.CreateSignedPreKeyJob;
import org.BastienLQ.SecuredText.jobs.DeliveryReceiptJob;
import org.BastienLQ.SecuredText.jobs.PushGroupSendJob;
import org.BastienLQ.SecuredText.jobs.PushMediaSendJob;
import org.BastienLQ.SecuredText.jobs.PushTextSendJob;
import org.BastienLQ.SecuredText.jobs.RefreshPreKeysJob;
import org.BastienLQ.SecuredText.push.SecurityEventListener;
import org.BastienLQ.SecuredText.push.SecuredTextPushTrustStore;
import org.BastienLQ.SecuredText.recipients.Recipient;
import org.BastienLQ.SecuredText.recipients.RecipientFactory;
import org.BastienLQ.SecuredText.recipients.RecipientFormattingException;
import org.BastienLQ.SecuredText.service.MessageRetrievalService;
import org.BastienLQ.SecuredText.util.SecuredTextPreferences;
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
