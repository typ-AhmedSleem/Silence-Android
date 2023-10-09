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

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.jude.easyrecyclerview.EasyRecyclerView;
import com.jude.easyrecyclerview.decoration.StickyHeaderDecoration;

import org.smssecure.smssecure.ConversationListAdapter.ItemClickListener;
import org.smssecure.smssecure.attachments.Attachment;
import org.smssecure.smssecure.attachments.UriAttachment;
import org.smssecure.smssecure.components.reminder.DefaultSmsReminder;
import org.smssecure.smssecure.components.reminder.DeliveryReportsReminder;
import org.smssecure.smssecure.components.reminder.Reminder;
import org.smssecure.smssecure.components.reminder.ReminderView;
import org.smssecure.smssecure.components.reminder.StoreRatingReminder;
import org.smssecure.smssecure.components.reminder.SystemSmsImportReminder;
import org.smssecure.smssecure.crypto.MasterCipher;
import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.crypto.SessionUtil;
import org.smssecure.smssecure.database.AttachmentDatabase;
import org.smssecure.smssecure.database.DatabaseFactory;
import org.smssecure.smssecure.database.DraftDatabase;
import org.smssecure.smssecure.database.ThreadDatabase;
import org.smssecure.smssecure.database.loaders.ConversationListLoader;
import org.smssecure.smssecure.database.model.ThreadRecord;
import org.smssecure.smssecure.mms.OutgoingMediaMessage;
import org.smssecure.smssecure.mms.OutgoingSecureMediaMessage;
import org.smssecure.smssecure.notifications.MessageNotifier;
import org.smssecure.smssecure.recipients.Recipients;
import org.smssecure.smssecure.search.GlobalSearchResult;
import org.smssecure.smssecure.search.SearchManager;
import org.smssecure.smssecure.search.SearchResultsAdapter;
import org.smssecure.smssecure.search.SearchStickyHeaderAdapter;
import org.smssecure.smssecure.search.contacts.ContactRecord;
import org.smssecure.smssecure.search.threads.ConversationRecord;
import org.smssecure.smssecure.sms.MessageSender;
import org.smssecure.smssecure.sms.OutgoingEncryptedMessage;
import org.smssecure.smssecure.sms.OutgoingTextMessage;
import org.smssecure.smssecure.util.Util;
import org.smssecure.smssecure.util.ViewUtil;
import org.smssecure.smssecure.util.task.SnackbarAsyncTask;
import org.whispersystems.libsignal.util.guava.Optional;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ConversationListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        ActionMode.Callback, ItemClickListener, SearchOptionsBottomSheet.Callback {

    public static final String ARCHIVE = "archive";
    private static final String TAG = ConversationListFragment.class.getSimpleName();
    private MasterSecret masterSecret;
    private ActionMode actionMode;
    private RecyclerView rvConversations;
    private EasyRecyclerView ervSearch;
    private ReminderView reminderView;
    private FloatingActionButton fab;
    private SearchOptionsBottomSheet bsSearchOptions;
    private Locale locale;
    private boolean isShowingArchive;

    // Search related runtime
    private @NonNull String searchQuery = "";
    private boolean isSearchActive = false;
    private AdvancedSearchOptions searchOptions;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        masterSecret = requireArguments().getParcelable("master_secret");
        locale = (Locale) requireArguments().getSerializable(PassphraseRequiredActionBarActivity.LOCALE_EXTRA);
        isShowingArchive = requireArguments().getBoolean(ARCHIVE, false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        final View view = inflater.inflate(R.layout.conversation_list_fragment, container, false);
        reminderView = ViewUtil.findById(view, R.id.reminder);
        rvConversations = ViewUtil.findById(view, R.id.rv_conversations);
        ervSearch = ViewUtil.findById(view, R.id.erv_search);
        fab = ViewUtil.findById(view, R.id.fab);

        fab.setVisibility(isShowingArchive ? View.GONE : View.VISIBLE);

        rvConversations.setHasFixedSize(true);
        rvConversations.setItemAnimator(new DefaultItemAnimator());
        rvConversations.setLayoutManager(new LinearLayoutManager(requireActivity()));
        rvConversations.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        ervSearch.setItemAnimator(new DefaultItemAnimator());
        ervSearch.setLayoutManager(new LinearLayoutManager(requireContext()));
        ervSearch.setAdapter(new SearchResultsAdapter(requireContext(), masterSecret, locale));
        ervSearch.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        ervSearch.addItemDecoration(new StickyHeaderDecoration(new SearchStickyHeaderAdapter(requireContext(), getSearchResultsAdapter())));
        getSearchResultsAdapter().setOnItemClickListener(position -> {
            final GlobalSearchResult result = getSearchResultsAdapter().getItem(position);
            if (result instanceof GlobalSearchResult.ContactSearchResult) {
                // Show contact at AddressBook
                final ContactRecord contact = ((GlobalSearchResult.ContactSearchResult) result).getContact();
                final Intent intent = new Intent(Intent.ACTION_VIEW);
                final Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, String.valueOf(contact.getId()));
                intent.setData(uri);
                startActivity(intent);
            } else {
                // Open or create a conversation
                final ConversationRecord conversation = ((GlobalSearchResult.MessageSearchResult) result).getConversation();
                final ThreadRecord thread = conversation.getRecord();
                final Intent intent = new Intent(getContext(), ConversationActivity.class);
                intent.putExtra(ConversationActivity.RECIPIENTS_EXTRA, thread.getRecipients().getIds());
                intent.putExtra(ConversationActivity.THREAD_ID_EXTRA, thread.getThreadId());
                intent.putExtra(ConversationActivity.DISTRIBUTION_TYPE_EXTRA, thread.getDistributionType());
                intent.putExtra(ConversationActivity.TIMING_EXTRA, System.currentTimeMillis());
                intent.putExtra(ConversationActivity.LAST_SEEN_EXTRA, thread.getLastSeen());
                intent.putExtra(ConversationActivity.LAST_SEEN_EXTRA, thread.getLastSeen());
                intent.putExtra(ConversationActivity.MESSAGE_TO_HIGHLIGHT, conversation.getEncryptedBody());
                startActivity(intent);
                requireActivity().overridePendingTransition(R.anim.slide_from_right, R.anim.fade_scale_out);
            }
        });

        new ItemTouchHelper(new ArchiveListenerCallback()).attachToRecyclerView(rvConversations);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setHasOptionsMenu(true);

        if (bsSearchOptions == null) bsSearchOptions = new SearchOptionsBottomSheet(requireContext(), this);

        fab.setOnClickListener(v -> {
            if (isSearchActive) bsSearchOptions.show();
            else startActivity(new Intent(requireActivity(), NewConversationActivity.class));
        });

        initializeListAdapter();
    }

    public void unselectAllThreads() {
        try {
            getListAdapter().unselectAllThreads();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onResume() {
        super.onResume();

        initializeReminders();
        getListAdapter().notifyDataSetChanged();
    }

    @NonNull
    public ConversationListAdapter getListAdapter() {
        return (ConversationListAdapter) rvConversations.getAdapter();
    }

    // ============== START: Search ============== //
    public void performSearch(String query) {
        if (searchQuery.equals(query)) return;
        searchQuery = query;
        if (isSearchActive) {
            ervSearch.showProgress();
            SearchManager.performGlobalSearch(requireContext(),
                    locale,
                    masterSecret,
                    searchOptions,
                    searchQuery,
                    results -> {
                        if (results.isEmpty()) {
                            ervSearch.showEmpty();
                            return;
                        }
                        final SearchResultsAdapter adapter = getSearchResultsAdapter();
                        if (adapter == null) return;
                        adapter.clear();
                        ervSearch.showRecycler();
                        adapter.addAll(results);
                    });
        }
//        LoaderManager.getInstance(this).restartLoader(0, null, this);
    }

    private SearchResultsAdapter getSearchResultsAdapter() {
        return ((SearchResultsAdapter) ervSearch.getAdapter());
    }

    public void openSearch() {
        searchQuery = "";
        isSearchActive = true;

        fab.setImageResource(R.drawable.ic_search_options);
        rvConversations.setVisibility(View.GONE);
        ervSearch.setVisibility(View.VISIBLE);
        getSearchResultsAdapter().clear();
        ervSearch.showRecycler();
    }

    public void closeSearch() {
        searchQuery = "";
        isSearchActive = false;

        fab.setImageResource(R.drawable.ic_create_white_24dp);
        rvConversations.setVisibility(View.VISIBLE);
        ervSearch.setVisibility(View.GONE);
        getSearchResultsAdapter().clear();
        ervSearch.showRecycler();
    }

    @Override
    public void onLoadLastOptions(AdvancedSearchOptions searchOptions) {
        this.searchOptions = searchOptions;
    }

    @Override
    public void onSetSearchOptions(AdvancedSearchOptions searchOptions) {
        this.searchOptions = searchOptions;
        if (isSearchActive) performSearch(searchQuery);
    }
    // ============== END: Search ============== //

    @SuppressLint("StaticFieldLeak")
    private void initializeReminders() {
        reminderView.hide();
        new AsyncTask<Context, Void, Optional<? extends Reminder>>() {
            @Override
            protected Optional<? extends Reminder> doInBackground(Context... params) {
                final Context context = params[0];
                if (DefaultSmsReminder.isEligible(context)) {
                    return Optional.of(new DefaultSmsReminder(context));
                } else if (Util.isDefaultSmsProvider(context) && SystemSmsImportReminder.isEligible(context)) {
                    return Optional.of((new SystemSmsImportReminder(context, masterSecret)));
                } else if (DeliveryReportsReminder.isEligible(context)) {
                    return Optional.of((new DeliveryReportsReminder(context)));
                } else if (StoreRatingReminder.isEligible(context)) {
                    return Optional.of((new StoreRatingReminder(context)));
                } else {
                    return Optional.absent();
                }
            }

            @Override
            protected void onPostExecute(Optional<? extends Reminder> reminder) {
                if (reminder.isPresent() && requireActivity() != null && !isRemoving()) {
                    reminderView.showReminder(reminder.get());
                }
            }
        }.execute(requireActivity());
    }

    private void initializeListAdapter() {
        rvConversations.setAdapter(new ConversationListAdapter(requireActivity(), masterSecret, locale, null, this));
        LoaderManager.getInstance(this).restartLoader(0, null, this);
    }

    @SuppressLint("StaticFieldLeak")
    private void handleArchiveAllSelected() {
        final Set<Long> selectedConversations = new HashSet<>(getListAdapter().getBatchSelections());
        final boolean archive = this.isShowingArchive;

        int snackBarTitleId;

        if (archive) {
            snackBarTitleId = R.plurals.ConversationListFragment_moved_conversations_to_inbox;
        } else {
            snackBarTitleId = R.plurals.ConversationListFragment_conversations_archived;
        }

        int count = selectedConversations.size();
        String snackBarTitle = getResources().getQuantityString(snackBarTitleId, count, count);

        new SnackbarAsyncTask<Void>(getView(), snackBarTitle, getString(R.string.ConversationListFragment_undo), getResources().getColor(R.color.amber_500), Snackbar.LENGTH_LONG, true) {

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);

                if (actionMode != null) {
                    actionMode.finish();
                    actionMode = null;
                }
            }

            @SuppressLint("StaticFieldLeak")
            @Override
            protected void executeAction(@Nullable Void parameter) {
                for (long threadId : selectedConversations) {
                    if (!archive) {
                        DatabaseFactory.getThreadDatabase(requireActivity()).archiveConversation(threadId);
                    } else {
                        DatabaseFactory.getThreadDatabase(requireActivity()).unarchiveConversation(threadId);
                    }
                }
            }

            @Override
            protected void reverseAction(@Nullable Void parameter) {
                for (long threadId : selectedConversations) {
                    if (!archive) {
                        DatabaseFactory.getThreadDatabase(requireActivity()).unarchiveConversation(threadId);
                    } else {
                        DatabaseFactory.getThreadDatabase(requireActivity()).archiveConversation(threadId);
                    }
                }
            }
        }.execute();
    }

    @SuppressLint("StaticFieldLeak")
    private void handleDeleteAllSelected() {
        int conversationsCount = getListAdapter().getBatchSelections().size();
        AlertDialog.Builder alert = new AlertDialog.Builder(requireActivity());
        alert.setIconAttribute(R.attr.dialog_alert_icon);
        alert.setTitle(requireActivity().getResources().getQuantityString(R.plurals.ConversationListFragment_delete_selected_conversations, conversationsCount, conversationsCount));
        alert.setMessage(requireActivity().getResources().getQuantityString(R.plurals.ConversationListFragment_this_will_permanently_delete_all_n_selected_conversations, conversationsCount, conversationsCount));
        alert.setCancelable(true);

        alert.setPositiveButton(R.string.delete, (dialog, which) -> {
            final Set<Long> selectedConversations = (getListAdapter()).getBatchSelections();

            if (!selectedConversations.isEmpty()) {
                new AsyncTask<Void, Void, Void>() {
                    private ProgressDialog dialog;

                    @Override
                    protected void onPreExecute() {
                        dialog = ProgressDialog.show(requireActivity(), requireActivity().getString(R.string.ConversationListFragment_deleting), requireActivity().getString(R.string.ConversationListFragment_deleting_selected_conversations), true, false);
                    }

                    @Override
                    protected Void doInBackground(Void... params) {
                        DatabaseFactory.getThreadDatabase(requireActivity()).deleteConversations(selectedConversations);
                        MessageNotifier.updateNotification(requireActivity(), masterSecret);
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void result) {
                        dialog.dismiss();
                        if (actionMode != null) {
                            actionMode.finish();
                            actionMode = null;
                        }
                    }
                }.execute();
            }
        });

        alert.setNegativeButton(android.R.string.cancel, null);
        alert.show();
    }

    private void handleTogglePinAllSelected() {
        final Set<Long> selectedConversations = new HashSet<>(getListAdapter().getBatchSelections());
        for (long threadId : selectedConversations) {
            DatabaseFactory.getThreadDatabase(requireActivity()).toggleThreadPin(threadId);
        }
        if (actionMode != null) {
            actionMode.finish();
            actionMode = null;
        }
    }

    @SuppressLint("StringFormatMatches")
    private void handleSelectAllThreads() {
        getListAdapter().selectAllThreads();
        actionMode.setSubtitle(getString(R.string.conversation_fragment_cab__batch_selection_amount, getListAdapter().getBatchSelections().size()));
    }

    private void handleCreateConversation(long threadId, Recipients recipients, int distributionType, long lastSeen) {
        ((ConversationSelectedListener) requireActivity()).onCreateConversation(threadId, recipients, distributionType, lastSeen);
    }

    @SuppressLint("StaticFieldLeak")
    private void handleSendDrafts() {
        AlertDialog.Builder alert = new AlertDialog.Builder(requireActivity());
        alert.setIconAttribute(R.attr.dialog_alert_icon);
        alert.setTitle(getString(R.string.ConversationListFragment_send_drafts));
        alert.setMessage(getString(R.string.ConversationListFragment_this_will_send_drafts_of_selected_conversations));
        alert.setCancelable(true);

        alert.setPositiveButton(R.string.ConversationListFragment_send, (dialog, which) -> {
            final Set<Long> selectedConversations = new HashSet<>(getListAdapter().getBatchSelections());
            final Context context = requireActivity();

            if (!selectedConversations.isEmpty() && masterSecret != null) {
                final MasterCipher masterCipher = new MasterCipher(masterSecret);

                new AsyncTask<Void, Void, Void>() {
                    private ProgressDialog dialog;
                    private boolean isSingleConversation;
                    private boolean isSecureDestination;
                    private DraftDatabase draftDatabase;
                    private Recipients recipients;

                    @Override
                    protected void onPreExecute() {
                        dialog = ProgressDialog.show(context, context.getString(R.string.ConversationListFragment_sending), context.getString(R.string.ConversationListFragment_sending_selected_drafts), true, false);
                    }

                    @Override
                    protected Void doInBackground(Void... params) {
                        draftDatabase = DatabaseFactory.getDraftDatabase(context);

                        for (long threadId : selectedConversations) {
                            List<DraftDatabase.Draft> drafts = draftDatabase.getDrafts(masterCipher, threadId);
                            recipients = getListAdapter().getRecipientsFromThreadId(threadId);

                            if (recipients != null) {
                                isSingleConversation = recipients.isSingleRecipient() && !recipients.isGroupRecipient();
                                isSecureDestination = isSingleConversation && SessionUtil.hasSession(context, masterSecret, recipients.getPrimaryRecipient().getNumber(), recipients.getDefaultSubscriptionId());

                                Log.w(TAG, "Number of drafts: " + drafts.size());
                                if (drafts.size() > 1 && !drafts.get(1).getType().equals(DraftDatabase.Draft.TEXT)) {
                                    sendMediaDraft(drafts.get(1), threadId, drafts.get(0).getValue());
                                } else {
                                    for (DraftDatabase.Draft draft : drafts) {
                                        Log.w(TAG, "getType(): " + draft.getType());
                                        if (draft.getType().equals(DraftDatabase.Draft.TEXT)) {
                                            sendTextDraft(draft, threadId);
                                        } else {
                                            sendMediaDraft(draft, threadId, null);
                                        }
                                    }
                                }
                            } else {
                                Log.w(TAG, "null recipients when sending drafts ?!");
                            }
                            draftDatabase.clearDrafts(threadId);
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void result) {
                        dialog.dismiss();
                        if (actionMode != null) {
                            actionMode.finish();
                            actionMode = null;
                        }
                    }

                    private void sendTextDraft(DraftDatabase.Draft draft, long threadId) {
                        OutgoingTextMessage message;
                        if (isSecureDestination) {
                            message = new OutgoingEncryptedMessage(recipients, draft.getValue(), recipients.getDefaultSubscriptionId());
                        } else {
                            message = new OutgoingTextMessage(recipients, draft.getValue(), recipients.getDefaultSubscriptionId());
                        }
                        MessageSender.send(context, masterSecret, message, threadId, false);
                    }

                    private void sendMediaDraft(DraftDatabase.Draft draft, long threadId, @Nullable String forcedValue) {
                        List<Attachment> attachment = new LinkedList<Attachment>();
                        attachment.add(new UriAttachment(Uri.parse(draft.getValue()), draft.getType() + "/*", AttachmentDatabase.TRANSFER_PROGRESS_DONE, 0));

                        OutgoingMediaMessage message = new OutgoingMediaMessage(recipients, forcedValue != null ? forcedValue : "", attachment, System.currentTimeMillis(), recipients.getDefaultSubscriptionId(), ThreadDatabase.DistributionTypes.BROADCAST);

                        if (isSecureDestination) {
                            message = new OutgoingSecureMediaMessage(message);
                        }
                        MessageSender.send(context, masterSecret, message, threadId, false);
                    }
                }.execute();
            }
        });

        alert.setNegativeButton(android.R.string.cancel, null);
        alert.show();
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        return new ConversationListLoader(masterSecret, locale, requireActivity(), searchQuery, searchOptions, isShowingArchive);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
        getListAdapter().changeCursor(cursor);
        if (getListAdapter().getItemCount() > 0) {
            // todo: Hide empty msg
        } else {
            // todo: Show empty msg
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> arg0) {
        getListAdapter().changeCursor(null);
    }

    @SuppressLint("StringFormatMatches")
    @Override
    public void onItemClick(ConversationListItem item) {
        if (actionMode == null) {
            handleCreateConversation(item.getThreadId(), item.getRecipients(), item.getDistributionType(), item.getLastSeen());
        } else {
            if (isSearchActive) return;

            ConversationListAdapter adapter = (ConversationListAdapter) rvConversations.getAdapter();
            adapter.toggleThreadInBatchSet(item.getThreadId());
            adapter.populateRecipients(item.getThreadId(), item.getRecipients());

            if (adapter.getBatchSelections().size() == 0) actionMode.finish();
            else actionMode.setSubtitle(getString(R.string.conversation_fragment_cab__batch_selection_amount, adapter.getBatchSelections().size()));

            adapter.notifyDataSetChanged();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onItemLongClick(ConversationListItem item) {
        // Disable selection if search mode is active
        if (isSearchActive) return;

        actionMode = ((AppCompatActivity) requireActivity()).startSupportActionMode(ConversationListFragment.this);
        getListAdapter().initializeBatchMode(true);
        getListAdapter().toggleThreadInBatchSet(item.getThreadId());
        getListAdapter().populateRecipients(item.getThreadId(), item.getRecipients());
        getListAdapter().notifyDataSetChanged();
    }

    @Override
    public void onSwitchToArchive() {
        ((ConversationSelectedListener) requireActivity()).onSwitchToArchive();
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = requireActivity().getMenuInflater();

        if (isShowingArchive) {
            inflater.inflate(R.menu.conversation_list_batch_unarchive, menu);
        } else {
            inflater.inflate(R.menu.conversation_list_batch_archive, menu);
        }

        inflater.inflate(R.menu.conversation_list_batch, menu);
        inflater.inflate(R.menu.conversation_send_drafts, menu);

        mode.setTitle(R.string.conversation_fragment_cab__batch_selection_mode);
        mode.setSubtitle(null);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = requireActivity().getWindow();
            window.setStatusBarColor(getResources().getColor(R.color.action_mode_status_bar));
            window.setNavigationBarColor(getResources().getColor(android.R.color.black));
        }

        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_select_all) {
            handleSelectAllThreads();
            return true;
        } else if (itemId == R.id.menu_delete_selected) {
            handleDeleteAllSelected();
            return true;
        } else if (itemId == R.id.menu_archive_selected) {
            handleArchiveAllSelected();
            return true;
        } else if (itemId == R.id.menu_toggle_pin_selected) {
            handleTogglePinAllSelected();
            return true;
        } else if (itemId == R.id.menu_send_drafts) {
            handleSendDrafts();
            return true;
        }

        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        getListAdapter().initializeBatchMode(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = requireActivity().getWindow();
            TypedArray color = requireActivity().getTheme().obtainStyledAttributes(new int[]{android.R.attr.statusBarColor});
            window.setStatusBarColor(color.getColor(0, Color.BLACK));
            window.setNavigationBarColor(getResources().getColor(android.R.color.black));
            color.recycle();
        }

        actionMode = null;
    }

    public interface ConversationSelectedListener {
        void onCreateConversation(long threadId, Recipients recipients, int distributionType, long lastSeen);

        void onSwitchToArchive();
    }

    private class ArchiveListenerCallback extends ItemTouchHelper.SimpleCallback {

        public ArchiveListenerCallback() {
            super(0, ItemTouchHelper.RIGHT);
        }

        @Override
        public boolean onMove(
                @NonNull RecyclerView recyclerView,
                @NonNull RecyclerView.ViewHolder viewHolder,
                @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public int getSwipeDirs(@NonNull RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            if (viewHolder.itemView instanceof ConversationListItemAction) return 0;

            if (actionMode != null) return 0;

            if (!TextUtils.isEmpty(searchQuery)) return 0;

            return super.getSwipeDirs(recyclerView, viewHolder);
        }

        @SuppressLint("StaticFieldLeak")
        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            final long threadId = ((ConversationListItem) viewHolder.itemView).getThreadId();
            final boolean read = ((ConversationListItem) viewHolder.itemView).getRead();

            if (direction == ItemTouchHelper.RIGHT) {
                if (isShowingArchive) {
                    new SnackbarAsyncTask<Long>(getView(), getResources().getQuantityString(R.plurals.ConversationListFragment_moved_conversations_to_inbox, 1, 1), getString(R.string.ConversationListFragment_undo), getResources().getColor(R.color.amber_500), Snackbar.LENGTH_LONG, false) {
                        @Override
                        protected void executeAction(@Nullable Long parameter) {
                            DatabaseFactory.getThreadDatabase(requireActivity()).unarchiveConversation(threadId);
                        }

                        @Override
                        protected void reverseAction(@Nullable Long parameter) {
                            DatabaseFactory.getThreadDatabase(requireActivity()).archiveConversation(threadId);
                        }
                    }.execute(threadId);
                } else {
                    new SnackbarAsyncTask<Long>(getView(), getResources().getQuantityString(R.plurals.ConversationListFragment_conversations_archived, 1, 1), getString(R.string.ConversationListFragment_undo), getResources().getColor(R.color.amber_500), Snackbar.LENGTH_LONG, false) {
                        @Override
                        protected void executeAction(@Nullable Long parameter) {
                            DatabaseFactory.getThreadDatabase(requireActivity()).archiveConversation(threadId);

                            if (!read) {
                                DatabaseFactory.getThreadDatabase(requireActivity()).setRead(threadId);
                                MessageNotifier.updateNotification(requireActivity(), masterSecret);
                            }
                        }

                        @Override
                        protected void reverseAction(@Nullable Long parameter) {
                            DatabaseFactory.getThreadDatabase(requireActivity()).unarchiveConversation(threadId);

                            if (!read) {
                                DatabaseFactory.getThreadDatabase(requireActivity()).setUnread(threadId);
                                MessageNotifier.updateNotification(requireActivity(), masterSecret);
                            }
                        }
                    }.execute(threadId);
                }
            } else {
                if (!isShowingArchive) {
                    new SnackbarAsyncTask<Long>(getView(), getResources().getString(R.string.ConversationListFragment_conversation_pinned), getString(R.string.ConversationListFragment_undo), getResources().getColor(R.color.amber_500), Snackbar.LENGTH_LONG, false) {
                        @Override
                        protected void executeAction(@Nullable Long parameter) {
                            getListAdapter().threadDatabase.toggleThreadPin(threadId);
                        }

                        @Override
                        protected void reverseAction(@Nullable Long parameter) {
                            getListAdapter().threadDatabase.toggleThreadPin(threadId);
                        }
                    }.execute(threadId);
                }
            }
        }

        @Override
        public void onChildDraw(
                @NonNull Canvas c, @NonNull RecyclerView recyclerView,
                @NonNull RecyclerView.ViewHolder viewHolder,
                float dX,
                float dY,
                int actionState,
                boolean isCurrentlyActive) {

            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                View itemView = viewHolder.itemView;
                final long threadId = (long) viewHolder.itemView.getTag();
                Paint p = new Paint();
                p.setColor(getResources().getColor(R.color.green_500));

                if (dX > 0) {
                    // Archive/Unarchive
                    final Bitmap icon;
                    if (isShowingArchive) {
                        icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_unarchive_white_36dp);
                    } else {
                        icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_archive_white_36dp);
                    }
                    p.setColor(getResources().getColor(R.color.green_500));
                    c.drawRect((float) itemView.getLeft(), (float) itemView.getTop(), dX, (float) itemView.getBottom(), p);
                    c.drawBitmap(
                            icon,
                            (float) itemView.getLeft() + getResources().getDimension(R.dimen.conversation_list_fragment_archive_padding),
                            (float) itemView.getTop() + ((float) itemView.getBottom() - (float) itemView.getTop() - icon.getHeight()) / 2,
                            p);
                } else if (dX < 0) {
                    // Pin/Unpin
                    final Bitmap icon;
                    if (DatabaseFactory.getThreadDatabase(getContext()).isThreadPinned(threadId)) {
                        icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_forward_white_24dp);
                    } else {
                        icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launch_white_24dp);
                    }
                    p.setColor(getResources().getColor(R.color.blue_500));
                    c.drawRect((float) itemView.getLeft(), (float) itemView.getTop(), dX, (float) itemView.getBottom(), p);
                    c.drawBitmap(
                            icon,
                            (float) itemView.getWidth() - 3 * getResources().getDimension(R.dimen.conversation_list_fragment_archive_padding),
                            (float) itemView.getTop() + ((float) itemView.getBottom() - (float) itemView.getTop() - icon.getHeight()) / 2,
                            p);
                    Log.i(TAG, "onChildDraw: direction= Left | dx= " + dX + " | dy= " + dY);
                }

                float alpha = 1.0f - Math.abs(dX) / (float) viewHolder.itemView.getWidth();
                viewHolder.itemView.setAlpha(alpha);
                viewHolder.itemView.setTranslationX(dX);

            } else {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        }
    }

}
