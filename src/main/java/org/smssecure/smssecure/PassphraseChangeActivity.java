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

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import org.smssecure.smssecure.crypto.InvalidPassphraseException;
import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.crypto.MasterSecretUtil;
import org.smssecure.smssecure.util.DynamicLanguage;
import org.smssecure.smssecure.util.DynamicTheme;
import org.smssecure.smssecure.util.SilencePreferences;

/**
 * Activity for changing a user's local encryption passphrase.
 *
 * @author Moxie Marlinspike
 */

public class PassphraseChangeActivity extends PassphraseActivity {

    private final DynamicTheme dynamicTheme = new DynamicTheme();
    private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

    private EditText originalPassphrase;
    private EditText newPassphrase;
    private EditText repeatPassphrase;
    private Button okButton;
    private Button cancelButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        dynamicTheme.onCreate(this);
        dynamicLanguage.onCreate(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.change_passphrase_activity);

        initializeResources();
    }

    @Override
    public void onResume() {
        super.onResume();
        dynamicTheme.onResume(this);
        dynamicLanguage.onResume(this);
    }

    private void initializeResources() {
        this.originalPassphrase = findViewById(R.id.old_passphrase);
        this.newPassphrase = findViewById(R.id.new_passphrase);
        this.repeatPassphrase = findViewById(R.id.repeat_passphrase);

        this.okButton = findViewById(R.id.ok_button);
        this.cancelButton = findViewById(R.id.cancel_button);

        this.okButton.setOnClickListener(new OkButtonClickListener());
        this.cancelButton.setOnClickListener(new CancelButtonClickListener());

        if (SilencePreferences.isPasswordDisabled(this)) {
            this.originalPassphrase.setVisibility(View.GONE);
        } else {
            this.originalPassphrase.setVisibility(View.VISIBLE);
        }
    }

    private void verifyAndSavePassphrases() {
        String original;
        String passphrase;
        String passphraseRepeat;

        if (this.originalPassphrase == null) {
            original = "";
        } else {
            Editable originalText = this.originalPassphrase.getText();
            original = (originalText == null ? "" : originalText.toString());
        }

        if (this.newPassphrase == null) {
            passphrase = "";
        } else {
            Editable newText = this.newPassphrase.getText();
            passphrase = (newText == null ? "" : newText.toString());
        }

        if (this.repeatPassphrase == null) {
            passphraseRepeat = "";
        } else {
            Editable repeatText = this.repeatPassphrase.getText();
            passphraseRepeat = (repeatText == null ? "" : repeatText.toString());
        }

        if (SilencePreferences.isPasswordDisabled(this)) {
            original = MasterSecretUtil.UNENCRYPTED_PASSPHRASE;
        }

        if (!passphrase.equals(passphraseRepeat)) {
            this.newPassphrase.setText("");
            this.repeatPassphrase.setText("");
            this.newPassphrase.setError(getString(R.string.PassphraseChangeActivity_passphrases_dont_match_exclamation));
            this.newPassphrase.requestFocus();
        } else if (passphrase.equals("")) {
            this.newPassphrase.setError(getString(R.string.PassphraseChangeActivity_enter_new_passphrase_exclamation));
            this.newPassphrase.requestFocus();
        } else {
            new ChangePassphraseTask(this).execute(original, passphrase);
        }
    }

    @Override
    protected void cleanup() {
        this.originalPassphrase = null;
        this.newPassphrase = null;
        this.repeatPassphrase = null;

        System.gc();
    }

    private class CancelButtonClickListener implements OnClickListener {
        public void onClick(View v) {
            finish();
        }
    }

    private class OkButtonClickListener implements OnClickListener {
        public void onClick(View v) {
            verifyAndSavePassphrases();
        }
    }

    private class ChangePassphraseTask extends AsyncTask<String, Void, MasterSecret> {
        private final Context context;

        public ChangePassphraseTask(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            okButton.setEnabled(false);
        }

        @Override
        protected MasterSecret doInBackground(String... params) {
            try {
                MasterSecret masterSecret = MasterSecretUtil.changeMasterSecretPassphrase(context, params[0], params[1]);
                SilencePreferences.setPasswordDisabled(context, false);

                return masterSecret;

            } catch (InvalidPassphraseException e) {
                Log.w(PassphraseChangeActivity.class.getSimpleName(), e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(MasterSecret masterSecret) {
            okButton.setEnabled(true);

            if (masterSecret != null) {
                setMasterSecret(masterSecret);
            } else {
                originalPassphrase.setText("");
                originalPassphrase.setError(getString(R.string.PassphraseChangeActivity_incorrect_old_passphrase_exclamation));
                originalPassphrase.requestFocus();
            }
        }
    }
}
