package com.ngs.react.RNTwilioVoice;

import android.util.Log;
import androidx.annotation.Nullable;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.ngs.react.BuildConfig;

import static com.ngs.react.RNTwilioVoice.TwilioVoiceModule.TAG;

public class EventManager {

    public static String TAG = "RNTwilioVoice";
    private ReactApplicationContext mContext;

    public static final String EVENT_PROXIMITY = "proximity";
    public static final String EVENT_WIRED_HEADSET = "wiredHeadset";

    public static final String EVENT_DEVICE_READY = "deviceReady";
    public static final String EVENT_DEVICE_NOT_READY = "deviceNotReady";
    public static final String EVENT_CONNECTION_DID_CONNECT = "connectionDidConnect";
    public static final String EVENT_CONNECTION_DID_DISCONNECT = "connectionDidDisconnect";
    public static final String EVENT_DEVICE_DID_RECEIVE_INCOMING = "deviceDidReceiveIncoming";
    public static final String EVENT_CALL_STATE_RINGING = "callStateRinging";
    public static final String EVENT_CALL_INVITE_CANCELLED = "callInviteCancelled";
    public static final String EVENT_CONNECTION_IS_RECONNECTING = "connectionIsReconnecting";
    public static final String EVENT_CONNECTION_DID_RECONNECT = "connectionDidReconnect";
    public static final String EVENT_CALL_ACCEPTED = "voiceCallAccepted";
    public static final String EVENT_CALL_REJECTED = "voiceCallRejected";
    public static final String EVENT_AUDIO_ROUTE_CHANGED = "audioRouteChanged";
    public static final String EVENT_GO_OFFLINE = "voiceGoOffline";


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
