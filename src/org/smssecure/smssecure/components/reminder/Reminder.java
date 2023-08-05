package org.smssecure.smssecure.components.reminder;

import androidx.annotation.NonNull;
import android.view.View.OnClickListener;

public abstract class Reminder {
    private final CharSequence buttonText;
    private final CharSequence title;
    private final CharSequence text;

    private OnClickListener okListener;
    private OnClickListener dismissListener;

    public Reminder(@NonNull CharSequence title,
                    @NonNull CharSequence text,
                    @NonNull CharSequence buttonText) {
        this.title = title;
        this.text = text;
        this.buttonText = buttonText;
    }

    public CharSequence getTitle() {
        return title;
    }

    public CharSequence getText() {
        return text;
    }

    public CharSequence getButtonText() {
        return buttonText;
    }

    public OnClickListener getOkListener() {
        return okListener;
    }

    public void setOkListener(OnClickListener okListener) {
        this.okListener = okListener;
    }

    public OnClickListener getDismissListener() {
        return dismissListener;
    }

    public void setDismissListener(OnClickListener dismissListener) {
        this.dismissListener = dismissListener;
    }

    public boolean isDismissable() {
        return true;
    }
}
