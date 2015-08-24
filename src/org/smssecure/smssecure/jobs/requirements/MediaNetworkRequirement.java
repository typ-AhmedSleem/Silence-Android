package org.smssecure.smssecure.jobs.requirements;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.util.Log;

import org.smssecure.smssecure.database.DatabaseFactory;
import org.smssecure.smssecure.database.PartDatabase;
import org.smssecure.smssecure.database.PartDatabase.PartId;
import org.smssecure.smssecure.util.MediaUtil;
import org.smssecure.smssecure.util.ServiceUtil;
import org.smssecure.smssecure.util.SMSSecurePreferences;
import org.whispersystems.jobqueue.dependencies.ContextDependent;
import org.whispersystems.jobqueue.requirements.Requirement;

import java.util.Collections;
import java.util.Set;

import ws.com.google.android.mms.pdu.PduPart;

public class MediaNetworkRequirement implements Requirement, ContextDependent {
  private static final long   serialVersionUID = 0L;
  private static final String TAG              = MediaNetworkRequirement.class.getSimpleName();

  private transient Context context;

  private final long messageId;
  private final long partRowId;
  private final long partUniqueId;

  public MediaNetworkRequirement(Context context, long messageId, PartId partId) {
    this.context      = context;
    this.messageId    = messageId;
    this.partRowId    = partId.getRowId();
    this.partUniqueId = partId.getUniqueId();
  }

  @Override public void setContext(Context context) {
    this.context = context;
  }

  private NetworkInfo getNetworkInfo() {
    return ServiceUtil.getConnectivityManager(context).getActiveNetworkInfo();
  }

  public boolean isConnectedWifi() {
    final NetworkInfo info = getNetworkInfo();
    return info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI;
  }

  public boolean isConnectedMobile() {
    final NetworkInfo info = getNetworkInfo();
    return info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_MOBILE;
  }

  public boolean isConnectedRoaming() {
    final NetworkInfo info = getNetworkInfo();
    return info != null && info.isConnected() && info.isRoaming() && info.getType() == ConnectivityManager.TYPE_MOBILE;
  }

  private @NonNull Set<String> getAllowedAutoDownloadTypes() {
    if (isConnectedWifi()) {
      return SMSSecurePreferences.getWifiMediaDownloadAllowed(context);
    } else if (isConnectedRoaming()) {
      return SMSSecurePreferences.getRoamingMediaDownloadAllowed(context);
    } else if (isConnectedMobile()) {
      return SMSSecurePreferences.getMobileMediaDownloadAllowed(context);
    } else {
      return Collections.emptySet();
    }
  }

  @Override
  public boolean isPresent() {
    final PartId       partId = new PartId(partRowId, partUniqueId);
    final PartDatabase db     = DatabaseFactory.getPartDatabase(context);
    final PduPart      part   = db.getPart(partId);
    if (part == null) {
      Log.w(TAG, "part was null");
      return false;
    }

    Log.w(TAG, "part transfer progress is " + part.getTransferProgress());
    switch (part.getTransferProgress()) {
    case PartDatabase.TRANSFER_PROGRESS_STARTED:
      return true;
    case PartDatabase.TRANSFER_PROGRESS_AUTO_PENDING:
      final Set<String> allowedTypes = getAllowedAutoDownloadTypes();
      final boolean     isAllowed    = allowedTypes.contains(MediaUtil.getDiscreteMimeType(part));

      if (isAllowed) db.setTransferState(messageId, partId, PartDatabase.TRANSFER_PROGRESS_STARTED);
      return isAllowed;
    default:
      return false;
    }
  }
}
