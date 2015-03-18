package org.BastienLQ.SecuredText;

import org.BastienLQ.SecuredText.crypto.MasterSecret;

public interface PassphraseRequiredActivity {
  public void onMasterSecretCleared();
  public void onNewMasterSecret(MasterSecret masterSecret);
}
