package org.smssecure.smssecure.mms;

import android.support.annotation.NonNull;

import org.smssecure.smssecure.transport.UndeliverableMessageException;

import ws.com.google.android.mms.pdu.SendConf;

public interface OutgoingMmsConnection {
  SendConf send(@NonNull byte[] pduBytes) throws UndeliverableMessageException;
}
