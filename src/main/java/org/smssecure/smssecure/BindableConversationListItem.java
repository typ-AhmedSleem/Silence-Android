package org.smssecure.smssecure;

import androidx.annotation.NonNull;

import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.database.model.ThreadRecord;

import java.util.Locale;
import java.util.Set;

public interface BindableConversationListItem extends Unbindable {

    void bind(@NonNull MasterSecret masterSecret, @NonNull ThreadRecord thread,
                     @NonNull Locale locale, @NonNull Set<Long> selectedThreads, boolean batchMode);
}
