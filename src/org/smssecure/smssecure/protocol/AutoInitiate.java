package org.smssecure.smssecure.protocol;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.smssecure.smssecure.ApplicationPreferencesActivity;
import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.crypto.SessionUtil;
import org.smssecure.smssecure.recipients.Recipient;

public class AutoInitiate {

  public static final String WHITESPACE_TAG = "             ";

  public static boolean isTaggable(String message) {
    return message.matches(".*[^\\s].*") &&
           message.replaceAll("\\s+$", "").length() + WHITESPACE_TAG.length() <= 158;
  }

  public static boolean isTagged(String message) {
    return message != null && message.matches(".*[^\\s]" + WHITESPACE_TAG + "$");
  }

  public static String getTaggedMessage(String message) {
    return message.replaceAll("\\s+$", "") + WHITESPACE_TAG;
  }

  public static String stripTag(String message) {
    if (isTagged(message))
      return message.substring(0, message.length() - WHITESPACE_TAG.length());

    return message;
  }

  public static void exemptThread(Context context, long threadId) {
    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
    sp.edit().putBoolean("pref_thread_auto_init_exempt_" + threadId, true).apply();
  }

  public static boolean isValidAutoInitiateSituation(Context context, MasterSecret masterSecret,
                                                     Recipient recipient, String message, long threadId)
  {
    return
        AutoInitiate.isTagged(message)       &&
        isThreadQualified(context, threadId) &&
        isExchangeQualified(context, masterSecret, recipient);
  }

  private static boolean isThreadQualified(Context context, long threadId) {
    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
    return !sp.getBoolean("pref_thread_auto_init_exempt_" + threadId, false);
  }

  private static boolean isExchangeQualified(Context context,
                                             MasterSecret masterSecret,
                                             Recipient recipient)
  {
    return !SessionUtil.hasSession(context, masterSecret, recipient);
  }

}
