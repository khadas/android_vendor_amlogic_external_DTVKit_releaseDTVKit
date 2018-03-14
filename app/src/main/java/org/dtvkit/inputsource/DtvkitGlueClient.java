package org.dtvkit.inputsource;

import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import org.dtvkit.IDTVKit;
import org.dtvkit.ISignalHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class DtvkitGlueClient {
    private static final String TAG = "DtvkitGlueClient";

    private static DtvkitGlueClient mSingleton = null;
    private IDTVKit mDtvkit = null;
    private ArrayList<SignalHandler> mHandlers = new ArrayList<>();

    private final ISignalHandler.Stub mSignalHandler = new ISignalHandler.Stub() {
        public void signal(String signal, String json) {
            JSONObject object;
            try {
                object = new JSONObject(json);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                return;
            }

            for (SignalHandler handler : mHandlers) {
                handler.onSignal(signal, object);
            }
        }
    };

    private final IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            mDtvkit = null;
        }
    };

    interface SignalHandler {
        void onSignal(String signal, JSONObject data);
    }

    protected DtvkitGlueClient() {
        // Singleton
    }

    public static DtvkitGlueClient getInstance() {
        if (mSingleton == null) {
            mSingleton = new DtvkitGlueClient();
        }

        mSingleton.connectIfUnconnected();

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
        mSingleton.connectIfUnconnected();
        try {
            JSONObject object = new JSONObject(mDtvkit.request(resource, arguments.toString()));
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
        if (mDtvkit == null) {
            IBinder service = getService("org.dtvkit.IDTVKit");
            if (service != null) {
                mDtvkit = IDTVKit.Stub.asInterface(service);
                try {
                    mDtvkit.registerSignalHandler(mSignalHandler);
                    mDtvkit.asBinder().linkToDeath(mDeathRecipient, 0);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());}
            } else {
                Log.e(TAG, "Failed to get service");
            }
        }
    }

    private IBinder getService(String name) {
        IBinder service = null;
        try {
            Method getService = Class.forName("android.os.ServiceManager").getMethod("getService", String.class);
            final Object[] args = {name};
            service = (IBinder) getService.invoke(new Object(), args);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return service;
    }
}
