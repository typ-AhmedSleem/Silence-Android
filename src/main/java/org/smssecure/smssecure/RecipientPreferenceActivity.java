package org.smssecure.smssecure;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import org.smssecure.smssecure.color.MaterialColor;
import org.smssecure.smssecure.color.MaterialColors;
import org.smssecure.smssecure.components.AvatarImageView;
import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.database.DatabaseFactory;
import org.smssecure.smssecure.database.RecipientPreferenceDatabase.VibrateState;
import org.smssecure.smssecure.preferences.CorrectedPreferenceFragment;
import org.smssecure.smssecure.preferences.widgets.AdvancedRingtonePreference;
import org.smssecure.smssecure.preferences.widgets.ColorPickerPreference;
import org.smssecure.smssecure.recipients.RecipientFactory;
import org.smssecure.smssecure.recipients.Recipients;
import org.smssecure.smssecure.util.DynamicLanguage;
import org.smssecure.smssecure.util.DynamicNoActionBarTheme;
import org.smssecure.smssecure.util.DynamicTheme;

@SuppressLint("StaticFieldLeak")
public class RecipientPreferenceActivity extends PassphraseRequiredActionBarActivity implements Recipients.RecipientsModifiedListener {
    public static final String RECIPIENTS_EXTRA = "recipient_ids";
    private static final String TAG = RecipientPreferenceActivity.class.getSimpleName();
    private static final String PREFERENCE_MUTED = "pref_key_recipient_mute";
    private static final String PREFERENCE_TONE = "pref_key_recipient_ringtone";
    private static final String PREFERENCE_VIBRATE = "pref_key_recipient_vibrate";
    private static final String PREFERENCE_BLOCK = "pref_key_recipient_block";
    private static final String PREFERENCE_COLOR = "pref_key_recipient_color";

    private final DynamicTheme dynamicTheme = new DynamicNoActionBarTheme();
    private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

    private AvatarImageView avatar;
    private Toolbar toolbar;
    private TextView title;
    private TextView blockedIndicator;

    @Override
    public void onPreCreate() {
        dynamicTheme.onCreate(this);
        dynamicLanguage.onCreate(this);
    }

    @Override
    public void onCreate(Bundle instanceState, @NonNull MasterSecret masterSecret) {
        setContentView(R.layout.recipient_preference_activity);

        long[] recipientIds = getIntent().getLongArrayExtra(RECIPIENTS_EXTRA);
        Recipients recipients = RecipientFactory.getRecipientsForIds(this, recipientIds, true);

        initializeToolbar();
        setHeader(recipients);
        recipients.addListener(this);

        Bundle bundle = new Bundle();
        bundle.putLongArray(RECIPIENTS_EXTRA, recipientIds);
        initFragment(R.id.preference_fragment, new RecipientPreferenceFragment(), masterSecret, null, bundle);
    }

    @Override
    public void onResume() {
        super.onResume();
        dynamicTheme.onResume(this);
        dynamicLanguage.onResume(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.preference_fragment);
        fragment.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        if (item.getItemId() == android.R.id.home) {
            super.onBackPressed();
            return true;
        }

        return false;
    }

    private void initializeToolbar() {
        this.toolbar = findViewById(R.id.toolbar);
        this.toolbar.setLogo(null);

        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        this.avatar = toolbar.findViewById(R.id.avatar);
        this.title = toolbar.findViewById(R.id.name);
        this.blockedIndicator = toolbar.findViewById(R.id.blocked_indicator);
    }

    private void setHeader(Recipients recipients) {
        this.avatar.setAvatar(recipients, true);
        this.title.setText(recipients.toShortString());
        this.toolbar.setBackgroundColor(recipients.getColor().toActionBarColor(this));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(recipients.getColor().toStatusBarColor(this));
            window.setNavigationBarColor(getResources().getColor(android.R.color.black));
        }

