package org.smssecure.smssecure.database.loaders;

import android.content.Context;
import android.database.Cursor;

import org.smssecure.smssecure.database.DatabaseFactory;
import org.smssecure.smssecure.util.AbstractCursorLoader;

public class ConversationLoader extends AbstractCursorLoader {
    private final long threadId;
    private final long limit;
    private long lastSeen;

    public ConversationLoader(Context context, long threadId, long limit, long lastSeen) {
        super(context);
        this.threadId = threadId;
        this.limit = limit;
        this.lastSeen = lastSeen;
    }

    public boolean hasLimit() {
        return limit > 0;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    @Override
    public Cursor getCursor() {
        if (lastSeen == -1) {
            this.lastSeen = DatabaseFactory.getThreadDatabase(context).getLastSeen(threadId);
        }

        return DatabaseFactory.getMmsSmsDatabase(context).getConversation(threadId, limit);
    }
}
