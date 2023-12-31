/**
 * Copyright (C) 2014 Open Whisper Systems
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
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.smssecure.smssecure.components.AvatarImageView;
import org.smssecure.smssecure.components.FromTextView;
import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.database.documents.IdentityKeyMismatch;
import org.smssecure.smssecure.database.documents.NetworkFailure;
import org.smssecure.smssecure.database.model.MessageRecord;
import org.smssecure.smssecure.recipients.Recipient;
import org.smssecure.smssecure.sms.MessageSender;

/**
 * A simple view to show the recipients of a message
 *
 * @author Jake McGinty
 */
public class MessageRecipientListItem extends RelativeLayout
        implements Recipient.RecipientModifiedListener {
    private final static String TAG = MessageRecipientListItem.class.getSimpleName();
    private final Handler handler = new Handler();
    private Recipient recipient;
    private FromTextView fromView;
    private TextView errorDescription;
    private Button conflictButton;
    private Button resendButton;
    private AvatarImageView contactPhotoImage;

    public MessageRecipientListItem(Context context) {
        super(context);
    }

    public MessageRecipientListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.fromView = findViewById(R.id.from);
        this.errorDescription = findViewById(R.id.error_description);
        this.contactPhotoImage = findViewById(R.id.contact_photo_image);
        this.conflictButton = findViewById(R.id.conflict_button);
        this.resendButton = findViewById(R.id.resend_button);
    }

    public void set(final MasterSecret masterSecret,
                    final MessageRecord record,
                    final Recipient recipient) {
        this.recipient = recipient;

        recipient.addListener(this);
        fromView.setText(recipient);
        contactPhotoImage.setAvatar(recipient, false);
        setIssueIndicators(masterSecret, record);
    }

    private void setIssueIndicators(final MasterSecret masterSecret,
                                    final MessageRecord record) {
        final NetworkFailure networkFailure = getNetworkFailure(record);
        final IdentityKeyMismatch keyMismatch = networkFailure == null ? getKeyMismatch(record) : null;

        String errorText = "";

        if (keyMismatch != null) {
            resendButton.setVisibility(View.GONE);
            conflictButton.setVisibility(View.VISIBLE);

            errorText = getContext().getString(R.string.MessageDetailsRecipient_new_identity);
            conflictButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    new ConfirmIdentityDialog(getContext(), masterSecret, record, null).show();
                }
            });
        } else if (networkFailure != null || record.isFailed()) {
            resendButton.setVisibility(View.VISIBLE);
            resendButton.setEnabled(true);
            resendButton.requestFocus();
            conflictButton.setVisibility(View.GONE);

            errorText = getContext().getString(R.string.MessageDetailsRecipient_failed_to_send);
            resendButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    new ResendAsyncTask(masterSecret, record, networkFailure).execute();
                }
            });
        } else {
            resendButton.setVisibility(View.GONE);
            conflictButton.setVisibility(View.GONE);
        }

        errorDescription.setText(errorText);
        errorDescription.setVisibility(TextUtils.isEmpty(errorText) ? View.GONE : View.VISIBLE);
    }

    private NetworkFailure getNetworkFailure(final MessageRecord record) {
        if (record.hasNetworkFailures()) {
            for (final NetworkFailure failure : record.getNetworkFailures()) {
                if (failure.getRecipientId() == recipient.getRecipientId()) {
                    return failure;
                }
            }
        }
        return null;
    }

    private IdentityKeyMismatch getKeyMismatch(final MessageRecord record) {
        if (record.isIdentityMismatchFailure()) {
            for (final IdentityKeyMismatch mismatch : record.getIdentityKeyMismatches()) {
                if (mismatch.getRecipientId() == recipient.getRecipientId()) {
                    return mismatch;
                }
            }
        }
        return null;
    }

    public void unbind() {
        if (this.recipient != null) this.recipient.removeListener(this);
    }

    @Override
    public void onModified(final Recipient recipient) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                fromView.setText(recipient);
                contactPhotoImage.setAvatar(recipient, false);
            }
        });
    }

    private class ResendAsyncTask extends AsyncTask<Void, Void, Void> {
        private final MasterSecret masterSecret;
        private final MessageRecord record;
        private final NetworkFailure failure;

        public ResendAsyncTask(MasterSecret masterSecret, MessageRecord record, NetworkFailure failure) {
            this.masterSecret = masterSecret;
            this.record = record;
            this.failure = failure;
        }

        @Override
        protected Void doInBackground(Void... params) {
            MessageSender.resend(getContext(), masterSecret, record);
            return null;
        }
    }

}
