package org.smssecure.smssecure.attachments;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.smssecure.smssecure.database.AttachmentDatabase;

public abstract class Attachment {

    @NonNull
    private final String contentType;
    private final int transferState;
    private final long size;

    @Nullable
    private final String location;

    @Nullable
    private final String key;

    @Nullable
    private final String relay;

    @Nullable
    private final byte[] digest;

    public Attachment(@NonNull String contentType, int transferState, long size,
                      @Nullable String location, @Nullable String key, @Nullable String relay,
                      @Nullable byte[] digest) {
        this.contentType = contentType;
        this.transferState = transferState;
        this.size = size;
        this.location = location;
        this.key = key;
        this.relay = relay;
        this.digest = digest;
    }

    @Nullable
    public abstract Uri getDataUri();

    @Nullable
    public abstract Uri getThumbnailUri();

    public int getTransferState() {
        return transferState;
    }

    public boolean isInProgress() {
        return transferState != AttachmentDatabase.TRANSFER_PROGRESS_DONE &&
                transferState != AttachmentDatabase.TRANSFER_PROGRESS_FAILED;
    }

    public long getSize() {
        return size;
    }

    @NonNull
    public String getContentType() {
        return contentType;
    }

    @Nullable
    public String getLocation() {
        return location;
    }

    @Nullable
    public String getKey() {
        return key;
    }

    @Nullable
    public String getRelay() {
        return relay;
    }

    @Nullable
    public byte[] getDigest() {
        return digest;
    }
}
