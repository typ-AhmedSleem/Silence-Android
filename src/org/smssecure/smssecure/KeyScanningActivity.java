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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.smssecure.smssecure.util.Base64;
import org.smssecure.smssecure.util.Dialogs;
import org.smssecure.smssecure.util.DynamicLanguage;
import org.smssecure.smssecure.util.DynamicTheme;
import org.smssecure.smssecure.util.Hex;
import org.whispersystems.libsignal.IdentityKey;

/**
 * Activity for initiating/receiving key QR code scans.
 *
 * @author Moxie Marlinspike
 */
public abstract class KeyScanningActivity extends PassphraseRequiredActionBarActivity {

    private final DynamicTheme dynamicTheme = new DynamicTheme();
    private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

    @Override
    protected void onPreCreate() {
        dynamicTheme.onCreate(this);
        dynamicLanguage.onCreate(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        dynamicTheme.onResume(this);
        dynamicLanguage.onResume(this);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuInflater inflater = this.getMenuInflater();
        menu.clear();

        inflater.inflate(R.menu.key_scanning, menu);

        menu.findItem(R.id.menu_scan).setTitle(getScanString());
        menu.findItem(R.id.menu_get_scanned).setTitle(getDisplayString());

        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case R.id.menu_scan:
                initiateScan();
                return true;
            case R.id.menu_get_scanned:
                initiateDisplay();
                return true;
            case R.id.menu_share_fingerprint:
                initiateShare();
                return true;
            case android.R.id.home:
                finish();
                return true;
        }

        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);

        if ((scanResult != null) && (scanResult.getContents() != null)) {
            String data = scanResult.getContents();

            if (data.equals(Base64.encodeBytes(getIdentityKeyToCompare().serialize()))) {
                Dialogs.showInfoDialog(this, getVerifiedTitle(), getVerifiedMessage());
            } else {
                Dialogs.showAlertDialog(this, getNotVerifiedTitle(), getNotVerifiedMessage());
            }
        } else {
            Toast.makeText(this, R.string.KeyScanningActivity_no_scanned_key_found_exclamation,
                    Toast.LENGTH_LONG).show();
        }
    }

    private IntentIntegrator getIntentIntegrator() {
        IntentIntegrator intentIntegrator = new IntentIntegrator(this);
        intentIntegrator.setButtonYesByID(R.string.yes);
        intentIntegrator.setButtonNoByID(R.string.no);
        intentIntegrator.setTitleByID(R.string.KeyScanningActivity_install_barcode_Scanner);
        intentIntegrator.setMessageByID(R.string.KeyScanningActivity_this_application_requires_barcode_scanner_would_you_like_to_install_it);
        return intentIntegrator;
    }

    protected void initiateScan() {
        IntentIntegrator intentIntegrator = getIntentIntegrator();
        intentIntegrator.initiateScan();
    }

    protected void initiateDisplay() {
        IdentityKey identityKey = getIdentityKeyToDisplay();
        if (identityKey != null) {
            IntentIntegrator intentIntegrator = getIntentIntegrator();
            intentIntegrator.shareText(Base64.encodeBytes(identityKey.serialize()));
        } else {
            Toast.makeText(this, R.string.VerifyIdentityActivity_you_do_not_have_an_identity_key,
                    Toast.LENGTH_LONG).show();
        }
    }

    protected void initiateShare() {
        final Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, Hex.toString(getIdentityKeyToDisplay().serialize()));
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_identity_fingerprint)));
    }

    protected abstract String getScanString();

    protected abstract String getDisplayString();

    protected abstract String getNotVerifiedTitle();

    protected abstract String getNotVerifiedMessage();

    protected abstract IdentityKey getIdentityKeyToCompare();

    protected abstract IdentityKey getIdentityKeyToDisplay();

    protected abstract String getVerifiedTitle();

    protected abstract String getVerifiedMessage();

}
