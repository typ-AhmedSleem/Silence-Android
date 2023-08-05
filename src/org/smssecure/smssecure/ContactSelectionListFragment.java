/**
 * Copyright (C) 2015 Open Whisper Systems
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

import android.Manifest;
import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.smssecure.smssecure.components.RecyclerViewFastScroller;
import org.smssecure.smssecure.contacts.ContactSelectionListAdapter;
import org.smssecure.smssecure.contacts.ContactSelectionListItem;
import org.smssecure.smssecure.contacts.ContactsCursorLoader;
import org.smssecure.smssecure.database.CursorRecyclerViewAdapter;
import org.smssecure.smssecure.permissions.Permissions;
import org.smssecure.smssecure.util.StickyHeaderDecoration;
import org.smssecure.smssecure.util.ViewUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Fragment for selecting a one or more contacts from a list.
 *
 * @author Moxie Marlinspike
 *
 */
public class ContactSelectionListFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = ContactSelectionListFragment.class.getSimpleName();

    private TextView emptyText;

    private Map<Long, String> selectedContacts;
    private OnContactSelectedListener onContactSelectedListener;
    private View showContactsLayout;
    private Button showContactsButton;
    private TextView showContactsDescription;
    private String cursorFilter;
    private RecyclerView recyclerView;
    private RecyclerViewFastScroller fastScroller;

    private boolean multi = false;

    @Override
    public void onActivityCreated(Bundle icicle) {
        super.onActivityCreated(icicle);
        super.onCreate(icicle);
        initializeCursor();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.w(TAG, "onStart()");

        Permissions.with(this)
                .request(Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_CONTACTS)
                .ifNecessary()
                .onAllGranted(() -> handleContactPermissionGranted())
                .onAnyDenied(() -> {
                    initializeNoContactsPermission();
                })
                .execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.contact_selection_list_fragment, container, false);

        emptyText = ViewUtil.findById(view, android.R.id.empty);
        recyclerView = ViewUtil.findById(view, R.id.recycler_view);
        fastScroller = ViewUtil.findById(view, R.id.fast_scroller);
        showContactsLayout = view.findViewById(R.id.show_contacts_container);
        showContactsButton = view.findViewById(R.id.show_contacts_button);
        showContactsDescription = view.findViewById(R.id.show_contacts_description);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        return view;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    public List<String> getSelectedContacts() {
        if (selectedContacts == null) return null;

        List<String> selected = new LinkedList<>();
        selected.addAll(selectedContacts.values());

        return selected;
    }

    public void setMultiSelect(boolean multi) {
        this.multi = multi;
    }

    private void initializeCursor() {
        ContactSelectionListAdapter adapter = new ContactSelectionListAdapter(getActivity(),
                null,
                new ListClickListener(),
                multi);
        selectedContacts = adapter.getSelectedContacts();
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new StickyHeaderDecoration(adapter, true, true));
    }

    private void initializeNoContactsPermission() {
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        emptyText.setVisibility(View.GONE);
        showContactsLayout.setVisibility(View.VISIBLE);
        showContactsDescription.setText(R.string.contact_selection_list_fragment__silence_needs_access_to_your_contacts_in_order_to_display_them);
        showContactsButton.setVisibility(View.VISIBLE);

        showContactsButton.setOnClickListener(v -> {
            Permissions.with(this)
                    .request(Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_CONTACTS)
                    .ifNecessary()
                    .withPermanentDenialDialog(getString(R.string.ContactSelectionListFragment_silence_requires_the_contacts_permission_in_order_to_display_your_contacts))
                    .onSomeGranted(permissions -> {
                        if (permissions.contains(Manifest.permission.WRITE_CONTACTS)) {
                            handleContactPermissionGranted();
                        }
                    })
                    .execute();
        });
    }

    public void setQueryFilter(String filter) {
        this.cursorFilter = filter;
        this.getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new ContactsCursorLoader(getActivity(), true, cursorFilter);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        showContactsLayout.setVisibility(View.GONE);

        ((CursorRecyclerViewAdapter) recyclerView.getAdapter()).changeCursor(data);
        emptyText.setText(R.string.contact_selection_group_activity__no_contacts);
        if (recyclerView.getAdapter().getItemCount() > 1) emptyText.setVisibility(View.GONE);
        boolean useFastScroller = (recyclerView.getAdapter().getItemCount() > 20);
        recyclerView.setVerticalScrollBarEnabled(!useFastScroller);
        if (useFastScroller) {
            fastScroller.setVisibility(View.VISIBLE);
            fastScroller.setRecyclerView(recyclerView);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        ((CursorRecyclerViewAdapter) recyclerView.getAdapter()).changeCursor(null);
        fastScroller.setVisibility(View.GONE);
    }

    @SuppressLint("StaticFieldLeak")
    private void handleContactPermissionGranted() {
        this.getLoaderManager().initLoader(0, null, this);
        showContactsLayout.setVisibility(View.GONE);
        emptyText.setVisibility(View.GONE);
    }

    public void setOnContactSelectedListener(OnContactSelectedListener onContactSelectedListener) {
        this.onContactSelectedListener = onContactSelectedListener;
    }

    public interface OnContactSelectedListener {
        void onContactSelected(String number);
    }

    private class ListClickListener implements ContactSelectionListAdapter.ItemClickListener {
        public void onItemClick(ContactSelectionListItem contact) {

            if (!multi || !selectedContacts.containsKey(contact.getContactId())) {
                selectedContacts.put(contact.getContactId(), contact.getNumber());
                contact.setChecked(true);
                if (onContactSelectedListener != null)
                    onContactSelectedListener.onContactSelected(contact.getNumber());
            } else {
                selectedContacts.remove(contact.getContactId());
                contact.setChecked(false);
            }
        }
    }

}
