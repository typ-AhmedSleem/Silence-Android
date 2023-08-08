/**
 * Copyright (C) 2013 Open Whisper Systems
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
package org.smssecure.smssecure.contacts;

import android.content.Context;
import android.database.Cursor;
import android.database.MergeCursor;
import androidx.loader.content.CursorLoader;
import android.text.TextUtils;

import org.smssecure.smssecure.database.DatabaseFactory;
import org.smssecure.smssecure.util.NumberUtil;

import java.util.ArrayList;

/**
 * CursorLoader that initializes a ContactsDatabase instance
 *
 * @author Jake McGinty
 */
public class ContactsCursorLoader extends CursorLoader {

    private static final String TAG = ContactsCursorLoader.class.getSimpleName();

    private final String filter;
    private final boolean includeSmsContacts;

    public ContactsCursorLoader(Context context, boolean includeSmsContacts, String filter) {
        super(context);

        this.filter = filter;
        this.includeSmsContacts = includeSmsContacts;
    }

    @Override
    public Cursor loadInBackground() {
        ContactsDatabase contactsDatabase = DatabaseFactory.getContactsDatabase(getContext());
        ArrayList<Cursor> cursorList = new ArrayList<>(3);

        cursorList.add(contactsDatabase.querySilenceContacts(filter));

        if (includeSmsContacts) {
            cursorList.add(contactsDatabase.querySystemContacts(filter));
        }

        if (!TextUtils.isEmpty(filter) && NumberUtil.isValidSmsOrEmail(filter)) {
            cursorList.add(contactsDatabase.getNewNumberCursor(filter));
        }

        return new MergeCursor(cursorList.toArray(new Cursor[0]));
    }
}
