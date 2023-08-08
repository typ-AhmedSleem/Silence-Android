package org.smssecure.smssecure;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.database.EncryptedBackupExporter;
import org.smssecure.smssecure.database.NoExternalStorageException;
import org.smssecure.smssecure.database.PlaintextBackupExporter;
import org.smssecure.smssecure.database.PlaintextBackupImporter;
import org.smssecure.smssecure.permissions.Permissions;
import org.smssecure.smssecure.service.ApplicationMigrationService;

import java.io.IOException;


public class ImportExportFragment extends Fragment {

    @SuppressWarnings("unused")
    private static final String TAG = ImportExportFragment.class.getSimpleName();

    private static final int SUCCESS = 0;
    private static final int NO_SD_CARD = 1;
    private static final int ERROR_IO = 2;

    private MasterSecret masterSecret;
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.masterSecret = getArguments().getParcelable("master_secret");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        View layout = inflater.inflate(R.layout.import_export_fragment, container, false);
        View importSmsView = layout.findViewById(R.id.import_sms);
        View importEncryptedView = layout.findViewById(R.id.import_encrypted_backup);
        View importPlaintextView = layout.findViewById(R.id.import_plaintext_backup);
        View exportEncryptedView = layout.findViewById(R.id.export_encrypted_backup);
        View exportPlaintextView = layout.findViewById(R.id.export_plaintext_backup);

        importSmsView.setOnClickListener(v -> handleImportSms());
        importEncryptedView.setOnClickListener(v -> handleImportEncryptedBackup());
        importPlaintextView.setOnClickListener(v -> handleImportPlaintextBackup());
        exportEncryptedView.setOnClickListener(v -> handleExportEncryptedBackup());
        exportPlaintextView.setOnClickListener(v -> handleExportPlaintextBackup());

