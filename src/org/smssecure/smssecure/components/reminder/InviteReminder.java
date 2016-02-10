package org.smssecure.smssecure.components.reminder;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.View.OnClickListener;

import org.smssecure.smssecure.R;
import org.smssecure.smssecure.database.DatabaseFactory;
import org.smssecure.smssecure.recipients.Recipients;

public class InviteReminder extends Reminder {

  public InviteReminder(final @NonNull Context context,
                        final @NonNull Recipients recipients)
  {
    super(context.getString(R.string.reminder_header_invite_title),
          context.getString(R.string.reminder_header_invite_text, recipients.toShortString()),
          context.getString(R.string.reminder_header_invite_button));

    setDismissListener(new OnClickListener() {
      @Override public void onClick(View v) {
        new AsyncTask<Void,Void,Void>() {

          @Override protected Void doInBackground(Void... params) {
            DatabaseFactory.getRecipientPreferenceDatabase(context).setSeenInviteReminder(recipients, true);
            return null;
          }
        }.execute();
      }
    });
  }
}
