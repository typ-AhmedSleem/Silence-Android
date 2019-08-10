package org.smssecure.smssecure.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.smssecure.smssecure.WelcomeActivity;

public class BootReceiver extends BroadcastReceiver {
  private static final String TAG = BootReceiver.class.getSimpleName();

  @Override
  public void onReceive(Context context, Intent intent) {
    if((Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction()) ||
      Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) &&
      intent.getData().getSchemeSpecificPart().equals(context.getPackageName()))
    {
      Log.w(TAG, "onReceive()");
      WelcomeActivity.checkForPermissions(context, intent);
    }
  }

}