        return layout;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @SuppressWarnings("CodeBlock2Expr")
    private void handleImportSms() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIconAttribute(R.attr.dialog_info_icon);
        builder.setTitle(getActivity().getString(R.string.ImportFragment_import_system_sms_database));
        builder.setMessage(getActivity().getString(R.string.ImportFragment_this_will_import_messages_from_the_system));
        builder.setPositiveButton(getActivity().getString(R.string.ImportFragment_import), (dialog, which) -> {
            Permissions.with(this)
                    .request(Manifest.permission.READ_SMS)
                    .ifNecessary()
                    .withPermanentDenialDialog(getString(R.string.ImportExportFragment_silence_needs_the_sms_permission_in_order_to_import_sms_messages))
                    .onAllGranted(() -> {
                        Intent intent = new Intent(getActivity(), ApplicationMigrationService.class);
                        intent.setAction(ApplicationMigrationService.MIGRATE_DATABASE);
                        intent.putExtra("master_secret", masterSecret);
                        getActivity().startService(intent);

                        Intent nextIntent = new Intent(getActivity(), ConversationListActivity.class);

                        Intent activityIntent = new Intent(getActivity(), DatabaseMigrationActivity.class);
                        activityIntent.putExtra("next_intent", nextIntent);
                        getActivity().startActivity(activityIntent);
                    })
                    .onAnyDenied(() -> Toast.makeText(getContext(), R.string.ImportExportFragment_silence_needs_the_sms_permission_in_order_to_import_sms_messages_toast, Toast.LENGTH_LONG).show())
                    .execute();
        });
        builder.setNegativeButton(getActivity().getString(R.string.ImportFragment_cancel), null);
        builder.show();
    }

    @SuppressWarnings("CodeBlock2Expr")
    @SuppressLint("InlinedApi")
    private void handleImportEncryptedBackup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIconAttribute(R.attr.dialog_alert_icon);
        builder.setTitle(getActivity().getString(R.string.ImportFragment_restore_encrypted_backup));
        builder.setMessage(getActivity().getString(R.string.ImportFragment_restoring_an_encrypted_backup_will_completely_replace_your_existing_keys));
        builder.setPositiveButton(getActivity().getString(R.string.ImportFragment_import), (dialog, which) -> {
            Permissions.with(this)
                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                    .ifNecessary()
                    .withPermanentDenialDialog(getString(R.string.ImportExportFragment_silence_needs_the_storage_permission_in_order_to_read_from_external_storage_but_it_has_been_permanently_denied))
                    .onAllGranted(() -> new ImportEncryptedBackupTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR))
                    .onAnyDenied(() -> Toast.makeText(getContext(), R.string.ImportExportFragment_silence_needs_the_storage_permission_in_order_to_read_from_external_storage, Toast.LENGTH_LONG).show())
                    .execute();
        });
        builder.setNegativeButton(getActivity().getString(R.string.ImportFragment_cancel), null);
        builder.show();
    }

    @SuppressWarnings("CodeBlock2Expr")
    @SuppressLint("InlinedApi")
    private void handleImportPlaintextBackup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIconAttribute(R.attr.dialog_alert_icon);
        builder.setTitle(getActivity().getString(R.string.ImportFragment_import_plaintext_backup));
        builder.setMessage(getActivity().getString(R.string.ImportFragment_this_will_import_messages_from_a_plaintext_backup));
        builder.setPositiveButton(getActivity().getString(R.string.ImportFragment_import), (dialog, which) -> {
            Permissions.with(ImportExportFragment.this)
                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                    .ifNecessary()
                    .withPermanentDenialDialog(getString(R.string.ImportExportFragment_silence_needs_the_storage_permission_in_order_to_read_from_external_storage_but_it_has_been_permanently_denied))
                    .onAllGranted(() -> new ImportPlaintextBackupTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR))
                    .onAnyDenied(() -> Toast.makeText(getContext(), R.string.ImportExportFragment_silence_needs_the_storage_permission_in_order_to_read_from_external_storage, Toast.LENGTH_LONG).show())
                    .execute();
        });
        builder.setNegativeButton(getActivity().getString(R.string.ImportFragment_cancel), null);
        builder.show();
    }

    private void handleExportEncryptedBackup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIconAttribute(R.attr.dialog_info_icon);
        builder.setTitle(getActivity().getString(R.string.ExportFragment_export_encrypted_backup));
        builder.setMessage(getActivity().getString(R.string.ExportFragment_this_will_export_your_encrypted_keys_settings_and_messages));
        builder.setPositiveButton(getActivity().getString(R.string.ExportFragment_export), (dialog, which) -> {
            Permissions.with(ImportExportFragment.this)
                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                    .ifNecessary()
                    .withPermanentDenialDialog(getString(R.string.ImportExportFragment_silence_needs_the_storage_permission_in_order_to_write_to_external_storage_but_it_has_been_permanently_denied))
                    .onAllGranted(() -> new ExportEncryptedBackupTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR))
                    .onAnyDenied(() -> Toast.makeText(getContext(), R.string.ImportExportFragment_silence_needs_the_storage_permission_in_order_to_write_to_external_storage, Toast.LENGTH_LONG).show())
                    .execute();
        });
        builder.setNegativeButton(getActivity().getString(R.string.ExportFragment_cancel), null);
        builder.show();
    }

    @SuppressWarnings("CodeBlock2Expr")
    @SuppressLint("InlinedApi")
    private void handleExportPlaintextBackup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIconAttribute(R.attr.dialog_alert_icon);
        builder.setTitle(getActivity().getString(R.string.ExportFragment_export_plaintext_to_storage));
        builder.setMessage(getActivity().getString(R.string.ExportFragment_warning_this_will_export_the_contents_of_your_messages_to_storage_in_plaintext));
        builder.setPositiveButton(getActivity().getString(R.string.ExportFragment_export), (dialog, which) -> {
            Permissions.with(ImportExportFragment.this)
                    .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                    .ifNecessary()
                    .withPermanentDenialDialog(getString(R.string.ImportExportFragment_silence_needs_the_storage_permission_in_order_to_write_to_external_storage_but_it_has_been_permanently_denied))
                    .onAllGranted(() -> new ExportPlaintextTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR))
                    .onAnyDenied(() -> Toast.makeText(getContext(), R.string.ImportExportFragment_silence_needs_the_storage_permission_in_order_to_write_to_external_storage, Toast.LENGTH_LONG).show())
                    .execute();
        });
        builder.setNegativeButton(getActivity().getString(R.string.ExportFragment_cancel), null);
        builder.show();
    }

    @SuppressLint("StaticFieldLeak")
    private class ImportPlaintextBackupTask extends AsyncTask<Void, Void, Integer> {

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(getActivity(),
                    getActivity().getString(R.string.ImportFragment_importing),
                    getActivity().getString(R.string.ImportFragment_import_plaintext_backup_elipse),
                    true, false);
        }

        protected void onPostExecute(Integer result) {
            Context context = getActivity();

            if (progressDialog != null)
                progressDialog.dismiss();

            if (context == null)
                return;

            switch (result) {
                case NO_SD_CARD:
                    Toast.makeText(context,
                            context.getString(R.string.ImportFragment_no_plaintext_backup_found),
                            Toast.LENGTH_LONG).show();
                    break;
                case ERROR_IO:
                    Toast.makeText(context,
                            context.getString(R.string.ImportFragment_error_importing_backup),
                            Toast.LENGTH_LONG).show();
                    break;
                case SUCCESS:
                    Toast.makeText(context,
                            context.getString(R.string.ImportFragment_import_complete),
                            Toast.LENGTH_LONG).show();
                    break;
            }
        }

        @Override
        protected Integer doInBackground(Void... params) {
            try {
                PlaintextBackupImporter.importPlaintextFromSd(getActivity(), masterSecret);
                return SUCCESS;
            } catch (NoExternalStorageException e) {
                Log.w("ImportFragment", e);
                return NO_SD_CARD;
            } catch (IOException e) {
                Log.w("ImportFragment", e);
                return ERROR_IO;
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class ExportPlaintextTask extends AsyncTask<Void, Void, Integer> {
        private ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            dialog = ProgressDialog.show(getActivity(),
                    getActivity().getString(R.string.ExportFragment_exporting),
                    getActivity().getString(R.string.ExportFragment_exporting_plaintext_to_storage),
                    true, false);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            try {
                PlaintextBackupExporter.exportPlaintextToSd(getActivity(), masterSecret);
                return SUCCESS;
            } catch (NoExternalStorageException e) {
                Log.w("ExportFragment", e);
                return NO_SD_CARD;
            } catch (IOException e) {
                Log.w("ExportFragment", e);
                return ERROR_IO;
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            Context context = getActivity();

            if (dialog != null)
                dialog.dismiss();

            if (context == null)
                return;

            switch (result) {
                case NO_SD_CARD:
                    Toast.makeText(context,
                            context.getString(R.string.ExportFragment_error_unable_to_write_to_storage),
                            Toast.LENGTH_LONG).show();
                    break;
                case ERROR_IO:
                    Toast.makeText(context,
                            context.getString(R.string.ExportFragment_error_while_writing_to_storage),
                            Toast.LENGTH_LONG).show();
                    break;
                case SUCCESS:
                    Toast.makeText(context,
                            context.getString(R.string.ExportFragment_export_successful),
                            Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class ImportEncryptedBackupTask extends AsyncTask<Void, Void, Integer> {

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(getActivity(),
                    getActivity().getString(R.string.ImportFragment_importing),
                    getActivity().getString(R.string.ImportFragment_restoring_encrypted_backup),
                    true, false);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            try {
                EncryptedBackupExporter.importFromStorage(getActivity());
                return SUCCESS;
            } catch (NoExternalStorageException e) {
                Log.w("ImportFragment", e);
                return NO_SD_CARD;
            } catch (IOException e) {
                Log.w("ImportFragment", e);
                return ERROR_IO;
            }
        }

        protected void onPostExecute(Integer result) {
            Context context = getActivity();

            if (progressDialog != null)
                progressDialog.dismiss();

            if (context == null)
                return;

            switch (result) {
                case NO_SD_CARD:
                    Toast.makeText(context,
                            context.getString(R.string.ImportFragment_no_encrypted_backup_found),
                            Toast.LENGTH_LONG).show();
                    break;
                case ERROR_IO:
                    Toast.makeText(context,
                            context.getString(R.string.ImportFragment_error_importing_backup),
                            Toast.LENGTH_LONG).show();
                    break;
                case SUCCESS:
                    ExitActivity.exitAndRemoveFromRecentApps(getActivity());
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class ExportEncryptedBackupTask extends AsyncTask<Void, Void, Integer> {
        private ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            dialog = ProgressDialog.show(getActivity(),
                    getActivity().getString(R.string.ExportFragment_exporting),
                    getActivity().getString(R.string.ExportFragment_exporting_keys_settings_and_messages),
                    true, false);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            try {
                EncryptedBackupExporter.exportToStorage(getActivity());
                return SUCCESS;
            } catch (NoExternalStorageException e) {
                Log.w("ExportFragment", e);
                return NO_SD_CARD;
            } catch (IOException e) {
                Log.w("ExportFragment", e);
                return ERROR_IO;
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            Context context = getActivity();

            if (dialog != null) dialog.dismiss();

            if (context == null) return;

            switch (result) {
                case NO_SD_CARD:
                    Toast.makeText(context,
                            context.getString(R.string.ExportFragment_error_unable_to_write_to_storage),
                            Toast.LENGTH_LONG).show();
                    break;
                case ERROR_IO:
                    Toast.makeText(context,
                            context.getString(R.string.ExportFragment_error_while_writing_to_storage),
                            Toast.LENGTH_LONG).show();
                    break;
                case SUCCESS:
                    Toast.makeText(context,
                            context.getString(R.string.ExportFragment_export_successful),
                            Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

}
