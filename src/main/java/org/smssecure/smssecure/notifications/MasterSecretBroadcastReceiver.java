package org.smssecure.smssecure.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.Nullable;

import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.service.KeyCachingService;

public abstract class MasterSecretBroadcastReceiver extends BroadcastReceiver {

    @Override
    public final void onReceive(Context context, Intent intent) {
        onReceive(context, intent, KeyCachingService.getMasterSecret(context));
    }

    protected abstract void onReceive(Context context, Intent intent, @Nullable MasterSecret masterSecret);
}
