package org.smssecure.smssecure.util;

import android.app.Activity;

import org.smssecure.smssecure.R;

public class DynamicIntroTheme extends DynamicTheme {
  @Override
  protected int getSelectedTheme(Activity activity) {
    String theme = SMSSecurePreferences.getTheme(activity);

    if (theme.equals("dark")) return R.style.SMSSecure_DarkIntroTheme;

    return R.style.SMSSecure_LightIntroTheme;
  }
}
