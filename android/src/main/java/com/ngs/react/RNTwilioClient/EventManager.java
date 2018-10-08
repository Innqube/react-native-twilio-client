package com.ngs.react.RNTwilioClient;

import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import static com.ngs.react.RNTwilioClient.TwilioClientModule.TAG;

public class EventManager {

    private ReactApplicationContext mContext;

    public static final String EVENT_PROXIMITY = "proximity";
    public static final String EVENT_WIRED_HEADSET = "wiredHeadset";

    public static final String EVENT_DEVICE_READY = "deviceReady";
    public static final String EVENT_DEVICE_NOT_READY = "deviceNotReady";
    public static final String EVENT_CONNECTION_DID_CONNECT = "connectionDidConnect";
    public static final String EVENT_CONNECTION_DID_DISCONNECT = "connectionDidDisconnect";
    public static final String EVENT_DEVICE_DID_RECEIVE_INCOMING = "deviceDidReceiveIncoming";

    public static final String EVENT_PERFORM_ANSWER_VOICE_CALL = "performAnswerVoiceCall";
    public static final String EVENT_PERFORM_ANSWER_VIDEO_CALL = "performAnswerVideoCall";
    public static final String EVENT_PERFORM_END_VIDEO_CALL = "performEndVideoCall";
    public static final String EVENT_REQUEST_TRANSACTION_ERROR = "requestTransactionError";
    public static final String EVENT_VOIP_REMOTE_NOTIFICATION_REGISTERED = "voipRemoteNotificationsRegistered";
    public static final String EVENT_VOIP_REMOTE_NOTIFICATION_RECEIVED = "voipRemoteNotificationReceived";

    public EventManager(ReactApplicationContext context) {
        mContext = context;
    }

    public void sendEvent(String eventName) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "sendEvent "+eventName);
        }
        if (mContext.hasActiveCatalystInstance()) {
            mContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(eventName, null);
        } else {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "failed Catalyst instance not active");
            }
        }
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

    public void sendEvent(String eventName, @Nullable String param) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "sendEvent "+eventName+" params "+param);
        }
        if (mContext.hasActiveCatalystInstance()) {
            mContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(eventName, param);
        } else {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "failed Catalyst instance not active");
            }
        }
    }
}
