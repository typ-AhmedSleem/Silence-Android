package org.SecuredText.SecuredText.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;

import org.SecuredText.SecuredText.ApplicationPreferencesActivity;
import org.SecuredText.SecuredText.R;
import org.SecuredText.SecuredText.util.SecuredTextPreferences;

public class NotificationsPreferenceFragment extends ListSummaryPreferenceFragment {

  @Override
  public void onCreate(Bundle paramBundle) {
    super.onCreate(paramBundle);
    addPreferencesFromResource(R.xml.preferences_notifications);

    this.findPreference(SecuredTextPreferences.LED_COLOR_PREF)
        .setOnPreferenceChangeListener(new ListSummaryListener());
    this.findPreference(SecuredTextPreferences.LED_BLINK_PREF)
        .setOnPreferenceChangeListener(new ListSummaryListener());
    this.findPreference(SecuredTextPreferences.RINGTONE_PREF)
        .setOnPreferenceChangeListener(new RingtoneSummaryListener());
    this.findPreference(SecuredTextPreferences.REPEAT_ALERTS_PREF)
        .setOnPreferenceChangeListener(new ListSummaryListener());

    initializeListSummary((ListPreference) findPreference(SecuredTextPreferences.LED_COLOR_PREF));
    initializeListSummary((ListPreference) findPreference(SecuredTextPreferences.LED_BLINK_PREF));
    initializeListSummary((ListPreference) findPreference(SecuredTextPreferences.REPEAT_ALERTS_PREF));
    initializeRingtoneSummary((RingtonePreference) findPreference(SecuredTextPreferences.RINGTONE_PREF));
  }

  @Override
  public void onResume() {
    super.onResume();
    ((ApplicationPreferencesActivity) getActivity()).getSupportActionBar().setTitle(R.string.preferences__notifications);
  }

  private class RingtoneSummaryListener implements Preference.OnPreferenceChangeListener {
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
      String value = (String) newValue;

      if (TextUtils.isEmpty(value)) {
        preference.setSummary(R.string.preferences__default);
      } else {
        Ringtone tone = RingtoneManager.getRingtone(getActivity(), Uri.parse(value));
        if (tone != null) {
          preference.setSummary(tone.getTitle(getActivity()));
        }
      }

      return true;
    }
  }

  private void initializeRingtoneSummary(RingtonePreference pref) {
    RingtoneSummaryListener listener =
      (RingtoneSummaryListener) pref.getOnPreferenceChangeListener();
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

    listener.onPreferenceChange(pref, sharedPreferences.getString(pref.getKey(), ""));
  }

  public static CharSequence getSummary(Context context) {
    final int onCapsResId   = R.string.ApplicationPreferencesActivity_On;
    final int offCapsResId  = R.string.ApplicationPreferencesActivity_Off;

    return context.getString(SecuredTextPreferences.isNotificationsEnabled(context) ? onCapsResId : offCapsResId);
  }
}
