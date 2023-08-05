package org.smssecure.smssecure;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.MenuItem;

import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.util.DynamicLanguage;
import org.smssecure.smssecure.util.DynamicTheme;


public class ImportExportActivity extends PassphraseRequiredActionBarActivity {

    private final DynamicTheme dynamicTheme = new DynamicTheme();
    private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

    @Override
    protected void onPreCreate() {
        dynamicTheme.onCreate(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState, @NonNull MasterSecret masterSecret) {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        initFragment(android.R.id.content, new ImportExportFragment(),
                masterSecret, dynamicLanguage.getCurrentLocale());
    }

    @Override
    public void onResume() {
        dynamicTheme.onResume(this);
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return false;
    }
}
