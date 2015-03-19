package org.SecuredText.SecuredText;

import org.SecuredText.SecuredText.crypto.MasterSecret;

public interface PassphraseRequiredActivity {
  public void onMasterSecretCleared();
  public void onNewMasterSecret(MasterSecret masterSecret);
}
