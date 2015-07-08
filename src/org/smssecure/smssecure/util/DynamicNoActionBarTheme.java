package org.smssecure.smssecure.util;

import android.app.Activity;

import org.smssecure.smssecure.R;

public class DynamicNoActionBarTheme extends DynamicTheme {
  @Override
  protected int getSelectedTheme(Activity activity) {
    String theme = SMSSecurePreferences.getTheme(activity);

    if (theme.equals("dark")) return R.style.SMSSecure_DarkNoActionBar;

    return R.style.SMSSecure_LightNoActionBar;
  }
}
