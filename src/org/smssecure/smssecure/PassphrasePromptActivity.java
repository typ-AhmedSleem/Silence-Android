/**
 * Copyright (C) 2011 Whisper Systems
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.smssecure.smssecure;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.TypefaceSpan;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import org.smssecure.smssecure.crypto.InvalidPassphraseException;
import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.crypto.MasterSecretUtil;
import org.smssecure.smssecure.util.DynamicIntroTheme;
import org.smssecure.smssecure.util.DynamicLanguage;

/**
 * Activity that prompts for a user's passphrase.
 *
 * @author Moxie Marlinspike
 */
public class PassphrasePromptActivity extends PassphraseActivity {

    private final DynamicIntroTheme dynamicTheme = new DynamicIntroTheme();
    private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

    private EditText passphraseText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        dynamicTheme.onCreate(this);
        dynamicLanguage.onCreate(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.prompt_passphrase_activity);
        initializeResources();
    }

    @Override
    public void onResume() {
        super.onResume();
        dynamicTheme.onResume(this);
        dynamicLanguage.onResume(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuInflater inflater = this.getMenuInflater();
        menu.clear();

        inflater.inflate(R.menu.log_submit, menu);
        super.onPrepareOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        if (item.getItemId() == R.id.menu_submit_debug_logs) {
            handleLogSubmit();
            return true;
        }

        return false;
    }

    private void handleLogSubmit() {
        Intent intent = new Intent(this, LogSubmitActivity.class);
        startActivity(intent);
    }

    private void handlePassphrase() {
        try {
            Editable text = passphraseText.getText();
            String passphrase = (text == null ? "" : text.toString());
            MasterSecret masterSecret = MasterSecretUtil.getMasterSecret(this, passphrase);

            setMasterSecret(masterSecret);
        } catch (InvalidPassphraseException ipe) {
            passphraseText.setText("");
            passphraseText.setError(
                    getString(R.string.PassphrasePromptActivity_invalid_passphrase_exclamation));
        }
    }

    private void initializeResources() {
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.centered_app_title);

        ImageButton okButton = findViewById(R.id.ok_button);
        passphraseText = findViewById(R.id.passphrase_edit);
        SpannableString hint = new SpannableString("  " + getString(R.string.PassphrasePromptActivity_enter_passphrase));
        hint.setSpan(new RelativeSizeSpan(0.9f), 0, hint.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        hint.setSpan(new TypefaceSpan("sans-serif"), 0, hint.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        passphraseText.setHint(hint);
        okButton.setOnClickListener(new OkButtonClickListener());
        passphraseText.setOnEditorActionListener(new PassphraseActionListener());
        passphraseText.setImeActionLabel(getString(R.string.prompt_passphrase_activity__unlock),
                EditorInfo.IME_ACTION_DONE);
    }

    @Override
    protected void cleanup() {
        this.passphraseText.setText("");
        System.gc();
    }

    private class PassphraseActionListener implements TextView.OnEditorActionListener {
        @Override
        public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent keyEvent) {
            if ((keyEvent == null && actionId == EditorInfo.IME_ACTION_DONE) ||
                    (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN &&
                            (actionId == EditorInfo.IME_NULL))) {
                handlePassphrase();
                return true;
            } else return keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_UP &&
                    actionId == EditorInfo.IME_NULL;
        }
    }

    private class OkButtonClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            handlePassphrase();
        }
    }
}
