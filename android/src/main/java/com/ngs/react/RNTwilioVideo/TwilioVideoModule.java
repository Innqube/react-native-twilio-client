package com.ngs.react.RNTwilioVideo;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.ngs.react.RNTwilioVoice.SoundPoolManager;

import java.util.HashMap;
import java.util.Map;

import static com.ngs.react.RNTwilioVideo.EventManager.EVENT_CALL_INVITE_CANCELLED;
import static com.ngs.react.RNTwilioVideo.EventManager.EVENT_CONNECTION_DID_CONNECT;
import static com.ngs.react.RNTwilioVideo.EventManager.EVENT_CONNECTION_DID_REJECT;
import static com.ngs.react.RNTwilioVideo.EventManager.EVENT_DEVICE_DID_RECEIVE_INCOMING;
import static com.ngs.react.RNTwilioVideo.VideoConstants.ACTION_ANSWER_CALL;
import static com.ngs.react.RNTwilioVideo.VideoConstants.ACTION_CANCEL_CALL_INVITE;
import static com.ngs.react.RNTwilioVideo.VideoConstants.ACTION_CLEAR_MISSED_CALLS_COUNT;
import static com.ngs.react.RNTwilioVideo.VideoConstants.ACTION_HANGUP_CALL;
import static com.ngs.react.RNTwilioVideo.VideoConstants.ACTION_INCOMING_CALL;
import static com.ngs.react.RNTwilioVideo.VideoConstants.ACTION_MISSED_CALL;
import static com.ngs.react.RNTwilioVideo.VideoConstants.ACTION_REJECT_CALL;
import static com.ngs.react.RNTwilioVideo.VideoConstants.CANCELLED_CALL_INVITE;
import static com.ngs.react.RNTwilioVideo.VideoConstants.INCOMING_CALL_INVITE;
import static com.ngs.react.RNTwilioVideo.VideoConstants.INCOMING_CALL_NOTIFICATION_ID;
import static com.ngs.react.RNTwilioVideo.VideoConstants.INCOMING_NOTIFICATION_PREFIX;
import static com.ngs.react.RNTwilioVideo.VideoConstants.MISSED_CALLS_GROUP;
import static com.ngs.react.RNTwilioVideo.VideoConstants.PREFERENCE_KEY;

public class TwilioVideoModule extends ReactContextBaseJavaModule {

    public static String TAG = "RNTwilioVideo";
    public static Map<String, Integer> callNotificationMap = new HashMap<>();

    private final EventManager eventManager;
    private final CallNotificationManager callNotificationManager;
    private final NotificationManager notificationManager;
    private final VideoBroadcastReceiver videoBroadcastReceiver;

    private VideoCallInvite activeCallInvite;
    private VideoCall activeCall;
    private boolean callAccepted = false;
    private boolean isReceiverRegistered = false;

    public TwilioVideoModule(ReactApplicationContext rc) {
        super(rc);

        eventManager = new EventManager(rc);
        videoBroadcastReceiver = new VideoBroadcastReceiver();
        callNotificationManager = new CallNotificationManager();
        notificationManager = (NotificationManager) rc.getSystemService(Context.NOTIFICATION_SERVICE);

        registerBroadcastReceiver();
    }

    @Override
    public String getName() {
        return TAG;
    }

