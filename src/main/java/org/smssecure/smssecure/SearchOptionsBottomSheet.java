package org.smssecure.smssecure;

import static android.content.Context.MODE_PRIVATE;
import static org.smssecure.smssecure.AdvancedSearchOptions.DEFAULT_MSG_LIMIT;
import static org.smssecure.smssecure.AdvancedSearchOptions.DEFAULT_RESULTS_LIMIT;
import static org.smssecure.smssecure.AdvancedSearchOptions.PREF_NAME;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class SearchOptionsBottomSheet extends BottomSheetDialog {

    // Views
    private final TextInputEditText inputResultsLimit;
    private final TextInputEditText inputMsgLimit;
    private final CheckBox chkUnreadOnly;
    private final CheckBox chkIncludeArchived;
    private final CheckBox chkPinnedOnly;

    // Runtime
    private AdvancedSearchOptions options;

    @SuppressLint("InflateParams")
    public SearchOptionsBottomSheet(@NonNull Context context, @NonNull Callback callback) {
        super(context);
        // Init views
        final View bsView = LayoutInflater.from(context).inflate(R.layout.bs_advanced_search, null);
        setContentView(bsView);
        inputResultsLimit = (TextInputEditText) ((TextInputLayout) bsView.findViewById(R.id.til_results_limit)).getEditText();
        inputMsgLimit = (TextInputEditText) ((TextInputLayout) bsView.findViewById(R.id.til_msg_limit)).getEditText();
        chkUnreadOnly = bsView.findViewById(R.id.chk_unread_only);
        chkIncludeArchived = bsView.findViewById(R.id.chk_include_archived);
        chkPinnedOnly = bsView.findViewById(R.id.chk_pinned_only);
        // Listeners and Callbacks
        bsView.findViewById(R.id.btn_reset).setOnClickListener(v -> resetOptions(context, callback));
        bsView.findViewById(R.id.btn_perform_search).setOnClickListener(v -> updateOptions(callback));
        // Load last options then update views
        this.options = loadLastOptions(context);
        this.updateViews();
        callback.onLoadLastOptions(this.options);
    }

    private AdvancedSearchOptions loadLastOptions(Context ctx) {
        final SharedPreferences prefs = ctx.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        return new AdvancedSearchOptions.Builder()
                .setResultsLimit(prefs.getInt(AdvancedSearchOptions.KEY_RESULTS_LIMIT, DEFAULT_RESULTS_LIMIT))
                .setMsgLimit(prefs.getInt(AdvancedSearchOptions.KEY_MSG_LIMIT, DEFAULT_MSG_LIMIT))
                .setUnreadOnly(prefs.getBoolean(AdvancedSearchOptions.KEY_UNREAD_ONLY, false))
                .setIncludeArchived(prefs.getBoolean(AdvancedSearchOptions.KEY_INCLUDE_ARCHIVED, false))
                .setPinnedOnly(prefs.getBoolean(AdvancedSearchOptions.KEY_PINNED_ONLY, false))
                .build();
    }

    private void updateOptions(Callback callback) {
        // Fire callback
        if (callback != null) {
            // Build options
            final AdvancedSearchOptions options = new AdvancedSearchOptions.Builder()
                    .setResultsLimit(getResultsLimit())
                    .setMsgLimit(getMsgLimit())
                    .setUnreadOnly(chkUnreadOnly.isChecked())
                    .setIncludeArchived(chkIncludeArchived.isChecked())
                    .setPinnedOnly(chkPinnedOnly.isChecked())
                    .build();

            this.options = options;
            this.saveOptions(options);
            callback.onSetSearchOptions(options);
            this.cancel();
        }
    }

    private void saveOptions(AdvancedSearchOptions options) {
        if (options == null) return;
        final SharedPreferences.Editor editor = getContext().getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit();
        editor.putInt(AdvancedSearchOptions.KEY_RESULTS_LIMIT, options.getResultsLimit());
        editor.putInt(AdvancedSearchOptions.KEY_MSG_LIMIT, options.getMsgLimit());
        editor.putBoolean(AdvancedSearchOptions.KEY_UNREAD_ONLY, options.isUnreadOnly());
        editor.putBoolean(AdvancedSearchOptions.KEY_INCLUDE_ARCHIVED, options.isIncludeArchived());
        editor.putBoolean(AdvancedSearchOptions.KEY_PINNED_ONLY, options.isPinnedOnly());
        editor.apply();
    }

    private void resetOptions(Context context, Callback callback) {
        if (callback != null) {
            options = loadLastOptions(context);
            saveOptions(options);
        }
    }

    private int getResultsLimit() {
        try {
            return Integer.parseInt(inputResultsLimit.getText().toString());
        } catch (Throwable ignored) {
            return DEFAULT_RESULTS_LIMIT;
        }
    }

    private int getMsgLimit() {
        try {
            return Integer.parseInt(inputMsgLimit.getText().toString());
        } catch (Throwable ignored) {
            return DEFAULT_MSG_LIMIT;
        }
    }

    private void updateViews() {
        inputResultsLimit.setText(String.valueOf(options != null ? options.getResultsLimit() : DEFAULT_RESULTS_LIMIT));
        inputMsgLimit.setText(String.valueOf(options != null ? options.getMsgLimit() : DEFAULT_MSG_LIMIT));
        chkUnreadOnly.setChecked(options != null && options.isUnreadOnly());
        chkIncludeArchived.setChecked(options != null && options.isIncludeArchived());
        chkPinnedOnly.setChecked(options != null && options.isPinnedOnly());
    }

    @Override
    public void show() {
        this.updateViews();
        super.show();
    }

    public interface Callback {
        void onLoadLastOptions(AdvancedSearchOptions searchOptions);

        void onSetSearchOptions(AdvancedSearchOptions searchOptions);

    }

}
