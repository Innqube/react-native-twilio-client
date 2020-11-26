package com.ngs.react.RNTwilioVideo;

import android.util.Log;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.ngs.react.BuildConfig;

public class EventManager {

    private static final String TAG = "RNTwilioVideo";
    private final ReactApplicationContext mContext;

    public static final String EVENT_CONNECTION_DID_CONNECT = "videoConnectionDidConnect";
    public static final String EVENT_CONNECTION_DID_REJECT = "videoConnectionDidReject";
    public static final String EVENT_DEVICE_DID_RECEIVE_INCOMING = "videoDeviceDidReceiveIncoming";
    public static final String EVENT_CALL_INVITE_CANCELLED = "videoCallInviteCancelled";

    public EventManager(ReactApplicationContext context) {
        mContext = context;
    }

    public void sendEvent(String eventName, @Nullable WritableMap params) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "sendEvent "+eventName+" params "+params);
        }
        if (mContext.hasActiveCatalystInstance()) {
            mContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
        } else {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "failed Catalyst instance not active");
            }
        }
    }
}
