package org.smssecure.smssecure.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import android.text.TextUtils;

import org.smssecure.smssecure.ApplicationPreferencesActivity;
import org.smssecure.smssecure.R;
import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.notifications.MessageNotifier;
import org.smssecure.smssecure.preferences.widgets.AdvancedRingtonePreference;
import org.smssecure.smssecure.util.SilencePreferences;

public class NotificationsPreferenceFragment extends ListSummaryPreferenceFragment {

    private static final String TAG = NotificationsPreferenceFragment.class.getSimpleName();

    private MasterSecret masterSecret;

    public static CharSequence getSummary(Context context) {
        final int onCapsResId = R.string.ApplicationPreferencesActivity_On;
        final int offCapsResId = R.string.ApplicationPreferencesActivity_Off;

        return context.getString(SilencePreferences.isNotificationsEnabled(context) ? onCapsResId : offCapsResId);
    }

    @Override
    public void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        masterSecret = getArguments().getParcelable("master_secret");

        this.findPreference(SilencePreferences.LED_COLOR_PREF)
                .setOnPreferenceChangeListener(new ListSummaryListener());
        this.findPreference(SilencePreferences.LED_BLINK_PREF)
                .setOnPreferenceChangeListener(new ListSummaryListener());
        this.findPreference(SilencePreferences.RINGTONE_PREF)
                .setOnPreferenceChangeListener(new RingtoneSummaryListener());
        this.findPreference(SilencePreferences.REPEAT_ALERTS_PREF)
                .setOnPreferenceChangeListener(new ListSummaryListener());
        this.findPreference(SilencePreferences.NOTIFICATION_PRIVACY_PREF)
                .setOnPreferenceChangeListener(new NotificationPrivacyListener());

        initializeListSummary((ListPreference) findPreference(SilencePreferences.LED_COLOR_PREF));
        initializeListSummary((ListPreference) findPreference(SilencePreferences.LED_BLINK_PREF));
        initializeListSummary((ListPreference) findPreference(SilencePreferences.REPEAT_ALERTS_PREF));
        initializeListSummary((ListPreference) findPreference(SilencePreferences.NOTIFICATION_PRIVACY_PREF));
        initializeRingtoneSummary((AdvancedRingtonePreference) findPreference(SilencePreferences.RINGTONE_PREF));
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_notifications);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((ApplicationPreferencesActivity) getActivity()).getSupportActionBar().setTitle(R.string.preferences__notifications);
    }

    private void initializeRingtoneSummary(AdvancedRingtonePreference pref) {
        RingtoneSummaryListener listener = (RingtoneSummaryListener) pref.getOnPreferenceChangeListener();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String encodedUri = sharedPreferences.getString(pref.getKey(), null);
        Uri uri = !TextUtils.isEmpty(encodedUri) ? Uri.parse(encodedUri) : null;

        listener.onPreferenceChange(pref, uri);
    }

    private class RingtoneSummaryListener implements Preference.OnPreferenceChangeListener {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            Uri value = (Uri) newValue;

            if (value == null) {
                preference.setSummary(R.string.preferences__silent);
            } else {
                Ringtone tone = RingtoneManager.getRingtone(getActivity(), value);
                if (tone != null) {
                    preference.setSummary(tone.getTitle(getActivity()));
                }
            }

            return true;
        }
    }

    private class NotificationPrivacyListener extends ListSummaryListener {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    MessageNotifier.updateNotification(getActivity(), masterSecret);
                    return null;
                }
            }.execute();

            return super.onPreferenceChange(preference, value);
        }

    }
}
