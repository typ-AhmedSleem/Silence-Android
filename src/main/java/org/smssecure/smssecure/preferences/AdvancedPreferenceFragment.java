package org.smssecure.smssecure.preferences;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import org.smssecure.smssecure.ApplicationPreferencesActivity;
import org.smssecure.smssecure.LogSubmitActivity;
import org.smssecure.smssecure.R;
import org.smssecure.smssecure.util.SilencePreferences;
import org.smssecure.smssecure.util.dualsim.SubscriptionManagerCompat;

public class AdvancedPreferenceFragment extends ListSummaryPreferenceFragment {
    private static final String TAG = AdvancedPreferenceFragment.class.getSimpleName();

    private static final String SYSTEM_EMOJI_PREF = SilencePreferences.SYSTEM_EMOJI_PREF;
    private static final String SUBMIT_DEBUG_LOG_PREF = "pref_submit_debug_logs";

    @Override
    public void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);

        this.findPreference(SUBMIT_DEBUG_LOG_PREF)
                .setOnPreferenceClickListener(new SubmitDebugLogListener());

        this.findPreference(SilencePreferences.ENTER_KEY_TYPE).setOnPreferenceChangeListener(new ListSummaryListener());
        initializeListSummary((ListPreference) this.findPreference(SilencePreferences.ENTER_KEY_TYPE));

        if (SubscriptionManagerCompat.from(getActivity()).getActiveSubscriptionInfoList().size() <= 1) {
            this.findPreference(SilencePreferences.ASK_FOR_SIM_CARD).setVisible(false);
        }
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_advanced);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((ApplicationPreferencesActivity) getActivity()).getSupportActionBar().setTitle(R.string.preferences__advanced);
    }

    private class SubmitDebugLogListener implements Preference.OnPreferenceClickListener {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            final Intent intent = new Intent(getActivity(), LogSubmitActivity.class);
            startActivity(intent);
            return true;
        }
    }
}
