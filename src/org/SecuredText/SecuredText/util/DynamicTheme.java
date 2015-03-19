package org.SecuredText.SecuredText.util;

import android.app.Activity;
import android.content.Intent;
import android.preference.PreferenceManager;

import org.SecuredText.SecuredText.ApplicationPreferencesActivity;
import org.SecuredText.SecuredText.ConversationActivity;
import org.SecuredText.SecuredText.ConversationListActivity;
import org.SecuredText.SecuredText.R;

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
    String theme = SecuredTextPreferences.getTheme(activity);

    if (theme.equals("dark")) return R.style.SecuredText_DarkTheme;

    return R.style.SecuredText_LightTheme;
  }

  private static final class OverridePendingTransition {
    static void invoke(Activity activity) {
      activity.overridePendingTransition(0, 0);
    }
  }
}
