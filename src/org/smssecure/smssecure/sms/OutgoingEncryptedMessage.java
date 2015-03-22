package org.smssecure.smssecure.sms;

import org.smssecure.smssecure.recipients.Recipient;
import org.smssecure.smssecure.recipients.Recipients;

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
