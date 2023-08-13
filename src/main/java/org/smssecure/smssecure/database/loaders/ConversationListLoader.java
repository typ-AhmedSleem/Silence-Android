package org.smssecure.smssecure.database.loaders;

import static org.smssecure.smssecure.database.ThreadDatabase.ID;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.util.Log;

import org.smssecure.smssecure.AdvancedSearchOptions;
import org.smssecure.smssecure.contacts.ContactAccessor;
import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.database.DatabaseFactory;
import org.smssecure.smssecure.database.ThreadDatabase;
import org.smssecure.smssecure.util.AbstractCursorLoader;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class ConversationListLoader extends AbstractCursorLoader {

    private static final String TAG = ConversationListLoader.class.getSimpleName();

    private final String query;
    private final boolean archived;
    private final MasterSecret masterSecret;
    private final Locale locale;
    private final AdvancedSearchOptions searchOptions;

    public ConversationListLoader(Context context, String query, boolean archived) {
        super(context);

        this.query = query;
        this.archived = archived;

        this.locale = null;
        this.masterSecret = null;
        this.searchOptions = null;
    }

    public ConversationListLoader(MasterSecret secret, Locale locale, Context context, String query, AdvancedSearchOptions options, boolean archived) {
        super(context);

        this.query = query;
        this.archived = archived;
        this.masterSecret = secret;
        this.locale = locale;
        this.searchOptions = options;
    }

    @Override
    public Cursor getCursor() {
        if (query != null && query.trim().length() != 0) {
            return getFilteredConversationList(query.trim().toLowerCase(locale), searchOptions);
        } else if (!archived) {
            return getUnarchivedConversationList();
        } else {
            return getArchivedConversationList();
        }
    }

    private Cursor getUnarchivedConversationList() {
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
                    0, null, 0, -1, 0});

            cursorList.add(switchToArchiveCursor);
        }

        return new MergeCursor(cursorList.toArray(new Cursor[0]));
    }

    private Cursor getArchivedConversationList() {
        return DatabaseFactory.getThreadDatabase(context).getArchivedConversationList();
    }

    private Cursor getFilteredConversationList(String query, AdvancedSearchOptions searchOptions) {

        // ============================ START: MY CODE ============================
        if (masterSecret != null) {
            try {
                final Cursor enhancedFilterCursor = DatabaseFactory.getThreadDatabase(getContext()).enhancedFilterThreads(locale, masterSecret, query, searchOptions);
                if (enhancedFilterCursor != null && enhancedFilterCursor.getCount() > 0) {
//                    Log.v(TAG, "enhancedFilterThreads: Found " + enhancedFilterCursor.getCount() + " messages that contain the given query.");
                    return enhancedFilterCursor;
                } else {
//                    Log.w(TAG, "enhancedFilterThreads: No messages found that contains query.");
                    return null;
                }
            } catch (Throwable e) {
//                Log.e(TAG, "enhancedFilterThreads:", e);
                return null;
            }
        } else {
//            Log.w(TAG, "enhancedFilterThreads: MasterSecret not provided.");
        }
        // ============================ FINISH: MY CODE ===========================

        List<String> numbers = ContactAccessor.getInstance().getNumbersForThreadSearchFilter(context, query);
        return DatabaseFactory.getThreadDatabase(context).getFilteredConversationList(numbers);
    }
}