    private void registerBroadcastReceiver() {
        if (!isReceiverRegistered) {
            Log.d(TAG, "registerBroadcastReceiver");

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_INCOMING_CALL);
            intentFilter.addAction(ACTION_CANCEL_CALL_INVITE);
            intentFilter.addAction(ACTION_MISSED_CALL);

            LocalBroadcastManager
                    .getInstance(getReactApplicationContext())
                    .registerReceiver(videoBroadcastReceiver, intentFilter);

            registerActionReceiver();

            isReceiverRegistered = true;
        }
    }

    private void registerActionReceiver() {
        Log.d(TAG, "registerActionReceiver");

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_ANSWER_CALL);
        intentFilter.addAction(ACTION_REJECT_CALL);
        intentFilter.addAction(ACTION_HANGUP_CALL);
        intentFilter.addAction(ACTION_CLEAR_MISSED_CALLS_COUNT);

        getReactApplicationContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action) {
                    case ACTION_ANSWER_CALL:
                        accept();
                        break;
                    case ACTION_REJECT_CALL:
                        reject();
                        break;
                    case ACTION_CLEAR_MISSED_CALLS_COUNT:
                        SharedPreferences sharedPref = getReactApplicationContext().getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE);
                        SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();
                        sharedPrefEditor.remove(MISSED_CALLS_GROUP);
                        sharedPrefEditor.commit();
                        break;
                }
                notificationManager.cancel(intent.getIntExtra(INCOMING_CALL_NOTIFICATION_ID, 0));
            }
        }, intentFilter);
    }

    @ReactMethod
    public void accept() {
        callAccepted = true;
        SoundPoolManager.getInstance(getReactApplicationContext()).stopRinging();
        if (activeCallInvite != null) {
            Log.d(TAG, "accept()");
            activeCall = new VideoCall(
                    activeCallInvite.getCallSid(),
                    activeCallInvite.getFrom()
            );

            WritableMap params = Arguments.createMap();
            for (Map.Entry<String, String> entry : activeCallInvite.getData().entrySet()) {
                params.putString(entry.getKey(), entry.getValue());
            }
            eventManager.sendEvent(EVENT_CONNECTION_DID_CONNECT, params);
            clearIncomingNotification(activeCallInvite.getCallSid());
        }
    }

    @ReactMethod
    public void reject() {
        callAccepted = false;
        SoundPoolManager.getInstance(getReactApplicationContext()).stopRinging();
        WritableMap params = Arguments.createMap();
        if (activeCallInvite != null) {
            for (Map.Entry<String, String> entry : activeCallInvite.getData().entrySet()) {
                params.putString(entry.getKey(), entry.getValue());
            }
            params.putString("call_state", "DISCONNECTED");
            clearIncomingNotification(activeCallInvite.getCallSid());
        }
        eventManager.sendEvent(EVENT_CONNECTION_DID_REJECT, params);
    }

    @ReactMethod
    public void disconnect() {
        activeCall = null;
    }

    @ReactMethod
    public void getActiveCall(Promise promise) {
        if (activeCall != null) {
            Log.d(TAG, "Active call found = " + activeCall.getCallSid());
            WritableMap params = Arguments.createMap();
            params.putString("call_sid", activeCall.getCallSid());
            params.putString("call_from", activeCall.getFrom());
            promise.resolve(params);
            return;
        }
        promise.resolve(null);
    }

    @ReactMethod
    public void getCallInvite(Promise promise) {
        if (activeCallInvite != null) {
            Log.d(TAG, "Call invite found " + activeCallInvite);
            WritableMap params = Arguments.createMap();
            for (Map.Entry<String, String> entry : activeCallInvite.getData().entrySet()) {
                params.putString(entry.getKey(), entry.getValue());
            }
            promise.resolve(params);
            return;
        }
        promise.resolve(null);
    }

    private class VideoBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "VideoBroadcastReceiver.onReceive " + action + ". Intent " + intent.getExtras());
            if (action.equals(ACTION_INCOMING_CALL)) {
                handleIncomingCallIntent(intent);
            } else if (action.equals(ACTION_CANCEL_CALL_INVITE)) {
                handleCancelledInvite(intent);
            } else if (action.equals(ACTION_MISSED_CALL)) {
                SharedPreferences sharedPref = getReactApplicationContext().getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE);
                SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();
                sharedPrefEditor.remove(MISSED_CALLS_GROUP);
                sharedPrefEditor.commit();
            } else {
                Log.e(TAG, "received broadcast unhandled action " + action);
            }
        }
    }

    private WritableMap buildRNNotification(CallInvite ci) {
        WritableMap params = Arguments.createMap();
        if (ci != null) {
            for (Map.Entry<String, String> entry : activeCallInvite.getData().entrySet()) {
                params.putString(entry.getKey(), entry.getValue());
            }
        }
        return params;
    }

    private void handleCancelledInvite(Intent intent) {
        CanceledVideoCallInvite cancelledCallInvite = intent.getParcelableExtra(CANCELLED_CALL_INVITE);
        clearIncomingNotification(cancelledCallInvite.getCallSid());
        WritableMap params = buildRNNotification(cancelledCallInvite);
        eventManager.sendEvent(EVENT_CALL_INVITE_CANCELLED, params);
        clearIncomingNotification(activeCallInvite.getCallSid());
    }

    private void handleIncomingCallIntent(Intent intent) {
        Log.d(TAG, "handleIncomingCallIntent");
        activeCallInvite = intent.getParcelableExtra(INCOMING_CALL_INVITE);
        if (activeCallInvite != null) {
            callAccepted = false;
            SoundPoolManager.getInstance(getReactApplicationContext()).playRinging();

            if (getReactApplicationContext().getCurrentActivity() != null) {
                Window window = getReactApplicationContext().getCurrentActivity().getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                );
            }
            // send a JS event ONLY if the app's importance is FOREGROUND or SERVICE
            // at startup the app would try to fetch the activeIncoming calls
            int appImportance = callNotificationManager.getApplicationImportance(getReactApplicationContext());
            if (appImportance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND ||
                    appImportance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE) {

                WritableMap params = buildRNNotification(activeCallInvite);
                eventManager.sendEvent(EVENT_DEVICE_DID_RECEIVE_INCOMING, params);
            }
        }
    }

    private void clearIncomingNotification(String callSid) {
        Log.d(TAG, "clearIncomingNotification() callSid: " + callSid);
        // remove incoming call notification
        String notificationKey = INCOMING_NOTIFICATION_PREFIX + callSid;
        int notificationId = 0;
        if (TwilioVideoModule.callNotificationMap.containsKey(notificationKey)) {
            notificationId = TwilioVideoModule.callNotificationMap.get(notificationKey);
        }
        callNotificationManager.removeIncomingCallNotification(getReactApplicationContext(), null, notificationId);
        TwilioVideoModule.callNotificationMap.remove(notificationKey);
        activeCallInvite = null;
    }

}
