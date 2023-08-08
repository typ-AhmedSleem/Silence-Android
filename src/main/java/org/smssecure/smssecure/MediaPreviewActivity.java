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

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import org.smssecure.smssecure.components.ZoomingImageView;
import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.mms.VideoSlide;
import org.smssecure.smssecure.permissions.Permissions;
import org.smssecure.smssecure.recipients.Recipient;
import org.smssecure.smssecure.recipients.Recipient.RecipientModifiedListener;
import org.smssecure.smssecure.recipients.RecipientFactory;
import org.smssecure.smssecure.util.DateUtils;
import org.smssecure.smssecure.util.DynamicLanguage;
import org.smssecure.smssecure.util.SaveAttachmentTask;
import org.smssecure.smssecure.util.SaveAttachmentTask.Attachment;
import org.smssecure.smssecure.video.VideoPlayer;

import java.io.IOException;

/**
 * Activity for displaying media attachments in-app
 */
public class MediaPreviewActivity extends PassphraseRequiredActionBarActivity implements RecipientModifiedListener {
    public static final String RECIPIENT_EXTRA = "recipient";
    public static final String THREAD_ID_EXTRA = "thread_id";
    public static final String DATE_EXTRA = "date";
    public static final String SIZE_EXTRA = "size";
    private final static String TAG = MediaPreviewActivity.class.getSimpleName();
    private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

    private MasterSecret masterSecret;

    private ZoomingImageView image;
    private VideoPlayer video;

    private Uri mediaUri;
    private String mediaType;
    private Recipient recipient;
    private long threadId;
    private long date;
    private long size;

    public static boolean isContentTypeSupported(final String contentType) {
        return contentType != null && (contentType.startsWith("image/") || contentType.startsWith("video/"));
    }

    @Override
    protected void onCreate(Bundle bundle, @NonNull MasterSecret masterSecret) {
        this.masterSecret = masterSecret;
        this.setTheme(R.style.Silence_DarkTheme);
        dynamicLanguage.onCreate(this);

        setFullscreenIfPossible();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.media_preview_activity);

        initializeViews();
        initializeResources();
        initializeActionBar();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @TargetApi(VERSION_CODES.JELLY_BEAN)
    private void setFullscreenIfPossible() {
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    @Override
    public void onModified(Recipient recipient) {
        initializeActionBar();
    }

    private void initializeActionBar() {
        final CharSequence relativeTimeSpan;
        if (date > 0) {
            relativeTimeSpan = DateUtils.getExtendedRelativeTimeSpanString(this, dynamicLanguage.getCurrentLocale(), date);
        } else {
            relativeTimeSpan = getString(R.string.MediaPreviewActivity_draft);
        }
        getSupportActionBar().setTitle(recipient == null ? getString(R.string.MediaPreviewActivity_you)
                : recipient.toShortString());
        getSupportActionBar().setSubtitle(relativeTimeSpan);
    }

    @Override
    public void onResume() {
        super.onResume();
        dynamicLanguage.onResume(this);
        if (recipient != null) recipient.addListener(this);
        initializeMedia();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (recipient != null) recipient.removeListener(this);
        cleanupMedia();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (recipient != null) recipient.removeListener(this);
        setIntent(intent);
        initializeResources();
        initializeActionBar();
    }

    private void initializeViews() {
        image = findViewById(R.id.image);
        video = findViewById(R.id.video_player);
    }

    private void initializeResources() {
        final long recipientId = getIntent().getLongExtra(RECIPIENT_EXTRA, -1);

        mediaUri = getIntent().getData();
        mediaType = getIntent().getType();
        date = getIntent().getLongExtra(DATE_EXTRA, -1);
        size = getIntent().getLongExtra(SIZE_EXTRA, 0);
        threadId = getIntent().getLongExtra(THREAD_ID_EXTRA, -1);

        if (recipientId > -1) {
            recipient = RecipientFactory.getRecipientForId(this, recipientId, true);
            recipient.addListener(this);
        } else {
            recipient = null;
        }
    }

    private void initializeMedia() {
        if (!isContentTypeSupported(mediaType)) {
            Log.w(TAG, "Unsupported media type sent to MediaPreviewActivity, finishing.");
            Toast.makeText(getApplicationContext(), R.string.MediaPreviewActivity_unssuported_media_type, Toast.LENGTH_LONG).show();
            finish();
        }

        Log.w(TAG, "Loading Part URI: " + mediaUri);

        try {
            if (mediaType != null && mediaType.startsWith("image/")) {
                image.setVisibility(View.VISIBLE);
                video.setVisibility(View.GONE);
                image.setImageUri(masterSecret, mediaUri, mediaType);
            } else if (mediaType != null && mediaType.startsWith("video/")) {
                image.setVisibility(View.GONE);
                video.setVisibility(View.VISIBLE);
                video.setVideoSource(masterSecret, new VideoSlide(this, mediaUri, size));
            }
        } catch (IOException e) {
            Log.w(TAG, e);
            Toast.makeText(getApplicationContext(), R.string.MediaPreviewActivity_unssuported_media_type, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void cleanupMedia() {
        image.cleanup();
        video.cleanup();
    }

    private void showOverview() {
        Intent intent = new Intent(this, MediaOverviewActivity.class);
        intent.putExtra(MediaOverviewActivity.THREAD_ID_EXTRA, threadId);
        startActivity(intent);
    }

    private void forward() {
        Intent composeIntent = new Intent(this, ShareActivity.class);
        composeIntent.putExtra(Intent.EXTRA_STREAM, mediaUri);
        composeIntent.setType(mediaType);
        startActivity(composeIntent);
    }

    private void saveToDisk() {
        SaveAttachmentTask.showWarningDialog(this, (dialogInterface, i) -> {
            Permissions.with(this)
                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                    .ifNecessary()
                    .withPermanentDenialDialog(getString(R.string.MediaPreviewActivity_silence_needs_the_storage_permission_in_order_to_write_to_external_storage_but_it_has_been_permanently_denied))
                    .onAnyDenied(() -> Toast.makeText(this, R.string.MediaPreviewActivity_unable_to_write_to_external_storage_without_permission, Toast.LENGTH_LONG).show())
                    .onAllGranted(() -> {
                        SaveAttachmentTask saveTask = new SaveAttachmentTask(MediaPreviewActivity.this, masterSecret);
                        long saveDate = (date > 0) ? date : System.currentTimeMillis();
                        saveTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Attachment(mediaUri, mediaType, saveDate));
                    })
                    .execute();
        });
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        menu.clear();
        MenuInflater inflater = this.getMenuInflater();
        inflater.inflate(R.menu.media_preview, menu);
        if (threadId == -1) menu.findItem(R.id.media_preview__overview).setVisible(false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        int itemId = item.getItemId();
        if (itemId == R.id.media_preview__overview) {
            showOverview();
            return true;
        } else if (itemId == R.id.media_preview__forward) {
            forward();
            return true;
        } else if (itemId == R.id.save) {
            saveToDisk();
            return true;
        } else if (itemId == android.R.id.home) {
            finish();
            return true;
        }

        return false;
    }
}
