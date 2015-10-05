package org.smssecure.smssecure.jobs.requirements;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.smssecure.smssecure.util.TelephonyUtil;
import org.smssecure.smssecure.util.SMSSecurePreferences;
import org.whispersystems.jobqueue.dependencies.ContextDependent;
import org.whispersystems.jobqueue.requirements.Requirement;

public class MediaNetworkRequirement implements Requirement, ContextDependent {
  private static final String TAG = MediaNetworkRequirement.class.getSimpleName();

  private transient Context context;
  private final     long    messageId;
  private final     boolean automatic;

  public MediaNetworkRequirement(Context context, long messageId, boolean automatic) {
    this.context   = context;
    this.messageId = messageId;
    this.automatic = automatic;
  }

  @Override public void setContext(Context context) {
    this.context = context;
  }

  @Override
  public boolean isPresent() {
    if (!automatic){
      return true;
    } else if (TelephonyUtil.isConnectedRoaming(context)) {
      return SMSSecurePreferences.isMediaDownloadAllowed(context) &&
             SMSSecurePreferences.isRoamingMediaDownloadAllowed(context);
    } else {
      return SMSSecurePreferences.isMediaDownloadAllowed(context);
    }
  }
}
