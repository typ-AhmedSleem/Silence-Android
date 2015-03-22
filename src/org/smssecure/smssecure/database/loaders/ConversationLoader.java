package org.smssecure.smssecure.database.loaders;

import android.content.Context;
import android.database.Cursor;

import org.smssecure.smssecure.database.DatabaseFactory;
import org.smssecure.smssecure.util.AbstractCursorLoader;

public class ConversationLoader extends AbstractCursorLoader {
  private final long                     threadId;

  public ConversationLoader(Context context, long threadId) {
    super(context);
    this.threadId = threadId;
  }

  @Override
  public Cursor getCursor() {
    return DatabaseFactory.getMmsSmsDatabase(context).getConversation(threadId);
  }
}
