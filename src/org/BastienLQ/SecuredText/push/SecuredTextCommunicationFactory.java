package org.BastienLQ.SecuredText.push;

import android.content.Context;

import org.BastienLQ.SecuredText.Release;
import org.BastienLQ.SecuredText.crypto.SecurityEvent;
import org.BastienLQ.SecuredText.crypto.MasterSecret;
import org.BastienLQ.SecuredText.crypto.storage.SecuredTextAxolotlStore;
import org.BastienLQ.SecuredText.database.DatabaseFactory;
import org.BastienLQ.SecuredText.recipients.RecipientFactory;
import org.BastienLQ.SecuredText.recipients.Recipients;
import org.BastienLQ.SecuredText.util.SecuredTextPreferences;
import org.whispersystems.libaxolotl.util.guava.Optional;
import org.whispersystems.textsecure.api.TextSecureAccountManager;
import org.whispersystems.textsecure.api.TextSecureMessageReceiver;
import org.whispersystems.textsecure.api.TextSecureMessageSender;

import static org.whispersystems.textsecure.api.TextSecureMessageSender.EventListener;

public class SecuredTextCommunicationFactory {

  public static TextSecureAccountManager createManager(Context context) {
    return new TextSecureAccountManager(Release.PUSH_URL,
                                        new SecuredTextPushTrustStore(context),
                                        SecuredTextPreferences.getLocalNumber(context),
                                        SecuredTextPreferences.getPushServerPassword(context));
  }

  public static TextSecureAccountManager createManager(Context context, String number, String password) {
    return new TextSecureAccountManager(Release.PUSH_URL, new SecuredTextPushTrustStore(context),
                                        number, password);
  }

}
