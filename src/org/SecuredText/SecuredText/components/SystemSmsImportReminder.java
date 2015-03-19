package org.SecuredText.SecuredText.components;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;

import org.SecuredText.SecuredText.ConversationListActivity;
import org.SecuredText.SecuredText.DatabaseMigrationActivity;
import org.SecuredText.SecuredText.R;
import org.SecuredText.SecuredText.service.ApplicationMigrationService;
import org.SecuredText.SecuredText.crypto.MasterSecret;

public class SystemSmsImportReminder extends Reminder {

  public SystemSmsImportReminder(final Context context, final MasterSecret masterSecret) {
    super(R.drawable.sms_system_import_icon,
          R.string.reminder_header_sms_import_title,
          R.string.reminder_header_sms_import_text);

    final OnClickListener okListener = new OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent intent = new Intent(context, ApplicationMigrationService.class);
        intent.setAction(ApplicationMigrationService.MIGRATE_DATABASE);
        intent.putExtra("master_secret", masterSecret);
        context.startService(intent);

        Intent nextIntent = new Intent(context, ConversationListActivity.class);
        intent.putExtra("master_secret", masterSecret);

        Intent activityIntent = new Intent(context, DatabaseMigrationActivity.class);
        activityIntent.putExtra("master_secret", masterSecret);
        activityIntent.putExtra("next_intent", nextIntent);
        context.startActivity(activityIntent);
      }
    };
    final OnClickListener cancelListener = new OnClickListener() {
      @Override
      public void onClick(View v) {
        ApplicationMigrationService.setDatabaseImported(context);
      }
    };
    setOkListener(okListener);
    setCancelListener(cancelListener);
  }

  public static boolean isEligible(Context context) {
    return !ApplicationMigrationService.isDatabaseImported(context);
  }
}
