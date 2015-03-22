package org.smssecure.smssecure;

import org.smssecure.smssecure.crypto.MasterSecret;

public interface PassphraseRequiredActivity {
  public void onMasterSecretCleared();
  public void onNewMasterSecret(MasterSecret masterSecret);
}
