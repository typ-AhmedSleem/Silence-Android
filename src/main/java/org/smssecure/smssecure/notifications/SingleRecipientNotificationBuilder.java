package org.smssecure.smssecure.notifications;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Action;
import androidx.core.app.RemoteInput;
import android.text.SpannableStringBuilder;

import com.bumptech.glide.Glide;

import org.smssecure.smssecure.R;
import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.mms.DecryptableStreamUriLoader;
import org.smssecure.smssecure.mms.Slide;
import org.smssecure.smssecure.mms.SlideDeck;
import org.smssecure.smssecure.preferences.widgets.NotificationPrivacyPreference;
import org.smssecure.smssecure.recipients.Recipient;
import org.smssecure.smssecure.recipients.Recipients;
import org.smssecure.smssecure.util.BitmapUtil;
import org.smssecure.smssecure.util.Util;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class SingleRecipientNotificationBuilder extends AbstractNotificationBuilder {

    private static final String TAG = SingleRecipientNotificationBuilder.class.getSimpleName();

    private final List<CharSequence> messageBodies = new LinkedList<>();
    private final MasterSecret masterSecret;
    private SlideDeck slideDeck;
    private CharSequence contentTitle;
    private CharSequence contentText;

    public SingleRecipientNotificationBuilder(@NonNull Context context,
                                              @Nullable MasterSecret masterSecret,
                                              @NonNull NotificationPrivacyPreference privacy) {
        super(context, privacy);
        this.masterSecret = masterSecret;

        setSmallIcon(R.drawable.icon_notification);
        setColor(context.getResources().getColor(R.color.silence_primary));
        setPriority(NotificationCompat.PRIORITY_HIGH);
        setCategory(NotificationCompat.CATEGORY_MESSAGE);
    }

    public void setThread(@NonNull Recipients recipients) {
        if (privacy.isDisplayContact()) {
            setContentTitle(recipients.toShortString());

            if (recipients.isSingleRecipient() && recipients.getPrimaryRecipient().getContactUri() != null) {
                addPerson(recipients.getPrimaryRecipient().getContactUri().toString());
            }

            setLargeIcon(recipients.getContactPhoto()
                    .asDrawable(context, recipients.getColor()
                            .toConversationColor(context)));
        } else {
            setContentTitle(context.getString(R.string.SingleRecipientNotificationBuilder_silence));
            setLargeIcon(Recipient.getUnknownRecipient()
                    .getContactPhoto()
                    .asDrawable(context, Recipient.getUnknownRecipient()
                            .getColor()
                            .toConversationColor(context)));
        }
    }

    public void setMessageCount(int messageCount) {
        setContentInfo(String.valueOf(messageCount));
        setNumber(messageCount);
    }

    public void setPrimaryMessageBody(@NonNull Recipients threadRecipients,
                                      @NonNull Recipient individualRecipient,
                                      @NonNull CharSequence message,
                                      @Nullable SlideDeck slideDeck) {
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder();

        if (privacy.isDisplayContact() && (threadRecipients.isGroupRecipient() || !threadRecipients.isSingleRecipient())) {
            stringBuilder.append(Util.getBoldedString(individualRecipient.toShortString() + ": "));
        }

        if (privacy.isDisplayMessage()) {
            setContentText(stringBuilder.append(message));
            this.slideDeck = slideDeck;
        } else {
            setContentText(stringBuilder.append(context.getString(R.string.SingleRecipientNotificationBuilder_new_message)));
        }
    }

    public void addAndroidAutoAction(@NonNull PendingIntent androidAutoReplyIntent,
                                     @NonNull PendingIntent androidAutoHeardIntent, long timestamp) {

        if (contentTitle == null || contentText == null)
            return;

        RemoteInput remoteInput = new RemoteInput.Builder(AndroidAutoReplyReceiver.VOICE_REPLY_KEY)
                .setLabel(context.getString(R.string.MessageNotifier_reply))
                .build();

        NotificationCompat.CarExtender.UnreadConversation.Builder unreadConversationBuilder =
                new NotificationCompat.CarExtender.UnreadConversation.Builder(contentTitle.toString())
                        .addMessage(contentText.toString())
                        .setLatestTimestamp(timestamp)
                        .setReadPendingIntent(androidAutoHeardIntent)
                        .setReplyAction(androidAutoReplyIntent, remoteInput);

        extend(new NotificationCompat.CarExtender().setUnreadConversation(unreadConversationBuilder.build()));
    }

    public void addActions(@Nullable MasterSecret masterSecret,
                           @NonNull PendingIntent markReadIntent,
                           @NonNull PendingIntent quickReplyIntent,
                           @NonNull PendingIntent wearableReplyIntent) {
        Action markAsReadAction = new Action(R.drawable.check,
                context.getString(R.string.MessageNotifier_mark_read),
                markReadIntent);

        if (masterSecret != null) {
            Action replyAction = new Action(R.drawable.ic_reply_white_36dp,
                    context.getString(R.string.MessageNotifier_reply),
                    quickReplyIntent);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                replyAction = new Action.Builder(R.drawable.ic_reply_white_36dp,
                        context.getString(R.string.MessageNotifier_reply),
                        wearableReplyIntent)
                        .addRemoteInput(new RemoteInput.Builder(MessageNotifier.EXTRA_REMOTE_REPLY)
                                .setLabel(context.getString(R.string.MessageNotifier_reply)).build())
                        .build();
            }

            Action wearableReplyAction = new Action.Builder(R.drawable.ic_reply,
                    context.getString(R.string.MessageNotifier_reply),
                    wearableReplyIntent)
                    .addRemoteInput(new RemoteInput.Builder(MessageNotifier.EXTRA_REMOTE_REPLY)
                            .setLabel(context.getString(R.string.MessageNotifier_reply)).build())
                    .build();

            addAction(markAsReadAction);
            addAction(replyAction);

            extend(new NotificationCompat.WearableExtender().addAction(markAsReadAction)
                    .addAction(wearableReplyAction));
        } else {
            addAction(markAsReadAction);

            extend(new NotificationCompat.WearableExtender().addAction(markAsReadAction));
        }
    }

    public void addMessageBody(@NonNull Recipients threadRecipients,
                               @NonNull Recipient individualRecipient,
                               @Nullable CharSequence messageBody) {
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder();

        if (privacy.isDisplayContact() && (threadRecipients.isGroupRecipient() || !threadRecipients.isSingleRecipient())) {
            stringBuilder.append(Util.getBoldedString(individualRecipient.toShortString() + ": "));
        }

        if (privacy.isDisplayMessage()) {
            messageBodies.add(stringBuilder.append(messageBody == null ? "" : messageBody));
        } else {
            messageBodies.add(stringBuilder.append(context.getString(R.string.SingleRecipientNotificationBuilder_new_message)));
        }
    }

    @Override
    public Notification build() {
        if (privacy.isDisplayMessage()) {
            if (messageBodies.size() == 1 && hasBigPictureSlide(slideDeck)) {
                assert masterSecret != null;
                setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(getBigPicture(masterSecret, slideDeck))
                        .setSummaryText(getBigText(messageBodies)));
            } else {
                setStyle(new NotificationCompat.BigTextStyle().bigText(getBigText(messageBodies)));
            }
        }

        return super.build();
    }

    private void setLargeIcon(@Nullable Drawable drawable) {
        if (drawable != null) {
            int largeIconTargetSize = context.getResources().getDimensionPixelSize(R.dimen.contact_photo_target_size);
            Bitmap recipientPhotoBitmap = BitmapUtil.createFromDrawable(drawable, largeIconTargetSize, largeIconTargetSize);

            if (recipientPhotoBitmap != null) {
                setLargeIcon(recipientPhotoBitmap);
            }
        }
    }

    private boolean hasBigPictureSlide(@Nullable SlideDeck slideDeck) {
        if (masterSecret == null || slideDeck == null || Build.VERSION.SDK_INT < 16) {
            return false;
        }

        Slide thumbnailSlide = slideDeck.getThumbnailSlide();

        if (thumbnailSlide == null) return false;

        Uri uri = thumbnailSlide.getThumbnailUri();

        if (uri == null) return false;

        DecryptableStreamUriLoader.DecryptableUri decryptableUri = new DecryptableStreamUriLoader.DecryptableUri(masterSecret, uri);

        return decryptableUri != null &&
                thumbnailSlide.hasImage() &&
                !thumbnailSlide.isInProgress();
    }

    private Bitmap getBigPicture(@NonNull MasterSecret masterSecret,
                                 @NonNull SlideDeck slideDeck) {
        try {
            @SuppressWarnings("ConstantConditions")
            Uri uri = slideDeck.getThumbnailSlide().getThumbnailUri();

            return Glide.with(context)
                    .load(new DecryptableStreamUriLoader.DecryptableUri(masterSecret, uri))
                    .asBitmap()
                    .into(500, 500)
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public NotificationCompat.Builder setContentTitle(CharSequence contentTitle) {
        this.contentTitle = contentTitle;
        return super.setContentTitle(contentTitle);
    }

    public NotificationCompat.Builder setContentText(CharSequence contentText) {
        this.contentText = trimToDisplayLength(contentText);
        return super.setContentText(this.contentText);
    }

    private CharSequence getBigText(List<CharSequence> messageBodies) {
        SpannableStringBuilder content = new SpannableStringBuilder();

        for (CharSequence message : messageBodies) {
            content.append(message);
            content.append('\n');
        }

        return content;
    }

}
