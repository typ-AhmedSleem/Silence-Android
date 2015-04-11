package org.smssecure.smssecure;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import org.smssecure.smssecure.database.DatabaseFactory;
import org.smssecure.smssecure.recipients.RecipientFactory;
import org.smssecure.smssecure.recipients.Recipients;

public class SmsSendtoActivity extends Activity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    startActivity(getNextIntent(getIntent()));
    finish();
    super.onCreate(savedInstanceState);
  }

  private Intent getNextIntent(Intent original) {
    String     body       = original.getStringExtra("sms_body");
    String     data       = original.getData().getSchemeSpecificPart();
    Recipients recipients = RecipientFactory.getRecipientsFromString(this, data, false);
    long       threadId   = DatabaseFactory.getThreadDatabase(this).getThreadIdIfExistsFor(recipients);

    final Intent nextIntent;
    if (recipients == null || recipients.isEmpty()) {
      nextIntent = new Intent(this, NewConversationActivity.class);
      nextIntent.putExtra(ConversationActivity.DRAFT_TEXT_EXTRA, body);
      Toast.makeText(this, R.string.ConversationActivity_specify_recipient, Toast.LENGTH_LONG).show();
    } else {
      nextIntent = new Intent(this, ConversationActivity.class);
      nextIntent.putExtra(ConversationActivity.DRAFT_TEXT_EXTRA, body);
      nextIntent.putExtra(ConversationActivity.THREAD_ID_EXTRA, threadId);
      nextIntent.putExtra(ConversationActivity.RECIPIENTS_EXTRA, recipients.getIds());
    }
    return nextIntent;
  }
}
