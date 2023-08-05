package org.smssecure.smssecure.preferences.widgets;


import android.content.Context;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.TextView;

import org.smssecure.smssecure.R;

public class SilenceRingtonePreference extends AdvancedRingtonePreference {

    private TextView rightSummary;
    private CharSequence summary;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public SilenceRingtonePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize();
    }

    public SilenceRingtonePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    public SilenceRingtonePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public SilenceRingtonePreference(Context context) {
        super(context);
        initialize();
    }

    private void initialize() {
        setWidgetLayoutResource(R.layout.preference_right_summary_widget);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);

        this.rightSummary = (TextView) view.findViewById(R.id.right_summary);
        setSummary(summary);
    }

    @Override
    public void setSummary(CharSequence summary) {
        this.summary = summary;

        super.setSummary(null);

        if (rightSummary != null) {
            rightSummary.setText(summary);
        }
    }

}
