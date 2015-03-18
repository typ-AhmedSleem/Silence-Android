package org.BastienLQ.SecuredText.util;

import android.content.Context;
import android.content.pm.PackageManager;

import java.io.IOException;

public class VersionTracker {


  public static int getLastSeenVersion(Context context) {
    return SecuredTextPreferences.getLastVersionCode(context);
  }

  public static void updateLastSeenVersion(Context context) {
    try {
      int currentVersionCode = Util.getCurrentApkReleaseVersion(context);
      SecuredTextPreferences.setLastVersionCode(context, currentVersionCode);
    } catch (IOException ioe) {
      throw new AssertionError(ioe);
    }
  }
}
