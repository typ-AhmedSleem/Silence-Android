package org.BastienLQ.SecuredText.push;

import android.content.Context;

import org.BastienLQ.SecuredText.R;
import org.whispersystems.textsecure.api.push.TrustStore;
import org.whispersystems.textsecure.internal.push.PushServiceSocket;

import java.io.InputStream;

public class SecuredTextPushTrustStore implements TrustStore {

  private final Context context;

  public SecuredTextPushTrustStore(Context context) {
    this.context = context.getApplicationContext();
  }

  @Override
  public InputStream getKeyStoreInputStream() {
    return context.getResources().openRawResource(R.raw.whisper);
  }

  @Override
  public String getKeyStorePassword() {
    return "whisper";
  }
}
