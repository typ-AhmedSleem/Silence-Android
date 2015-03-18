package org.BastienLQ.SecuredText.crypto;

import android.content.Context;
import android.support.annotation.NonNull;

import org.BastienLQ.SecuredText.crypto.storage.SecuredTextSessionStore;
import org.BastienLQ.SecuredText.recipients.Recipient;
import org.whispersystems.libaxolotl.AxolotlAddress;
import org.whispersystems.libaxolotl.state.SessionStore;
import org.whispersystems.textsecure.api.push.TextSecureAddress;

public class SessionUtil {

  public static boolean hasSession(Context context, MasterSecret masterSecret, Recipient recipient) {
    return hasSession(context, masterSecret, recipient.getNumber());
  }

  public static boolean hasSession(Context context, MasterSecret masterSecret, @NonNull String number) {
    SessionStore   sessionStore   = new SecuredTextSessionStore(context, masterSecret);
    AxolotlAddress axolotlAddress = new AxolotlAddress(number, TextSecureAddress.DEFAULT_DEVICE_ID);

    return sessionStore.containsSession(axolotlAddress);
  }
}
