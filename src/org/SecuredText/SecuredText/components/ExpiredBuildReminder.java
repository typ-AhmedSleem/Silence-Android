package org.SecuredText.SecuredText.components;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Telephony;
import android.util.Log;
import android.view.View;

import org.SecuredText.SecuredText.R;
import org.SecuredText.SecuredText.SecuredTextExpiredException;
import org.SecuredText.SecuredText.util.SecuredTextPreferences;
import org.SecuredText.SecuredText.util.Util;

public class ExpiredBuildReminder extends Reminder {

  private static final String TAG = ExpiredBuildReminder.class.getSimpleName();

  public ExpiredBuildReminder() {
    super(R.drawable.ic_dialog_alert_holo_dark,
          R.string.reminder_header_expired_build,
          R.string.reminder_header_expired_build_details);
  }

  @Override
  public boolean isDismissable() {
    return false;
  }

  public static boolean isEligible(Context context) {
    return !Util.isBuildFresh();
  }

}
