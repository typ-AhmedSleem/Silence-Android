package org.smssecure.smssecure.providers;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.content.UriMatcher;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import android.webkit.MimeTypeMap;

import org.smssecure.smssecure.crypto.DecryptingPartInputStream;
import org.smssecure.smssecure.crypto.EncryptingPartOutputStream;
import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.util.FileProviderUtil;
import org.smssecure.smssecure.util.Util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PersistentBlobProvider {

    public static final String AUTHORITY = "org.smssecure.smssecure";
    public static final String EXPECTED_PATH = "capture/*/*/#";
    private static final String TAG = PersistentBlobProvider.class.getSimpleName();
    private static final String URI_STRING = "content://org.smssecure.smssecure/capture";
    public static final Uri CONTENT_URI = Uri.parse(URI_STRING);
    private static final int MIMETYPE_PATH_SEGMENT = 1;
    private static final String BLOB_EXTENSION = "blob";
    private static final int MATCH = 1;
    private static final UriMatcher MATCHER = new UriMatcher(UriMatcher.NO_MATCH) {{
        addURI(AUTHORITY, EXPECTED_PATH, MATCH);
    }};

    private static volatile PersistentBlobProvider instance;
    private final Context context;
    @SuppressLint("UseSparseArrays")
    private final Map<Long, byte[]> cache = Collections.synchronizedMap(new HashMap<Long, byte[]>());
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private PersistentBlobProvider(Context context) {
        this.context = context.getApplicationContext();
    }

    public static PersistentBlobProvider getInstance(Context context) {
        if (instance == null) {
            synchronized (PersistentBlobProvider.class) {
                if (instance == null) {
                    instance = new PersistentBlobProvider(context);
                }
            }
        }
        return instance;
    }

    public static @Nullable String getMimeType(@NonNull Context context, @NonNull Uri persistentBlobUri) {
        if (!isAuthority(context, persistentBlobUri)) return null;
        return isExternalBlobUri(context, persistentBlobUri)
                ? getMimeTypeFromExtension(persistentBlobUri)
                : persistentBlobUri.getPathSegments().get(MIMETYPE_PATH_SEGMENT);
    }

    private static @NonNull String getExtensionFromMimeType(String mimeType) {
        final String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        return extension != null ? extension : BLOB_EXTENSION;
    }

    private static @NonNull String getMimeTypeFromExtension(@NonNull Uri uri) {
        final String mimeType = MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri.toString()));
        return mimeType != null ? mimeType : "application/octet-stream";
    }

    private static @NonNull File getExternalDir(Context context) throws IOException {
        final File externalDir = context.getExternalFilesDir(null);
        if (externalDir == null) throw new IOException("no external files directory");
        return externalDir;
    }

    public static boolean isAuthority(@NonNull Context context, @NonNull Uri uri) {
        return MATCHER.match(uri) == MATCH || isExternalBlobUri(context, uri);
    }

    private static boolean isExternalBlobUri(@NonNull Context context, @NonNull Uri uri) {
        try {
            return uri.getPath().startsWith(getExternalDir(context).getAbsolutePath());
        } catch (IOException ioe) {
            return false;
        }
    }

    public Uri create(@NonNull MasterSecret masterSecret,
                      @NonNull byte[] blobBytes,
                      @NonNull String mimeType) {
        final long id = System.currentTimeMillis();
        cache.put(id, blobBytes);
        return create(masterSecret, new ByteArrayInputStream(blobBytes), id, mimeType);
    }

    public Uri create(@NonNull MasterSecret masterSecret,
                      @NonNull InputStream input,
                      @NonNull String mimeType) {
        return create(masterSecret, input, System.currentTimeMillis(), mimeType);
    }

    private Uri create(MasterSecret masterSecret, InputStream input, long id, String mimeType) {
        persistToDisk(masterSecret, id, input);
        final Uri uniqueUri = CONTENT_URI.buildUpon()
                .appendPath(mimeType)
                .appendEncodedPath(String.valueOf(System.currentTimeMillis()))
                .build();
        return ContentUris.withAppendedId(uniqueUri, id);
    }

    private void persistToDisk(final MasterSecret masterSecret, final long id, final InputStream input) {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    OutputStream output = new EncryptingPartOutputStream(getFile(id), masterSecret);
                    Log.w(TAG, "Starting stream copy....");
                    Util.copy(input, output);
                    Log.w(TAG, "Stream copy finished...");
                } catch (IOException e) {
                    Log.w(TAG, e);
                }

                cache.remove(id);
            }
        });
    }

    public Uri createForExternal(@NonNull String mimeType) throws IOException {
        File target = new File(getExternalDir(context), System.currentTimeMillis() + "." + getExtensionFromMimeType(mimeType));
        return FileProviderUtil.getUriFor(context, target);
    }

    public boolean delete(@NonNull Uri uri) {
        if (MATCHER.match(uri) == MATCH) {
            long id = ContentUris.parseId(uri);
            cache.remove(id);
            return getFile(ContentUris.parseId(uri)).delete();
        }
        return new File(uri.getPath()).delete();
    }

    public @NonNull InputStream getStream(MasterSecret masterSecret, long id) throws IOException {
        final byte[] cached = cache.get(id);
        return cached != null ? new ByteArrayInputStream(cached)
                : new DecryptingPartInputStream(getFile(id), masterSecret, null);
    }

    private File getFile(long id) {
        return new File(context.getDir("captures", Context.MODE_PRIVATE), id + "." + BLOB_EXTENSION);
    }
}
