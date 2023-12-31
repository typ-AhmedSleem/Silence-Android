package org.smssecure.smssecure.video;


import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import org.smssecure.smssecure.R;
import org.smssecure.smssecure.attachments.AttachmentServer;
import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.mms.PartAuthority;
import org.smssecure.smssecure.mms.VideoSlide;
import org.smssecure.smssecure.util.ViewUtil;

import java.io.IOException;

public class VideoPlayer extends FrameLayout {

    private static final String TAG = VideoPlayer.class.getName();

    @NonNull
    private final VideoView videoView;
    @Nullable
    private AttachmentServer attachmentServer;

    public VideoPlayer(Context context) {
        this(context, null);
    }

    public VideoPlayer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        inflate(context, R.layout.video_player, this);

        this.videoView = ViewUtil.findById(this, R.id.video_view);

        initializeVideoViewControls(videoView);
    }

    public void setVideoSource(@NonNull MasterSecret masterSecret, @NonNull VideoSlide videoSource) throws IOException {
        if (this.attachmentServer != null) {
            this.attachmentServer.stop();
        }

        if (videoSource.getUri() != null && PartAuthority.isLocalUri(videoSource.getUri())) {
            Log.w(TAG, "Starting video attachment server for part provider Uri...");
            this.attachmentServer = new AttachmentServer(getContext(), masterSecret, videoSource.asAttachment());
            this.attachmentServer.start();

            this.videoView.setVideoURI(this.attachmentServer.getUri());
        } else if (videoSource.getUri() != null) {
            Log.w(TAG, "Playing video directly from non-local Uri...");
            this.videoView.setVideoURI(videoSource.getUri());
        } else {
            Toast.makeText(getContext(), getContext().getString(R.string.VideoPlayer_error_playing_video), Toast.LENGTH_LONG).show();
            return;
        }

        this.videoView.start();
    }

    public void cleanup() {
        if (this.attachmentServer != null) {
            this.attachmentServer.stop();
        }
    }

    private void initializeVideoViewControls(@NonNull VideoView videoView) {
        MediaController mediaController = new MediaController(getContext());
        mediaController.setAnchorView(videoView);
        mediaController.setMediaPlayer(videoView);

        videoView.setMediaController(mediaController);
    }
}
