package org.smssecure.smssecure.mms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.PowerManager;
import android.util.Log;

import org.smssecure.smssecure.util.Util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MmsRadio {

    public static final int TYPE_MOBILE_MMS = 2;
    private static final String TAG = MmsRadio.class.getSimpleName();
    private static final String FEATURE_ENABLE_MMS = "enableMMS";

    ///
    private static final int APN_ALREADY_ACTIVE = 0;
    private static MmsRadio instance;
    private final Context context;
    private final ConnectivityManager connectivityManager;
    private ConnectivityListener connectivityListener;
    private final PowerManager.WakeLock wakeLock;
    private int connectedCounter = 0;
    private MmsRadio(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        this.context = context;
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "silence:mms");
        this.wakeLock.setReferenceCounted(true);
    }

    public static synchronized MmsRadio getInstance(Context context) {
        if (instance == null)
            instance = new MmsRadio(context.getApplicationContext());

        return instance;
    }

    public synchronized void disconnect() {
        Log.w(TAG, "MMS Radio Disconnect Called...");
        wakeLock.release();
        connectedCounter--;

        Log.w(TAG, "Reference count: " + connectedCounter);

        if (connectedCounter == 0) {
            Log.w(TAG, "Turning off MMS radio...");
            try {
                final Method stopUsingNetworkFeatureMethod = connectivityManager.getClass().getMethod("stopUsingNetworkFeature", Integer.TYPE, String.class);
                stopUsingNetworkFeatureMethod.invoke(connectivityManager, ConnectivityManager.TYPE_MOBILE, FEATURE_ENABLE_MMS);
            } catch (NoSuchMethodException nsme) {
                Log.w(TAG, nsme);
            } catch (IllegalAccessException iae) {
                Log.w(TAG, iae);
            } catch (InvocationTargetException ite) {
                Log.w(TAG, ite);
            }

            if (connectivityListener != null) {
                Log.w(TAG, "Unregistering receiver...");
                context.unregisterReceiver(connectivityListener);
                connectivityListener = null;
            }
        }
    }

    public synchronized void connect() throws MmsRadioException {
        int status;

        try {
            final Method startUsingNetworkFeatureMethod = connectivityManager.getClass().getMethod("startUsingNetworkFeature", Integer.TYPE, String.class);
            status = (int) startUsingNetworkFeatureMethod.invoke(connectivityManager, ConnectivityManager.TYPE_MOBILE, FEATURE_ENABLE_MMS);
        } catch (NoSuchMethodException nsme) {
            throw new MmsRadioException(nsme);
        } catch (IllegalAccessException iae) {
            throw new MmsRadioException(iae);
        } catch (InvocationTargetException ite) {
            throw new MmsRadioException(ite);
        }

        Log.w(TAG, "startUsingNetworkFeature status: " + status);

        if (status == APN_ALREADY_ACTIVE) {
            wakeLock.acquire();
            connectedCounter++;
        } else {
            wakeLock.acquire();
            connectedCounter++;

            if (connectivityListener == null) {
                IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
                connectivityListener = new ConnectivityListener();
                context.registerReceiver(connectivityListener, filter);
            }

            Util.wait(this, 30000);

            if (!isConnected()) {
                Log.w(TAG, "Got back from connectivity wait, and not connected...");
                disconnect();
                throw new MmsRadioException("Unable to successfully enable MMS radio.");
            }
        }
    }

    private boolean isConnected() {
        NetworkInfo info = connectivityManager.getNetworkInfo(TYPE_MOBILE_MMS);

        Log.w(TAG, "Connected: " + info);

        return (info != null) && (info.getType() == TYPE_MOBILE_MMS) && info.isConnected();
    }

    private boolean isConnectivityPossible() {
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(TYPE_MOBILE_MMS);

        return networkInfo != null && networkInfo.isAvailable();
    }

    private boolean isConnectivityFailure() {
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(TYPE_MOBILE_MMS);

        return networkInfo == null || networkInfo.getDetailedState() == NetworkInfo.DetailedState.FAILED;
    }

    private synchronized void issueConnectivityChange() {
        if (isConnected()) {
            Log.w(TAG, "Notifying connected...");
            notifyAll();
            return;
        }

        if (!isConnected() && (isConnectivityFailure() || !isConnectivityPossible())) {
            Log.w(TAG, "Notifying not connected...");
            notifyAll();
        }
    }

    private class ConnectivityListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.w(TAG, "Got connectivity change...");
            issueConnectivityChange();
        }
    }


}
