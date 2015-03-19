package org.SecuredText.SecuredText.sms;

import org.SecuredText.SecuredText.recipients.Recipient;

public class OutgoingKeyExchangeMessage extends OutgoingTextMessage {

  public OutgoingKeyExchangeMessage(Recipient recipient, String message) {
    super(recipient, message);
  }

  private OutgoingKeyExchangeMessage(OutgoingKeyExchangeMessage base, String body) {
    super(base, body);
  }

  @Override
  public boolean isKeyExchange() {
    return true;
  }

  @Override
  public OutgoingTextMessage withBody(String body) {
    return new OutgoingKeyExchangeMessage(this, body);
  }
}
