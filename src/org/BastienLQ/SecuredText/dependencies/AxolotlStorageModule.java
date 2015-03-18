package org.BastienLQ.SecuredText.dependencies;

import android.content.Context;

import org.BastienLQ.SecuredText.crypto.MasterSecret;
import org.BastienLQ.SecuredText.crypto.storage.SecuredTextAxolotlStore;
import org.BastienLQ.SecuredText.jobs.CleanPreKeysJob;
import org.whispersystems.libaxolotl.state.SignedPreKeyStore;

import dagger.Module;
import dagger.Provides;

@Module (complete = false, injects = {CleanPreKeysJob.class})
public class AxolotlStorageModule {

  private final Context context;

  public AxolotlStorageModule(Context context) {
    this.context = context;
  }

  @Provides SignedPreKeyStoreFactory provideSignedPreKeyStoreFactory() {
    return new SignedPreKeyStoreFactory() {
      @Override
      public SignedPreKeyStore create(MasterSecret masterSecret) {
        return new SecuredTextAxolotlStore(context, masterSecret);
      }
    };
  }

  public static interface SignedPreKeyStoreFactory {
    public SignedPreKeyStore create(MasterSecret masterSecret);
  }
}
