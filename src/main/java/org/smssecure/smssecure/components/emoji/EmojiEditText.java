package org.smssecure.smssecure.components.emoji;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import android.text.InputFilter;
import android.util.AttributeSet;

import org.smssecure.smssecure.components.emoji.EmojiProvider.EmojiDrawable;
import org.smssecure.smssecure.util.SilencePreferences;


public class EmojiEditText extends AppCompatEditText {
    private static final String TAG = EmojiEditText.class.getSimpleName();

    public EmojiEditText(Context context) {
        this(context, null);
    }

    public EmojiEditText(Context context, AttributeSet attrs) {
        this(context, attrs, androidx.appcompat.R.attr.editTextStyle);
    }

    public EmojiEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (!SilencePreferences.isSystemEmojiPreferred(getContext())) {
            setFilters(new InputFilter[]{new EmojiFilter(this)});
        }
    }

    public void insertEmoji(String emoji) {
        final int start = getSelectionStart();
        final int end = getSelectionEnd();

        getText().replace(Math.min(start, end), Math.max(start, end), emoji);
        setSelection(start + emoji.length());
    }

    @Override
    public void invalidateDrawable(@NonNull Drawable drawable) {
        if (drawable instanceof EmojiDrawable) invalidate();
        else super.invalidateDrawable(drawable);
    }

    private InputFilter[] appendEmojiFilter(@Nullable InputFilter[] originalFilters) {
        InputFilter[] result;

        if (originalFilters != null) {
            result = new InputFilter[originalFilters.length + 1];
            System.arraycopy(originalFilters, 0, result, 1, originalFilters.length);
        } else {
            result = new InputFilter[1];
        }

        result[0] = new EmojiFilter(this);

        return result;
    }
}
