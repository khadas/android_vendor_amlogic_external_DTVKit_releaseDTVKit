package org.dtvkit.inputsource;

import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.os.HwBinder;
import java.util.NoSuchElementException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.util.ArrayList;

public class DtvkitGlueClient {
    private static final String TAG = "DtvkitGlueClient";

    private static DtvkitGlueClient mSingleton = null;
    private ArrayList<SignalHandler> mHandlers = new ArrayList<>();
    // Notification object used to listen to the start of the rpcserver daemon.
    //private final ServiceNotification mServiceNotification = new ServiceNotification();
    //private static final int DTVKITSERVER_DEATH_COOKIE = 1000;
   // private IDTVKitServer mProxy = null;
    //private HALCallback mHALCallback;
    private OverlayTarget mTarget;
    // Mutex for all mutable shared state.
    private final Object mLock = new Object();
    private native void nativeconnectdtvkit(DtvkitGlueClient client);
    private native void nativedisconnectdtvkit();
    private native void nativeSetSurface(Surface surface);
    private native String nativerequest(String resource, String json);

    static {
        System.loadLibrary("dtvkit_jni");
    }

        //native callback
    public void notifySubtitleCallback(int width, int height, int dstx, int dsty, int dstwidth, int dstheight, byte[] data)
    {
         Log.d(TAG, "notifySubtitleCallBack received!!! width = " + width + ", heigth = " + height);
         if (mTarget != null) {
            mTarget.draw(width, height, dstx, dsty, dstwidth, dstheight, data);
         }
    }

    public void notifyDvbCallback(String resource, String json) {
        Log.i(TAG, "notifyDvbCallback received!!! resource" + resource + ",json" + json);
        JSONObject object;
        try {
            object = new JSONObject(json);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return;
        }

        for (SignalHandler handler :mHandlers) {
            handler.onSignal(resource, object);
        }
    }

   public void setDisplay(Surface sh) {
        nativeSetSurface(sh);
   }

   public void disConnectDtvkitClient() {
        nativedisconnectdtvkit();
   }


/*
    final class ServiceNotification extends IServiceNotification.Stub {
        @Override
        public void onRegistration(String fqName, String name, boolean preexisting) {
            Log.i(TAG, "rpcserver HIDL service started " + fqName + " " + name);
            connectToProxy();
        }
    }

    private void connectToProxy() {
        synchronized (mLock) {
            if (mProxy != null) {
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
        DtvkitGlueClient DtvkitClient;
        HALCallback(DtvkitGlueClient dkgc) {
            DtvkitClient = dkgc;
    }

    public void notifyCallback(DTVKitHidlParcel parcel) {
        Log.i(TAG, "resource" + parcel.resource + ",json" + parcel.json);
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
*/
    interface SignalHandler {
        void onSignal(String signal, JSONObject data);
    }

    interface OverlayTarget {
        void draw(int src_width, int src_height, int dst_x, int dst_y, int dst_width, int dst_height, byte[] data);
    }

    protected DtvkitGlueClient() {
        // Singleton
        nativeconnectdtvkit(this);
    }

    public static DtvkitGlueClient getInstance() {
        if (mSingleton == null) {
            mSingleton = new DtvkitGlueClient();
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

    public void setOverlayTarget(OverlayTarget target) {
        mTarget = target;
    }

    public JSONObject request(String resource, JSONArray arguments) throws Exception {
        //mSingleton.connectIfUnconnected();
        try {
            JSONObject object = new JSONObject(nativerequest(resource, arguments.toString()));
            if (object.getBoolean("accepted")) {
                return object;
            } else {
                throw new Exception(object.getString("data"));
            }
        } catch (JSONException | RemoteException e) {
            throw new Exception(e.getMessage());
        }
    }
}
