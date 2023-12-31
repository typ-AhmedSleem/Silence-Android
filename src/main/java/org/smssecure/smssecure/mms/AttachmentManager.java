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
package org.smssecure.smssecure.mms;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.smssecure.smssecure.MediaPreviewActivity;
import org.smssecure.smssecure.R;
import org.smssecure.smssecure.components.AudioView;
import org.smssecure.smssecure.components.RemovableMediaView;
import org.smssecure.smssecure.components.ThumbnailView;
import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.permissions.Permissions;
import org.smssecure.smssecure.providers.PersistentBlobProvider;
import org.smssecure.smssecure.util.MediaUtil;
import org.smssecure.smssecure.util.ViewUtil;
import org.smssecure.smssecure.util.concurrent.ListenableFuture.Listener;
import org.smssecure.smssecure.util.views.Stub;
import org.whispersystems.libsignal.util.guava.Optional;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class AttachmentManager {

    private final static String TAG = AttachmentManager.class.getSimpleName();

    private final @NonNull Context context;
    private final @NonNull Stub<View> attachmentViewStub;
    private final @NonNull AttachmentListener attachmentListener;

    private RemovableMediaView removableMediaView;
    private ThumbnailView thumbnail;
    private AudioView audioView;

    private final @NonNull List<Uri> garbage = new LinkedList<>();
    private @NonNull Optional<Slide> slide = Optional.absent();
    private @Nullable Uri captureUri;

    public AttachmentManager(@NonNull Activity activity, @NonNull AttachmentListener listener) {
        this.context = activity;
        this.attachmentListener = listener;
        this.attachmentViewStub = ViewUtil.findStubById(activity, R.id.attachment_editor_stub);
    }

    public static void selectVideo(Activity activity, int requestCode) {
        Permissions.with(activity)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .ifNecessary()
                .withPermanentDenialDialog(activity.getString(R.string.AttachmentManager_silence_requires_the_external_storage_permission_in_order_to_attach_photos_videos_or_audio))
                .onAllGranted(() -> selectMediaType(activity, "video/*", requestCode))
                .execute();
    }

    public static void selectImage(Activity activity, int requestCode) {
        Permissions.with(activity)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .ifNecessary()
                .withPermanentDenialDialog(activity.getString(R.string.AttachmentManager_silence_requires_the_external_storage_permission_in_order_to_attach_photos_videos_or_audio))
                .onAllGranted(() -> selectMediaType(activity, "image/*", requestCode))
                .execute();
    }

    public static void selectAudio(Activity activity, int requestCode) {
        Permissions.with(activity)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .ifNecessary()
                .withPermanentDenialDialog(activity.getString(R.string.AttachmentManager_silence_requires_the_external_storage_permission_in_order_to_attach_photos_videos_or_audio))
                .onAllGranted(() -> selectMediaType(activity, "audio/*", requestCode))
                .execute();
    }

    public static void selectContactInfo(Activity activity, int requestCode) {
        Permissions.with(activity)
                .request(Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_CONTACTS)
                .ifNecessary()
                .withPermanentDenialDialog(activity.getString(R.string.AttachmentManager_silence_requires_contacts_permission_in_order_to_attach_contact_information))
                .onAllGranted(() -> {
                    Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                    activity.startActivityForResult(intent, requestCode);
                })
                .execute();
    }

    private static void selectMediaType(Activity activity, String type, int requestCode) {
        final Intent intent = new Intent();
        intent.setType(type);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
            try {
                activity.startActivityForResult(intent, requestCode);
                return;
            } catch (ActivityNotFoundException anfe) {
                Log.w(TAG, "couldn't complete ACTION_OPEN_DOCUMENT, no activity found. falling back.");
            }
        }

        intent.setAction(Intent.ACTION_GET_CONTENT);
        try {
            activity.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException anfe) {
            Log.w(TAG, "couldn't complete ACTION_GET_CONTENT intent, no activity found. falling back.");
            Toast.makeText(activity, R.string.AttachmentManager_cant_open_media_selection, Toast.LENGTH_LONG).show();
        }
    }

    private void inflateStub() {
        if (!attachmentViewStub.resolved()) {
            View root = attachmentViewStub.get();

            this.thumbnail = ViewUtil.findById(root, R.id.attachment_thumbnail);
            this.audioView = ViewUtil.findById(root, R.id.attachment_audio);
            this.removableMediaView = ViewUtil.findById(root, R.id.removable_media_view);

            removableMediaView.setRemoveClickListener(new RemoveButtonListener());
            thumbnail.setOnClickListener(new ThumbnailClickListener());
        }

    }

    public void clear() {
        if (attachmentViewStub.resolved()) {
            ViewUtil.fadeOut(attachmentViewStub.get(), 200).addListener(new Listener<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    thumbnail.clear();
                    attachmentViewStub.get().setVisibility(View.GONE);
                    attachmentListener.onAttachmentChanged();
                }

                @Override
                public void onFailure(ExecutionException e) {
                }
            });

            markGarbage(getSlideUri());
            slide = Optional.absent();

            audioView.cleanup();
        }
    }

    public void cleanup() {
        cleanup(captureUri);
        cleanup(getSlideUri());

        captureUri = null;
        slide = Optional.absent();

        Iterator<Uri> iterator = garbage.listIterator();

        while (iterator.hasNext()) {
            cleanup(iterator.next());
            iterator.remove();
        }
    }

    private void cleanup(final @Nullable Uri uri) {
        if (uri != null && PersistentBlobProvider.isAuthority(context, uri)) {
            Log.w(TAG, "cleaning up " + uri);
            PersistentBlobProvider.getInstance(context).delete(uri);
        }
    }

    private void markGarbage(@Nullable Uri uri) {
        if (uri != null && PersistentBlobProvider.isAuthority(context, uri)) {
            Log.w(TAG, "Marking garbage that needs cleaning: " + uri);
            garbage.add(uri);
        }
    }

    private void setSlide(@NonNull Slide slide) {
        if (getSlideUri() != null) cleanup(getSlideUri());
        if (captureUri != null && !captureUri.equals(slide.getUri())) cleanup(captureUri);

        this.captureUri = null;
        this.slide = Optional.of(slide);
    }

    @SuppressLint("StaticFieldLeak")
    public void setMedia(@NonNull final MasterSecret masterSecret,
                         @NonNull final Uri uri,
                         @NonNull final MediaType mediaType,
                         @NonNull final MediaConstraints constraints) {
        inflateStub();

        new AsyncTask<Void, Void, Slide>() {
            @Override
            protected void onPreExecute() {
                thumbnail.clear();
                thumbnail.showProgressSpinner();
                attachmentViewStub.get().setVisibility(View.VISIBLE);
            }

            @Override
            protected @Nullable Slide doInBackground(Void... params) {
                long start = System.currentTimeMillis();
                try {
                    final long mediaSize = MediaUtil.getMediaSize(context, masterSecret, uri);
                    final Slide slide = mediaType.createSlide(context, uri, mediaSize);
                    Log.w(TAG, "slide with size " + mediaSize + " took " + (System.currentTimeMillis() - start) + "ms");
                    return slide;
                } catch (IOException ioe) {
                    Log.w(TAG, ioe);
                    return null;
                }
            }

            @Override
            protected void onPostExecute(@Nullable final Slide slide) {
                if (slide == null) {
                    attachmentViewStub.get().setVisibility(View.GONE);
                    Toast.makeText(context,
                            R.string.ConversationActivity_sorry_there_was_an_error_adding_your_attachment,
                            Toast.LENGTH_SHORT).show();
                } else if (!areConstraintsSatisfied(context, masterSecret, slide, constraints)) {
                    attachmentViewStub.get().setVisibility(View.GONE);
                    Toast.makeText(context,
                            R.string.ConversationActivity_attachment_exceeds_size_limits,
                            Toast.LENGTH_SHORT).show();
                } else {
                    setSlide(slide);
                    attachmentViewStub.get().setVisibility(View.VISIBLE);

                    if (slide.hasAudio()) {
                        audioView.setAudio(masterSecret, (AudioSlide) slide, false);
                        removableMediaView.display(audioView);
                    } else {
                        thumbnail.setImageResource(masterSecret, slide, false);
                        removableMediaView.display(thumbnail);
                    }

                    attachmentListener.onAttachmentChanged();
                }
            }
        }.execute();
    }

    public boolean isAttachmentPresent() {
        return attachmentViewStub.resolved() && attachmentViewStub.get().getVisibility() == View.VISIBLE;
    }

    public @NonNull SlideDeck buildSlideDeck() {
        SlideDeck deck = new SlideDeck();
        if (slide.isPresent()) deck.addSlide(slide.get());
        return deck;
    }

    private @Nullable Uri getSlideUri() {
        return slide.isPresent() ? slide.get().getUri() : null;
    }

    public @Nullable Uri getCaptureUri() {
        return captureUri;
    }

    public void capturePhoto(Activity activity, int requestCode) {
        try {
            Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (captureIntent.resolveActivity(activity.getPackageManager()) != null) {
                if (captureUri == null) {
                    captureUri = PersistentBlobProvider.getInstance(context)
                            .createForExternal(MediaUtil.IMAGE_JPEG);
                }
                Log.w(TAG, "captureUri path is " + captureUri.getPath());
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, captureUri);
                activity.startActivityForResult(captureIntent, requestCode);
            }
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
        }
    }

    private boolean areConstraintsSatisfied(final @NonNull Context context,
                                            final @NonNull MasterSecret masterSecret,
                                            final @Nullable Slide slide,
                                            final @NonNull MediaConstraints constraints) {
        return slide == null ||
                constraints.isSatisfied(context, masterSecret, slide.asAttachment()) ||
                constraints.canResize(slide.asAttachment());
    }

    private void previewImageDraft(final @NonNull Slide slide) {
        if (MediaPreviewActivity.isContentTypeSupported(slide.getContentType()) && slide.getUri() != null) {
            Intent intent = new Intent(context, MediaPreviewActivity.class);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra(MediaPreviewActivity.SIZE_EXTRA, slide.asAttachment().getSize());
            intent.setDataAndType(slide.getUri(), slide.getContentType());

            context.startActivity(intent);
        }
    }

    public enum MediaType {
        IMAGE, GIF, AUDIO, VIDEO;

        public static @Nullable MediaType from(final @Nullable String mimeType) {
            if (TextUtils.isEmpty(mimeType)) return null;
            if (MediaUtil.isGif(mimeType)) return GIF;
            if (MediaUtil.isImageType(mimeType)) return IMAGE;
            if (MediaUtil.isAudioType(mimeType)) return AUDIO;
            if (MediaUtil.isVideoType(mimeType)) return VIDEO;
            return null;
        }

        public @NonNull Slide createSlide(@NonNull Context context,
                                          @NonNull Uri uri,
                                          long dataSize)
                throws IOException {
            switch (this) {
                case IMAGE:
                    return new ImageSlide(context, uri, dataSize);
                case GIF:
                    return new GifSlide(context, uri, dataSize);
                case AUDIO:
                    return new AudioSlide(context, uri, dataSize);
                case VIDEO:
                    return new VideoSlide(context, uri, dataSize);
                default:
                    throw new AssertionError("unrecognized enum");
            }
        }
    }

    public interface AttachmentListener {
        void onAttachmentChanged();
    }

    private class ThumbnailClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (slide.isPresent()) previewImageDraft(slide.get());
        }
    }

    private class RemoveButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            cleanup();
            clear();
        }
    }
}
