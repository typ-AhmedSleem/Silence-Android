package org.smssecure.smssecure;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import org.smssecure.smssecure.BaseActionBarActivity;
import org.smssecure.smssecure.R;
import org.smssecure.smssecure.permissions.Permissions;
import org.smssecure.smssecure.util.ServiceUtil;
import org.smssecure.smssecure.util.SilencePreferences;
import org.smssecure.smssecure.util.Util;

public class WelcomeActivity extends BaseActionBarActivity {

  private int backgroundColor = 0xFF7568AE;

  private static final int NOTIFICATION_ID = 1339;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setStatusBarColor();
    if (SilencePreferences.isFirstRun((Context)WelcomeActivity.this)) {
      setContentView(R.layout.welcome_activity);
    } else {
      setContentView(R.layout.welcome_activity_update);
    }
    findViewById(R.id.welcome_continue_button).setOnClickListener(v -> onContinueClicked());
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
  }

  private void onContinueClicked() {
    Permissions.with(this)
        .request(Manifest.permission.WRITE_CONTACTS,
                 Manifest.permission.READ_CONTACTS,
                 Manifest.permission.READ_PHONE_STATE,
                 Manifest.permission.RECEIVE_SMS,
                 Manifest.permission.RECEIVE_MMS,
                 Manifest.permission.READ_SMS,
                 Manifest.permission.SEND_SMS)
        .ifNecessary()
        .withRationaleDialog(getString(R.string.WelcomeActivity_silence_needs_access_to_your_contacts_phone_status_and_sms),
          R.drawable.ic_contacts_white_48dp, R.drawable.ic_phone_white_48dp)
        .onAnyResult(() -> {
          SilencePreferences.setFirstRun((Context)WelcomeActivity.this);
          SilencePreferences.setPermissionsAsked((Context)WelcomeActivity.this);

          Intent nextIntent = getIntent().getParcelableExtra("next_intent");

          if (nextIntent == null) {
            throw new IllegalStateException("Was not supplied a next_intent.");
          }

          startActivity(nextIntent);
          overridePendingTransition(R.anim.slide_from_right, R.anim.fade_scale_out);
          finish();
        })
        .execute();
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  private void setStatusBarColor() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      getWindow().setStatusBarColor(backgroundColor);
    }
  }

  public static class AppUpgradeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      if(Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction()) &&
         intent.getData().getSchemeSpecificPart().equals(context.getPackageName()))
      {
        if (SilencePreferences.isFirstRun(context) || !Util.shouldDisplayUpgradeNotification(context)) return;

        Intent       targetIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        Notification notification = new NotificationCompat.Builder(context)
                                        .setPriority(Notification.PRIORITY_MAX)
                                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                                        .setSmallIcon(R.drawable.icon_notification)
                                        .setColor(context.getResources().getColor(R.color.silence_primary))
                                        .setContentTitle(context.getString(R.string.WelcomeActivity_action_required))
                                        .setContentText(context.getString(R.string.WelcomeActivity_you_need_to_grant_some_permissions_to_silence))
                                        .setStyle(new NotificationCompat.BigTextStyle().bigText(context.getString(R.string.WelcomeActivity_you_need_to_grant_some_permissions_to_silence_in_order_to_continue_to_use_it)))
                                        .setAutoCancel(false)
                                        .setContentIntent(PendingIntent.getActivity(context, 0,
                                                                                    targetIntent,
                                                                                    PendingIntent.FLAG_UPDATE_CURRENT))
                                        .build();
        ServiceUtil.getNotificationManager(context).notify(NOTIFICATION_ID, notification);
      }
    }
  }
}
