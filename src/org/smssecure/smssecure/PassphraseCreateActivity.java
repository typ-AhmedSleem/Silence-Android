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

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;

import org.smssecure.smssecure.crypto.IdentityKeyUtil;
import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.crypto.MasterSecretUtil;
import org.smssecure.smssecure.util.SilencePreferences;
import org.smssecure.smssecure.util.VersionTracker;
import org.smssecure.smssecure.util.dualsim.DualSimUtil;
import org.smssecure.smssecure.util.dualsim.SubscriptionInfoCompat;
import org.smssecure.smssecure.util.dualsim.SubscriptionManagerCompat;

import java.util.List;

/**
 * Activity for creating a user's local encryption passphrase.
 *
 * @author Moxie Marlinspike
 */

public class PassphraseCreateActivity extends PassphraseActivity {

    public PassphraseCreateActivity() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.create_passphrase_activity);

        initializeResources();
    }

    private void initializeResources() {
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.centered_app_title);

        new SecretGenerator().execute(MasterSecretUtil.UNENCRYPTED_PASSPHRASE);
    }

    @Override
    protected void cleanup() {
        System.gc();
    }

    private class SecretGenerator extends AsyncTask<String, Void, Void> {
        private MasterSecret masterSecret;

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Void doInBackground(String... params) {
            String passphrase = params[0];
            masterSecret = MasterSecretUtil.generateMasterSecret(PassphraseCreateActivity.this,
                    passphrase);

            MasterSecretUtil.generateAsymmetricMasterSecret(PassphraseCreateActivity.this, masterSecret);

            SubscriptionManagerCompat subscriptionManagerCompat = SubscriptionManagerCompat.from(PassphraseCreateActivity.this);

            if (Build.VERSION.SDK_INT >= 22) {
                List<SubscriptionInfoCompat> activeSubscriptions = subscriptionManagerCompat.updateActiveSubscriptionInfoList();
                DualSimUtil.generateKeysIfDoNotExist(PassphraseCreateActivity.this, masterSecret, activeSubscriptions, false);
            } else {
                IdentityKeyUtil.generateIdentityKeys(PassphraseCreateActivity.this, masterSecret, -1, false);
                subscriptionManagerCompat.updateActiveSubscriptionInfoList();
            }
            VersionTracker.updateLastSeenVersion(PassphraseCreateActivity.this);
            SilencePreferences.setPasswordDisabled(PassphraseCreateActivity.this, true);

            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            setMasterSecret(masterSecret);
        }
    }
}
