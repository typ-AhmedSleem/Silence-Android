package org.smssecure.smssecure.util;

import android.app.Activity;
import android.content.Intent;
import android.preference.PreferenceManager;

import org.smssecure.smssecure.ApplicationPreferencesActivity;
import org.smssecure.smssecure.ConversationActivity;
import org.smssecure.smssecure.ConversationListActivity;
import org.smssecure.smssecure.R;

public class DynamicTheme {

  private int currentTheme;

  public void onCreate(Activity activity) {
    currentTheme = getSelectedTheme(activity);
    activity.setTheme(currentTheme);
  }

  public void onResume(Activity activity) {
    if (currentTheme != getSelectedTheme(activity)) {
      Intent intent = activity.getIntent();
      activity.finish();
      OverridePendingTransition.invoke(activity);
      activity.startActivity(intent);
      OverridePendingTransition.invoke(activity);
    }
  }

  private static int getSelectedTheme(Activity activity) {
    String theme = SMSSecurePreferences.getTheme(activity);

    if (theme.equals("dark")) return R.style.SMSSecure_DarkTheme;

    return R.style.SMSSecure_LightTheme;
  }

  private static final class OverridePendingTransition {
    static void invoke(Activity activity) {
      activity.overridePendingTransition(0, 0);
    }
  }
}
