package org.SecuredText.SecuredText.push;

import android.content.Context;

import org.SecuredText.SecuredText.Release;
import org.SecuredText.SecuredText.crypto.SecurityEvent;
import org.SecuredText.SecuredText.crypto.MasterSecret;
import org.SecuredText.SecuredText.crypto.storage.SecuredTextAxolotlStore;
import org.SecuredText.SecuredText.database.DatabaseFactory;
import org.SecuredText.SecuredText.recipients.RecipientFactory;
import org.SecuredText.SecuredText.recipients.Recipients;
import org.SecuredText.SecuredText.util.SecuredTextPreferences;
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
