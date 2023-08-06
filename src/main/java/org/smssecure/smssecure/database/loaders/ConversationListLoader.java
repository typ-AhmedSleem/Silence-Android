package org.smssecure.smssecure.database.loaders;

import static org.smssecure.smssecure.database.ThreadDatabase.*;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.util.Log;

import org.smssecure.smssecure.contacts.ContactAccessor;
import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.database.DatabaseFactory;
import org.smssecure.smssecure.database.ThreadDatabase;
import org.smssecure.smssecure.util.AbstractCursorLoader;

import java.util.LinkedList;
import java.util.List;

public class ConversationListLoader extends AbstractCursorLoader {

    private static final String TAG = ConversationListLoader.class.getSimpleName();

    private final String query;
    private final boolean archived;

    private final MasterSecret masterSecret;

    public ConversationListLoader (Context context, String query, boolean archived){
        super(context);
        this.query = query;
        this.archived = archived;
        masterSecret = null;
    }

    public ConversationListLoader (MasterSecret secret, Context context, String query, boolean archived){
        super(context);
        this.query = query;
        this.archived = archived;
        this.masterSecret = secret;
    }

    @Override
    public Cursor getCursor (){
        if (query != null && query.trim().length() != 0) {
            return getFilteredConversationList(query);
        } else if (!archived) {
            return getUnarchivedConversationList();
        } else {
            return getArchivedConversationList();
        }
    }

    private Cursor getUnarchivedConversationList (){
        List<Cursor> cursorList = new LinkedList<>();
        cursorList.add(DatabaseFactory.getThreadDatabase(context).getConversationList());

        int archivedCount = DatabaseFactory.getThreadDatabase(context).getArchivedConversationListCount();

        if (archivedCount > 0) {
            MatrixCursor switchToArchiveCursor = new MatrixCursor(
                    new String[]{
                            ID,
                            ThreadDatabase.DATE,
                            ThreadDatabase.MESSAGE_COUNT,
                            ThreadDatabase.RECIPIENT_IDS,
                            ThreadDatabase.SNIPPET,
                            ThreadDatabase.READ,
                            ThreadDatabase.TYPE,
                            ThreadDatabase.SNIPPET_TYPE,
                            ThreadDatabase.SNIPPET_URI,
                            ThreadDatabase.ARCHIVED,
                            ThreadDatabase.STATUS,
                            ThreadDatabase.LAST_SEEN
                    }, 1);

            switchToArchiveCursor.addRow(new Object[]{
                    -1L,
                    System.currentTimeMillis(), archivedCount,
                    "-1", null, 1,
                    ThreadDatabase.DistributionTypes.ARCHIVE,
                    0, null, 0, -1, 0 });

            cursorList.add(switchToArchiveCursor);
        }

        return new MergeCursor(cursorList.toArray(new Cursor[0]));
    }

    private Cursor getArchivedConversationList (){
        return DatabaseFactory.getThreadDatabase(context).getArchivedConversationList();
    }

    private Cursor getFilteredConversationList (String query){

        // ============================ START: MY CODE ============================
        final int MESSAGES_LIMIT_PER_THREAD = 2;
        // Enhanced filter
        if (masterSecret != null) {
            try {
                final Cursor enhancedFilterCursor = DatabaseFactory.getThreadDatabase(getContext()).enhancedFilterThreads(masterSecret, query, MESSAGES_LIMIT_PER_THREAD);
                if (enhancedFilterCursor != null && enhancedFilterCursor.getCount() > 0) {
                    Log.v(TAG, "enhancedFilterThreads: Found " + enhancedFilterCursor.getCount() + " messages that contain the given query.");
                    return enhancedFilterCursor;
                } else {
                    Log.w(TAG, "enhancedFilterThreads: No messages found that contains query.");
                }
            } catch (Throwable e) {
                Log.e(TAG, "enhancedFilterThreads:", e);
            }
        } else {
            Log.w(TAG, "enhancedFilterThreads: MasterSecret not provided.");
        }
        // ============================ FINISH: MY CODE ===========================

        List<String> numbers = ContactAccessor.getInstance().getNumbersForThreadSearchFilter(context, query);
        return DatabaseFactory.getThreadDatabase(context).getFilteredConversationList(numbers);
    }
}
