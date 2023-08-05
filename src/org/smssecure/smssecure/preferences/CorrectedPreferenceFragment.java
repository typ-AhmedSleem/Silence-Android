package org.smssecure.smssecure.preferences;

import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import android.view.View;

import org.smssecure.smssecure.components.CustomDefaultPreference;
import org.smssecure.smssecure.preferences.widgets.ColorPickerPreference;
import org.smssecure.smssecure.preferences.widgets.ColorPickerPreferenceDialogFragmentCompat;
import org.smssecure.smssecure.preferences.widgets.RingtonePreference;
import org.smssecure.smssecure.preferences.widgets.RingtonePreferenceDialogFragmentCompat;

public abstract class CorrectedPreferenceFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View lv = getView().findViewById(android.R.id.list);
        if (lv != null) lv.setPadding(0, 0, 0, 0);
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        DialogFragment dialogFragment = null;

        if (preference instanceof RingtonePreference) {
            dialogFragment = RingtonePreferenceDialogFragmentCompat.newInstance(preference.getKey());
        } else if (preference instanceof ColorPickerPreference) {
            dialogFragment = ColorPickerPreferenceDialogFragmentCompat.newInstance(preference.getKey());
        } else if (preference instanceof CustomDefaultPreference) {
            dialogFragment = CustomDefaultPreference.CustomDefaultPreferenceDialogFragmentCompat.newInstance(preference.getKey());
        }

        if (dialogFragment != null) {
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(getFragmentManager(), "android.support.v7.preference.PreferenceFragment.DIALOG");
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }


}
