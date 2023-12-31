/**
 * Copyright (C) 2014 Open Whisper Systems
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
package org.smssecure.smssecure;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import androidx.fragment.app.ListFragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.cursoradapter.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.database.loaders.ConversationListLoader;
import org.smssecure.smssecure.recipients.Recipients;

/**
 * A fragment to select and share to open conversations
 *
 * @author Jake McGinty
 */
public class ShareFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private ConversationSelectedListener listener;
    private MasterSecret masterSecret;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        masterSecret = getArguments().getParcelable("master_secret");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        return inflater.inflate(R.layout.share_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);

        initializeListAdapter();
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.listener = (ConversationSelectedListener) activity;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (v instanceof ShareListItem) {
            ShareListItem headerView = (ShareListItem) v;

            handleCreateConversation(headerView.getThreadId(), headerView.getRecipients(),
                    headerView.getDistributionType());
        }
    }

    private void initializeListAdapter() {
        this.setListAdapter(new ShareListAdapter(getActivity(), null, masterSecret));
        getListView().setRecyclerListener((ShareListAdapter) getListAdapter());
        getLoaderManager().restartLoader(0, null, this);
    }

    private void handleCreateConversation(long threadId, Recipients recipients, int distributionType) {
        listener.onCreateConversation(threadId, recipients, distributionType);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        return new ConversationListLoader(getActivity(), null, false);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
        ((CursorAdapter) getListAdapter()).changeCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        ((CursorAdapter) getListAdapter()).changeCursor(null);
    }

    public interface ConversationSelectedListener {
        void onCreateConversation(long threadId, Recipients recipients, int distributionType);
    }
}
