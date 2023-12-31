package org.smssecure.smssecure.audio;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import org.smssecure.smssecure.R;
import org.smssecure.smssecure.attachments.AttachmentServer;
import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.mms.AudioSlide;
import org.smssecure.smssecure.util.Util;
import org.whispersystems.libsignal.util.guava.Optional;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class AudioSlidePlayer {

    private static final String TAG = AudioSlidePlayer.class.getSimpleName();

    private static @NonNull Optional<AudioSlidePlayer> playing = Optional.absent();

    private final @NonNull Context context;
    private final @NonNull MasterSecret masterSecret;
    private final @NonNull AudioSlide slide;
    private final @NonNull Handler progressEventHandler;

    private @NonNull WeakReference<Listener> listener;
    private @Nullable MediaPlayer mediaPlayer;
    private @Nullable AttachmentServer audioAttachmentServer;

    private AudioSlidePlayer(@NonNull Context context,
                             @NonNull MasterSecret masterSecret,
                             @NonNull AudioSlide slide,
                             @NonNull Listener listener) {
        this.context = context;
        this.masterSecret = masterSecret;
        this.slide = slide;
        this.listener = new WeakReference<>(listener);
        this.progressEventHandler = new ProgressEventHandler(this);
    }

    public synchronized static AudioSlidePlayer createFor(@NonNull Context context,
                                                          @NonNull MasterSecret masterSecret,
                                                          @NonNull AudioSlide slide,
                                                          @NonNull Listener listener) {
        if (playing.isPresent() && playing.get().getAudioSlide().equals(slide)) {
            playing.get().setListener(listener);
            return playing.get();
        } else {
            return new AudioSlidePlayer(context, masterSecret, slide, listener);
        }
    }

    public synchronized static void stopAll() {
        if (playing.isPresent()) {
            playing.get().stop();
        }
    }

    private synchronized static void setPlaying(@NonNull AudioSlidePlayer player) {
        if (playing.isPresent() && playing.get() != player) {
            playing.get().notifyOnStop();
            playing.get().stop();
        }

        playing = Optional.of(player);
    }

    private synchronized static void removePlaying(@NonNull AudioSlidePlayer player) {
        if (playing.isPresent() && playing.get() == player) {
            playing = Optional.absent();
        }
    }

    public void play(final double progress) throws IOException {
        if (this.mediaPlayer != null) return;

        this.mediaPlayer = new MediaPlayer();
        this.audioAttachmentServer = new AttachmentServer(context, masterSecret, slide.asAttachment());

        audioAttachmentServer.start();

        mediaPlayer.setDataSource(context, audioAttachmentServer.getUri());
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                Log.w(TAG, "onPrepared");
                synchronized (AudioSlidePlayer.this) {
                    if (mediaPlayer == null) return;

                    if (progress > 0) {
                        mediaPlayer.seekTo((int) (mediaPlayer.getDuration() * progress));
                    }

                    mediaPlayer.start();

                    setPlaying(AudioSlidePlayer.this);
                }

                notifyOnStart();
                progressEventHandler.sendEmptyMessage(0);
            }
        });

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.w(TAG, "onComplete");
                synchronized (AudioSlidePlayer.this) {
                    mediaPlayer = null;

                    if (audioAttachmentServer != null) {
                        audioAttachmentServer.stop();
                        audioAttachmentServer = null;
                    }
                }

                notifyOnStop();
                progressEventHandler.removeMessages(0);
            }
        });

        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.w(TAG, "MediaPlayer Error: " + what + " , " + extra);

                Toast.makeText(context, R.string.AudioSlidePlayer_error_playing_audio, Toast.LENGTH_SHORT).show();

                synchronized (AudioSlidePlayer.this) {
                    mediaPlayer = null;

                    if (audioAttachmentServer != null) {
                        audioAttachmentServer.stop();
                        audioAttachmentServer = null;
                    }
                }

                notifyOnStop();
                progressEventHandler.removeMessages(0);
                return true;
            }
        });

        mediaPlayer.prepareAsync();
    }

    public synchronized void stop() {
        Log.w(TAG, "Stop called!");

        removePlaying(this);

        if (this.mediaPlayer != null) {
            this.mediaPlayer.stop();
        }

        if (this.audioAttachmentServer != null) {
            this.audioAttachmentServer.stop();
        }

        this.mediaPlayer = null;
        this.audioAttachmentServer = null;
    }

    public @NonNull AudioSlide getAudioSlide() {
        return slide;
    }

    private Pair<Double, Integer> getProgress() {
        if (mediaPlayer == null || mediaPlayer.getCurrentPosition() <= 0 || mediaPlayer.getDuration() <= 0) {
            return new Pair<>(0D, 0);
        } else {
            return new Pair<>((double) mediaPlayer.getCurrentPosition() / (double) mediaPlayer.getDuration(),
                    mediaPlayer.getCurrentPosition());
        }
    }

    private void notifyOnStart() {
        Util.runOnMain(new Runnable() {
            @Override
            public void run() {
                getListener().onStart();
            }
        });
    }

    private void notifyOnStop() {
        Util.runOnMain(new Runnable() {
            @Override
            public void run() {
                getListener().onStop();
            }
        });
    }

    private void notifyOnProgress(final double progress, final long millis) {
        Util.runOnMain(new Runnable() {
            @Override
            public void run() {
                getListener().onProgress(progress, millis);
            }
        });
    }

    private @NonNull Listener getListener() {
        Listener listener = this.listener.get();

        if (listener != null) return listener;
        else return new Listener() {
            @Override
            public void onStart() {
            }

            @Override
            public void onStop() {
            }

            @Override
            public void onProgress(double progress, long millis) {
            }
        };
    }

    public void setListener(@NonNull Listener listener) {
        this.listener = new WeakReference<>(listener);

        if (this.mediaPlayer != null && this.mediaPlayer.isPlaying()) {
            notifyOnStart();
        }
    }

    public interface Listener {
        void onStart();

        void onStop();

        void onProgress(double progress, long millis);
    }

    private static class ProgressEventHandler extends Handler {

        private final WeakReference<AudioSlidePlayer> playerReference;

        private ProgressEventHandler(@NonNull AudioSlidePlayer player) {
            this.playerReference = new WeakReference<>(player);
        }

        @Override
        public void handleMessage(Message msg) {
            AudioSlidePlayer player = playerReference.get();

            if (player == null || player.mediaPlayer == null || !player.mediaPlayer.isPlaying()) {
                return;
            }

            Pair<Double, Integer> progress = player.getProgress();
            player.notifyOnProgress(progress.first, progress.second);
            sendEmptyMessageDelayed(0, 50);
        }
    }

}
