package org.smssecure.smssecure.notifications;

import android.app.Notification;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;

import org.smssecure.smssecure.R;
import org.smssecure.smssecure.database.RecipientPreferenceDatabase;
import org.smssecure.smssecure.preferences.widgets.NotificationPrivacyPreference;
import org.smssecure.smssecure.recipients.Recipient;
import org.smssecure.smssecure.util.SilencePreferences;
import org.smssecure.smssecure.util.Util;

public abstract class AbstractNotificationBuilder extends NotificationCompat.Builder {

    private static final int MAX_DISPLAY_LENGTH = 500;

    protected Context context;
    protected NotificationPrivacyPreference privacy;

    public AbstractNotificationBuilder(Context context, NotificationPrivacyPreference privacy) {
        super(context);

        this.context = context;
        this.privacy = privacy;

        setChannelId(NotificationChannels.MESSAGES);
    }

    protected CharSequence getStyledMessage(@NonNull Recipient recipient, @Nullable CharSequence message) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(Util.getBoldedString(recipient.toShortString()));
        builder.append(": ");
        builder.append(message == null ? "" : message);

        return builder;
    }

    public void setAudibleAlarms(@Nullable Uri ringtone, @Nullable RecipientPreferenceDatabase.VibrateState vibrate) {
        String defaultRingtoneName = SilencePreferences.getNotificationRingtone(context);
        boolean defaultVibrate = SilencePreferences.isNotificationVibrateEnabled(context);

        if (ringtone == null && !TextUtils.isEmpty(defaultRingtoneName))
            setSound(Uri.parse(defaultRingtoneName));
        else if (ringtone != null && !ringtone.toString().isEmpty()) setSound(ringtone);

        if (vibrate != null &&
                (vibrate == RecipientPreferenceDatabase.VibrateState.ENABLED ||
                        (vibrate == RecipientPreferenceDatabase.VibrateState.DEFAULT && defaultVibrate))) {
            setDefaults(Notification.DEFAULT_VIBRATE);
        }

    }

    public void setVisualAlarms() {
        String ledColor = SilencePreferences.getNotificationLedColor(context);
        String ledBlinkPattern = SilencePreferences.getNotificationLedPattern(context);
        String ledBlinkPatternCustom = SilencePreferences.getNotificationLedPatternCustom(context);
        String[] blinkPatternArray = parseBlinkPattern(ledBlinkPattern, ledBlinkPatternCustom);

        if (!ledColor.equals("none")) {
            setLights(Color.parseColor(ledColor),
                    Integer.parseInt(blinkPatternArray[0]),
                    Integer.parseInt(blinkPatternArray[1]));
        }
    }

    public void setTicker(@NonNull Recipient recipient, @Nullable CharSequence message) {
        if (privacy.isDisplayMessage()) {
            setTicker(getStyledMessage(recipient, message));
        } else if (privacy.isDisplayContact()) {
            setTicker(getStyledMessage(recipient, context.getString(R.string.AbstractNotificationBuilder_new_message)));
        } else {
            setTicker(context.getString(R.string.AbstractNotificationBuilder_new_message));
        }
    }

    private String[] parseBlinkPattern(String blinkPattern, String blinkPatternCustom) {
        if (blinkPattern.equals("custom"))
            blinkPattern = blinkPatternCustom;

        return blinkPattern.split(",");
    }

    protected @NonNull CharSequence trimToDisplayLength(@Nullable CharSequence text) {
        text = text == null ? "" : text;

        return text.length() <= MAX_DISPLAY_LENGTH ? text
                : text.subSequence(0, MAX_DISPLAY_LENGTH);
    }
}
