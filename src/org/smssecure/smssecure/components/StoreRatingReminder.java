package org.smssecure.smssecure.components;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import com.afollestad.materialdialogs.MaterialDialog;

import org.smssecure.smssecure.R;
import org.smssecure.smssecure.util.SMSSecurePreferences;

import java.util.concurrent.TimeUnit;

/*
  Based on
  https://github.com/WhisperSystems/TextSecure/blob/0efdada92847e3d3351681ac36533cf94ec80863/src/org/thoughtcrime/securesms/components/RatingManager.java
 */

public class StoreRatingReminder extends Reminder{

  private static final String TAG = StoreRatingReminder.class.getSimpleName();

  private static final int DAYS_SINCE_INSTALL_THRESHOLD  = 7;
  private static final int DAYS_UNTIL_REPROMPT_THRESHOLD = 4;

  public StoreRatingReminder(final Context context) {
    super(R.drawable.ic_push_registration_reminder,
            R.string.StoreRatingReminder_title,
            R.string.StoreRatingReminder_message);

    final OnClickListener okListener = new OnClickListener() {
      @Override
      public void onClick(View v) {
        delayRating(context);

        new MaterialDialog.Builder(context)
                .title(context.getString(R.string.StoreRatingReminder_title))
                .content(context.getString(R.string.StoreRatingReminder_message))
                .positiveText(context.getString(R.string.StoreRatingReminder_rate_now))
                .neutralText(context.getString(R.string.StoreRatingReminder_later))
                .negativeText(context.getString(R.string.StoreRatingReminder_no_thanks))
                .callback(new MaterialDialog.ButtonCallback() {
                  @Override
                  public void onPositive(MaterialDialog dialog) {
                    SMSSecurePreferences.setRatingEnabled(context, false);
                    Uri uri = Uri.parse("market://details?id=" + context.getPackageName());
                    context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    super.onPositive(dialog);
                  }

                  @Override
                  public void onNegative(MaterialDialog dialog) {
                    SMSSecurePreferences.setRatingEnabled(context, false);
                    super.onNegative(dialog);
                  }

                  @Override
                  public void onNeutral(MaterialDialog dialog) {
                    super.onNeutral(dialog);
                  }
                })
                .show();
      }
    };
    final OnClickListener cancelListener = new OnClickListener() {
      @Override
      public void onClick(View v) {
        delayRating(context);
      }
    };
    setOkListener(okListener);
    setCancelListener(cancelListener);
  }

  @Override
  public boolean hideAfterTap(){
    return true;
  }

  public static boolean isEligible(Context context) {

    if (!SMSSecurePreferences.isRatingEnabled(context))
      return false;

    // App needs to be installed via Play/Amazon store to show the rating dialog
    String installer = context.getPackageManager().getInstallerPackageName(context.getPackageName());
    if (installer == null || !(installer.equals("com.android.vending") || installer.equals("com.amazon.venezia"))){
      SMSSecurePreferences.setRatingEnabled(context, false);
      return false;
    }

    long daysSinceInstall = getDaysSinceInstalled(context);
    long laterTimestamp   = SMSSecurePreferences.getRatingLaterTimestamp(context);

    return daysSinceInstall >= DAYS_SINCE_INSTALL_THRESHOLD &&
            System.currentTimeMillis() >= laterTimestamp;
  }

  private static void delayRating(Context context){
    long waitUntil = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(DAYS_UNTIL_REPROMPT_THRESHOLD);
    SMSSecurePreferences.setRatingLaterTimestamp(context, waitUntil);
  }

  private static long getDaysSinceInstalled(Context context) {
    try {
      long installTimestamp = context.getPackageManager()
              .getPackageInfo(context.getPackageName(), 0)
              .firstInstallTime;

      return TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - installTimestamp);
    } catch (PackageManager.NameNotFoundException e) {
      Log.w(TAG, e);
      return 0;
    }
  }
}
