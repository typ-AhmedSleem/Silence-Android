package org.smssecure.smssecure.service;

import android.app.IntentService;
import android.content.Intent;
import androidx.annotation.Nullable;

import org.smssecure.smssecure.crypto.MasterSecret;

public abstract class MasterSecretIntentService extends IntentService {

    public MasterSecretIntentService(String name) {
        super(name);
    }

    @Override
    protected final void onHandleIntent(Intent intent) {
        onHandleIntent(intent, KeyCachingService.getMasterSecret(this));
    }

    protected abstract void onHandleIntent(Intent intent, @Nullable MasterSecret masterSecret);
}
