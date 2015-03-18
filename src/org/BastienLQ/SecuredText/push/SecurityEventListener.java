package org.BastienLQ.SecuredText.push;

import android.content.Context;

import org.BastienLQ.SecuredText.crypto.SecurityEvent;
import org.BastienLQ.SecuredText.database.DatabaseFactory;
import org.BastienLQ.SecuredText.recipients.RecipientFactory;
import org.BastienLQ.SecuredText.recipients.Recipients;
import org.whispersystems.textsecure.api.TextSecureMessageSender;
import org.whispersystems.textsecure.api.push.TextSecureAddress;

public class SecurityEventListener implements TextSecureMessageSender.EventListener {

  private static final String TAG = SecurityEventListener.class.getSimpleName();

  private final Context context;

  public SecurityEventListener(Context context) {
    this.context = context.getApplicationContext();
  }

  @Override
  public void onSecurityEvent(TextSecureAddress textSecureAddress) {
    Recipients recipients = RecipientFactory.getRecipientsFromString(context, textSecureAddress.getNumber(), false);
    long       threadId   = DatabaseFactory.getThreadDatabase(context).getThreadIdFor(recipients);

    SecurityEvent.broadcastSecurityUpdateEvent(context, threadId);
  }
}
