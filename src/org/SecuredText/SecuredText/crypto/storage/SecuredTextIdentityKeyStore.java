package org.SecuredText.SecuredText.crypto.storage;

import android.content.Context;

import org.SecuredText.SecuredText.crypto.IdentityKeyUtil;
import org.SecuredText.SecuredText.crypto.MasterSecret;
import org.SecuredText.SecuredText.database.DatabaseFactory;
import org.SecuredText.SecuredText.recipients.RecipientFactory;
import org.SecuredText.SecuredText.util.SecuredTextPreferences;
import org.whispersystems.libaxolotl.IdentityKey;
import org.whispersystems.libaxolotl.IdentityKeyPair;
import org.whispersystems.libaxolotl.state.IdentityKeyStore;

public class SecuredTextIdentityKeyStore implements IdentityKeyStore {

  private final Context      context;
  private final MasterSecret masterSecret;

  public SecuredTextIdentityKeyStore(Context context, MasterSecret masterSecret) {
    this.context      = context;
    this.masterSecret = masterSecret;
  }

  @Override
  public IdentityKeyPair getIdentityKeyPair() {
    return IdentityKeyUtil.getIdentityKeyPair(context, masterSecret);
  }

  @Override
  public int getLocalRegistrationId() {
    return SecuredTextPreferences.getLocalRegistrationId(context);
  }

  @Override
  public void saveIdentity(String name, IdentityKey identityKey) {
    long recipientId = RecipientFactory.getRecipientsFromString(context, name, true).getPrimaryRecipient().getRecipientId();
    DatabaseFactory.getIdentityDatabase(context).saveIdentity(masterSecret, recipientId, identityKey);
  }

  @Override
  public boolean isTrustedIdentity(String name, IdentityKey identityKey) {
    long recipientId = RecipientFactory.getRecipientsFromString(context, name, true).getPrimaryRecipient().getRecipientId();
    return DatabaseFactory.getIdentityDatabase(context)
                          .isValidIdentity(masterSecret, recipientId, identityKey);
  }
}
