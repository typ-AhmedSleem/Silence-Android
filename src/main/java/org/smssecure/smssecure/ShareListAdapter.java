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

import android.content.Context;
import android.database.Cursor;
import androidx.cursoradapter.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

import org.smssecure.smssecure.crypto.MasterCipher;
import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.database.DatabaseFactory;
import org.smssecure.smssecure.database.ThreadDatabase;
import org.smssecure.smssecure.database.model.ThreadRecord;

/**
 * A CursorAdapter for building a list of open conversations
 *
 * @author Jake McGinty
 */
public class ShareListAdapter extends CursorAdapter implements AbsListView.RecyclerListener {

    private final ThreadDatabase threadDatabase;
    private final MasterCipher masterCipher;
    private final Context context;
    private final LayoutInflater inflater;

    public ShareListAdapter(Context context, Cursor cursor, MasterSecret masterSecret) {
        super(context, cursor, 0);

        if (masterSecret != null) this.masterCipher = new MasterCipher(masterSecret);
        else this.masterCipher = null;

        this.context = context;
        this.threadDatabase = DatabaseFactory.getThreadDatabase(context);
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return inflater.inflate(R.layout.share_list_item_view, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if (masterCipher != null) {
            ThreadDatabase.Reader reader = threadDatabase.readerFor(cursor, masterCipher);
            ThreadRecord record = reader.getCurrent();

            ((ShareListItem) view).set(record);
        }
    }

    @Override
    public void onMovedToScrapHeap(View view) {
        ((ShareListItem) view).unbind();
    }
}
