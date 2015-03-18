package org.BastienLQ.SecuredText.sms;

import org.BastienLQ.SecuredText.recipients.Recipient;
import org.BastienLQ.SecuredText.recipients.Recipients;

public class OutgoingEncryptedMessage extends OutgoingTextMessage {

  public OutgoingEncryptedMessage(Recipients recipients, String body) {
    super(recipients, body);
  }

  public OutgoingEncryptedMessage(Recipient recipient, String body) {
    super(recipient, body);
  }

  private OutgoingEncryptedMessage(OutgoingEncryptedMessage base, String body) {
    super(base, body);
  }

  @Override
  public boolean isSecureMessage() {
    return true;
  }

  @Override
  public OutgoingTextMessage withBody(String body) {
    return new OutgoingEncryptedMessage(this, body);
  }
}