        if (recipients.isBlocked()) this.blockedIndicator.setVisibility(View.VISIBLE);
        else this.blockedIndicator.setVisibility(View.GONE);
    }

    @Override
    public void onModified(final Recipients recipients) {
        title.post(new Runnable() {
            @Override
            public void run() {
                setHeader(recipients);
            }
        });
    }

    public static class RecipientPreferenceFragment
            extends CorrectedPreferenceFragment
            implements Recipients.RecipientsModifiedListener {

        private final Handler handler = new Handler();

        private Recipients recipients;

        @Override
        public void onCreate(Bundle icicle) {
            super.onCreate(icicle);

            this.recipients = RecipientFactory.getRecipientsForIds(getActivity(),
                    getArguments().getLongArray(RECIPIENTS_EXTRA),
                    true);

            this.recipients.addListener(this);
            this.findPreference(PREFERENCE_TONE)
                    .setOnPreferenceChangeListener(new RingtoneChangeListener());
            this.findPreference(PREFERENCE_VIBRATE)
                    .setOnPreferenceChangeListener(new VibrateChangeListener());
            this.findPreference(PREFERENCE_MUTED)
                    .setOnPreferenceClickListener(new MuteClickedListener());
            this.findPreference(PREFERENCE_BLOCK)
                    .setOnPreferenceClickListener(new BlockClickedListener());
            this.findPreference(PREFERENCE_COLOR)
                    .setOnPreferenceChangeListener(new ColorChangeListener());
        }

        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.recipient_preferences);
        }

        @Override
        public void onResume() {
            super.onResume();
            setSummaries(recipients);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            this.recipients.removeListener(this);
        }

        private void setSummaries(Recipients recipients) {
            CheckBoxPreference mutePreference = (CheckBoxPreference) this.findPreference(PREFERENCE_MUTED);
            AdvancedRingtonePreference ringtonePreference = (AdvancedRingtonePreference) this.findPreference(PREFERENCE_TONE);
            ListPreference vibratePreference = (ListPreference) this.findPreference(PREFERENCE_VIBRATE);
            ColorPickerPreference colorPreference = (ColorPickerPreference) this.findPreference(PREFERENCE_COLOR);
            Preference blockPreference = this.findPreference(PREFERENCE_BLOCK);

            mutePreference.setChecked(recipients.isMuted());

            final Uri toneUri = recipients.getRingtone();

            if (toneUri == null) {
                ringtonePreference.setSummary(R.string.preferences__default);
                ringtonePreference.setCurrentRingtone(Settings.System.DEFAULT_NOTIFICATION_URI);
            } else if (toneUri.toString().isEmpty()) {
                ringtonePreference.setSummary(R.string.preferences__silent);
                ringtonePreference.setCurrentRingtone(null);
            } else {
                Ringtone tone = RingtoneManager.getRingtone(getActivity(), toneUri);

                if (tone != null) {
                    ringtonePreference.setSummary(tone.getTitle(getActivity()));
                    ringtonePreference.setCurrentRingtone(toneUri);
                }
            }

            if (recipients.getVibrate() == VibrateState.DEFAULT) {
                vibratePreference.setSummary(R.string.preferences__default);
                vibratePreference.setValueIndex(0);
            } else if (recipients.getVibrate() == VibrateState.ENABLED) {
                vibratePreference.setSummary(R.string.RecipientPreferenceActivity_enabled);
                vibratePreference.setValueIndex(1);
            } else {
                vibratePreference.setSummary(R.string.RecipientPreferenceActivity_disabled);
                vibratePreference.setValueIndex(2);
            }

            if (!recipients.isSingleRecipient() || recipients.isGroupRecipient()) {
                if (colorPreference != null)
                    getPreferenceScreen().removePreference(colorPreference);
                if (blockPreference != null)
                    getPreferenceScreen().removePreference(blockPreference);
            } else {
                colorPreference.setColors(MaterialColors.CONVERSATION_PALETTE.asConversationColorArray(getActivity()));
                colorPreference.setColor(recipients.getColor().toActionBarColor(getActivity()));

                if (recipients.isBlocked())
                    blockPreference.setTitle(R.string.RecipientPreferenceActivity_unblock);
                else blockPreference.setTitle(R.string.RecipientPreferenceActivity_block);
            }
        }

        @Override
        public void onModified(final Recipients recipients) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    setSummaries(recipients);
                }
            });
        }

        private class RingtoneChangeListener implements Preference.OnPreferenceChangeListener {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Uri value = (Uri) newValue;

                final Uri uri;

                if (Settings.System.DEFAULT_NOTIFICATION_URI.equals(value)) {
                    uri = null;
                } else {
                    uri = value;
                }

                recipients.setRingtone(uri);

                new AsyncTask<Uri, Void, Void>() {
                    @Override
                    protected Void doInBackground(Uri... params) {
                        DatabaseFactory.getRecipientPreferenceDatabase(getActivity())
                                .setRingtone(recipients, params[0]);
                        return null;
                    }
                }.execute(uri);

                return false;
            }
        }

        private class VibrateChangeListener implements Preference.OnPreferenceChangeListener {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int value = Integer.parseInt((String) newValue);
                final VibrateState vibrateState = VibrateState.fromId(value);

                recipients.setVibrate(vibrateState);

                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        DatabaseFactory.getRecipientPreferenceDatabase(getActivity())
                                .setVibrate(recipients, vibrateState);
                        return null;
                    }
                }.execute();

                return false;
            }
        }

        private class ColorChangeListener implements Preference.OnPreferenceChangeListener {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final int value = (Integer) newValue;
                final MaterialColor selectedColor = MaterialColors.CONVERSATION_PALETTE.getByColor(getActivity(), value);
                final MaterialColor currentColor = recipients.getColor();

                if (selectedColor == null) return true;

                if (preference.isEnabled() && !currentColor.equals(selectedColor)) {
                    recipients.setColor(selectedColor);

                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            DatabaseFactory.getRecipientPreferenceDatabase(getActivity())
                                    .setColor(recipients, selectedColor);
                            return null;
                        }
                    }.execute();
                }
                return true;
            }
        }

        private class MuteClickedListener implements Preference.OnPreferenceClickListener {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (recipients.isMuted()) handleUnmute();
                else handleMute();

                return true;
            }

            private void handleMute() {
                MuteDialog.show(getActivity(), new MuteDialog.MuteSelectionListener() {
                    @Override
                    public void onMuted(long until) {
                        setMuted(recipients, until);
                    }
                });

                setSummaries(recipients);
            }

            private void handleUnmute() {
                setMuted(recipients, 0);
            }

            private void setMuted(final Recipients recipients, final long until) {
                recipients.setMuted(until);

                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        DatabaseFactory.getRecipientPreferenceDatabase(getActivity())
                                .setMuted(recipients, until);
                        return null;
                    }
                }.execute();
            }
        }

        private class BlockClickedListener implements Preference.OnPreferenceClickListener {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (recipients.isBlocked()) handleUnblock();
                else handleBlock();

                return true;
            }

            private void handleBlock() {
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.RecipientPreferenceActivity_block_this_contact_question)
                        .setMessage(R.string.RecipientPreferenceActivity_you_will_no_longer_see_messages_from_this_user)
                        .setCancelable(true)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.RecipientPreferenceActivity_block, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                setBlocked(recipients, true);
                            }
                        }).show();
            }

            private void handleUnblock() {
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.RecipientPreferenceActivity_unblock_this_contact_question)
                        .setMessage(R.string.RecipientPreferenceActivity_are_you_sure_you_want_to_unblock_this_contact)
                        .setCancelable(true)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.RecipientPreferenceActivity_unblock, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                setBlocked(recipients, false);
                            }
                        }).show();
            }

            private void setBlocked(final Recipients recipients, final boolean blocked) {
                recipients.setBlocked(blocked);

                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        DatabaseFactory.getRecipientPreferenceDatabase(getActivity())
                                .setBlocked(recipients, blocked);
                        return null;
                    }
                }.execute();
            }
        }
    }
}
