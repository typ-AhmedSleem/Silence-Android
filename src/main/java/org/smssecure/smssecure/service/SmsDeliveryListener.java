package org.smssecure.smssecure.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsMessage;
import android.util.Log;

import org.smssecure.smssecure.ApplicationContext;
import org.smssecure.smssecure.jobs.SmsSentJob;
import org.whispersystems.jobqueue.JobManager;

public class SmsDeliveryListener extends BroadcastReceiver {

    public static final String SENT_SMS_ACTION = "org.smssecure.smssecure.SendReceiveService.SENT_SMS_ACTION";
    public static final String DELIVERED_SMS_ACTION = "org.smssecure.smssecure.SendReceiveService.DELIVERED_SMS_ACTION";
    private static final String TAG = SmsDeliveryListener.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        JobManager jobManager = ApplicationContext.getInstance(context).getJobManager();
        long messageId = intent.getLongExtra("message_id", -1);

        switch (intent.getAction()) {
            case SENT_SMS_ACTION:
                int result = getResultCode();

                jobManager.add(new SmsSentJob(context, messageId, SENT_SMS_ACTION, result));
                break;
            case DELIVERED_SMS_ACTION:
                byte[] pdu = intent.getByteArrayExtra("pdu");

                if (pdu == null) {
                    Log.w(TAG, "No PDU in delivery receipt!");
                    break;
                }

                SmsMessage message = SmsMessage.createFromPdu(pdu);

                if (message == null) {
                    Log.w(TAG, "Delivery receipt failed to parse!");
                    break;
                }

                jobManager.add(new SmsSentJob(context, messageId, DELIVERED_SMS_ACTION, message.getStatus()));
                break;
            default:
                Log.w(TAG, "Unknown action: " + intent.getAction());
        }
    }
}
