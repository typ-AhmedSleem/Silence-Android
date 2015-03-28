package org.smssecure.smssecure.mms;

import org.smssecure.smssecure.transport.UndeliverableMessageException;

import ws.com.google.android.mms.pdu.SendConf;

public interface OutgoingMmsConnection {
  SendConf send(byte[] pduBytes) throws UndeliverableMessageException;
}
