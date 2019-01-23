package com.droidlogic.fragment;

import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.os.HwBinder;
import java.util.NoSuchElementException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification;
import vendor.amlogic.hardware.dtvkitserver.V1_0.IDTVKitServer;
import vendor.amlogic.hardware.dtvkitserver.V1_0.IDTVKitServerCallback;
import vendor.amlogic.hardware.dtvkitserver.V1_0.ConnectType;
import vendor.amlogic.hardware.dtvkitserver.V1_0.Result;
import vendor.amlogic.hardware.dtvkitserver.V1_0.DTVKitHidlParcel;


public class DtvkitGlueClient1 {
    private static final String TAG = "DtvkitGlueClient1";

    private static DtvkitGlueClient1 mSingleton = null;
    private ArrayList<SignalHandler> mHandlers = new ArrayList<>();
    // Notification object used to listen to the start of the rpcserver daemon.
    private final ServiceNotification mServiceNotification = new ServiceNotification();
    private static final int DTVKITSERVER_DEATH_COOKIE = 1000;
    private IDTVKitServer mProxy = null;
    private HALCallback mHALCallback;
    // Mutex for all mutable shared state.
    private final Object mLock = new Object();
    final class ServiceNotification extends IServiceNotification.Stub {
        @Override
        public void onRegistration(String fqName, String name, boolean preexisting) {
            Log.i(TAG, "rpcserver HIDL service started " + fqName + " " + name);
            //connectToProxy();
        }
    }

    private void connectToProxy() {
        Log.i(TAG, "connecting to DTVKitServer");
        synchronized (mLock) {
            if (mProxy != null) {
                Log.i(TAG, "connect to DTVKitServer already");
                return;
            }

            try {
                mProxy = IDTVKitServer.getService();
                mProxy.linkToDeath(new DeathRecipient(), DTVKITSERVER_DEATH_COOKIE);
                mProxy.setCallback(mHALCallback, ConnectType.TYPE_EXTEND);
            } catch (NoSuchElementException e) {
                Log.e(TAG, "connectToProxy: DTVKitServer HIDL service not found."
                        + " Did the service fail to start?", e);
            } catch (RemoteException e) {
                Log.e(TAG, "connectToProxy: DTVKitServer HIDL service not responding", e);
            }
        }

        Log.i(TAG, "connect to DTVKitServer HIDL service success");
    }

    private static class HALCallback extends IDTVKitServerCallback.Stub {
        DtvkitGlueClient1 DtvkitClient;
        HALCallback(DtvkitGlueClient1 dkgc) {
            DtvkitClient = dkgc;
    }

    public void notifyCallback(DTVKitHidlParcel parcel) {
            Log.i(TAG, "notifyCallback resource:" + parcel.resource + "json:"+ parcel.json);

            JSONObject object;
            try {
                object = new JSONObject(parcel.json);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                return;
            }

            for (SignalHandler handler : DtvkitClient.mHandlers) {
                handler.onSignal(parcel.resource, object);
            }

        }
    }

    final class DeathRecipient implements HwBinder.DeathRecipient {
        DeathRecipient() {
        }

        @Override
        public void serviceDied(long cookie) {
            if (DTVKITSERVER_DEATH_COOKIE == cookie) {
                Log.e(TAG, "dtvkitserver HIDL service died cookie: " + cookie);
                synchronized (mLock) {
                    mProxy = null;
                }
            }
        }
    }
    interface SignalHandler {
        void onSignal(String signal, JSONObject data);
    }

    protected DtvkitGlueClient1() {
        // Singleton
        mHALCallback = new HALCallback(this);
        connectIfUnconnected();
    }

    public static DtvkitGlueClient1 getInstance() {
        if (mSingleton == null) {
            mSingleton = new DtvkitGlueClient1();
        }
        return mSingleton;
    }

    public void registerSignalHandler(SignalHandler handler) {
        if (!mHandlers.contains(handler)) {
            mHandlers.add(handler);
        }
    }

    public void unregisterSignalHandler(SignalHandler handler) {
        if (mHandlers.contains(handler)) {
            mHandlers.remove(handler);
        }
    }

    public JSONObject request(String resource, JSONArray arguments) throws Exception {
        //mSingleton.connectIfUnconnected();
        try {
            JSONObject object = new JSONObject(mProxy.request(resource, arguments.toString()));
            if (object.getBoolean("accepted")) {
                return object;
            } else {
                throw new Exception(object.getString("data"));
            }
        } catch (JSONException | RemoteException e) {
            throw new Exception(e.getMessage());
        }
    }

    private void connectIfUnconnected() {
        try {
            boolean ret = IServiceManager.getService()
                .registerForNotifications("vendor.amlogic.hardware.dtvkitserver@1.0::IDTVKitServer", "", mServiceNotification);
            if (!ret) {
                Log.e(TAG, "Failed to register service start notification");
            } else {
                Log.d(TAG, "register ok");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to register service start notification", e);
        }
            connectToProxy();
    }
}
