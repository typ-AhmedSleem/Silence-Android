package org.smssecure.smssecure.components.emoji;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;

import org.smssecure.smssecure.R;
import org.smssecure.smssecure.util.ResUtil;

public class EmojiView extends View implements Drawable.Callback {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private String emoji;
    private Drawable drawable;

    public EmojiView(Context context) {
        this(context, null);
    }

    public EmojiView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EmojiView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
        this.drawable = EmojiProvider.getInstance(getContext())
                .getEmojiDrawable(emoji);
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (drawable != null) {
            drawable.setBounds(getPaddingLeft(),
                    getPaddingTop(),
                    getWidth() - getPaddingRight(),
                    getHeight() - getPaddingBottom());
            drawable.setCallback(this);
            drawable.draw(canvas);
        } else {
            float targetFontSize = 0.75f * getHeight() - getPaddingTop() - getPaddingBottom();
            paint.setTextSize(targetFontSize);
            paint.setColor(ResUtil.getColor(getContext(), R.attr.emoji_text_color));
            paint.setTextAlign(Paint.Align.CENTER);
            int xPos = (canvas.getWidth() / 2);
            int yPos = (int) ((canvas.getHeight() / 2) - ((paint.descent() + paint.ascent()) / 2));

            float overflow = paint.measureText(emoji) /
                    (getWidth() - getPaddingLeft() - getPaddingRight());
            if (overflow > 1f) {
                paint.setTextSize(targetFontSize / overflow);
                yPos = (int) ((canvas.getHeight() / 2) - ((paint.descent() + paint.ascent()) / 2));
            }
            canvas.drawText(emoji, xPos, yPos, paint);
        }
    }

    @Override
    public void invalidateDrawable(@NonNull Drawable drawable) {
        super.invalidateDrawable(drawable);
        postInvalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
