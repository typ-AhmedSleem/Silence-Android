package org.smssecure.smssecure;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.ListFragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.cursoradapter.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.database.loaders.BlockedContactsLoader;
import org.smssecure.smssecure.preferences.BlockedContactListItem;
import org.smssecure.smssecure.recipients.RecipientFactory;
import org.smssecure.smssecure.recipients.Recipients;
import org.smssecure.smssecure.util.DynamicLanguage;
import org.smssecure.smssecure.util.DynamicTheme;

public class BlockedContactsActivity extends PassphraseRequiredActionBarActivity {

    private final DynamicTheme dynamicTheme = new DynamicTheme();
    private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

    @Override
    public void onPreCreate() {
        dynamicTheme.onCreate(this);
        dynamicLanguage.onCreate(this);
    }


    @Override
    public void onCreate(Bundle bundle, @NonNull MasterSecret masterSecret) {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.BlockedContactsActivity_blocked_contacts);
        initFragment(android.R.id.content, new BlockedContactsFragment(), masterSecret);
    }

    @Override
    public void onResume() {
        super.onResume();
        dynamicTheme.onResume(this);
        dynamicLanguage.onResume(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return false;
    }

    public static class BlockedContactsFragment
            extends ListFragment
            implements LoaderManager.LoaderCallbacks<Cursor>, ListView.OnItemClickListener {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
            return inflater.inflate(R.layout.blocked_contacts_fragment, container, false);
        }

        @Override
        public void onCreate(Bundle bundle) {
            super.onCreate(bundle);
            setListAdapter(new BlockedContactAdapter(getActivity(), null));
            getLoaderManager().initLoader(0, null, this);
        }

        @Override
        public void onActivityCreated(Bundle bundle) {
            super.onActivityCreated(bundle);
            getListView().setOnItemClickListener(this);
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new BlockedContactsLoader(getActivity());
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (getListAdapter() != null) {
                ((CursorAdapter) getListAdapter()).changeCursor(data);
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            if (getListAdapter() != null) {
                ((CursorAdapter) getListAdapter()).changeCursor(null);
            }
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Recipients recipients = ((BlockedContactListItem) view).getRecipients();
            Intent intent = new Intent(getActivity(), RecipientPreferenceActivity.class);
            intent.putExtra(RecipientPreferenceActivity.RECIPIENTS_EXTRA, recipients.getIds());

            startActivity(intent);
        }

        private static class BlockedContactAdapter extends CursorAdapter {

            public BlockedContactAdapter(Context context, Cursor c) {
                super(context, c);
            }

            @Override
            public View newView(Context context, Cursor cursor, ViewGroup parent) {
                return LayoutInflater.from(context)
                        .inflate(R.layout.blocked_contact_list_item, parent, false);
            }

            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                String recipientIds = cursor.getString(1);
                Recipients recipients = RecipientFactory.getRecipientsForIds(context, recipientIds, true);

                ((BlockedContactListItem) view).set(recipients);
            }
        }

    }

}
