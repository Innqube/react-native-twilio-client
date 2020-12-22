package com.ngs.react.RNTwilioVideo;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.*;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import com.facebook.react.bridge.*;

import java.util.Map;

import static com.ngs.react.RNTwilioVideo.EventManager.*;
import static com.ngs.react.RNTwilioVideo.VideoConstants.*;

public class TwilioVideoModule extends ReactContextBaseJavaModule {

    public static String TAG = "RNTwilioVideo";

    private final EventManager eventManager;
    private final CallNotificationManager callNotificationManager;
    private final NotificationManager notificationManager;
    private final VideoBroadcastReceiver videoBroadcastReceiver;

    private VideoCall activeCall;
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
                        VideoCallInvite invite = intent.getParcelableExtra(INCOMING_CALL_INVITE);
                        Integer answerNotificationId = intent.getIntExtra(INCOMING_CALL_NOTIFICATION_ID, -1);
                        internalAccept(invite, answerNotificationId);
                        break;
                    case ACTION_REJECT_CALL:
                        VideoCallInvite rejectInvite = intent.getParcelableExtra(INCOMING_CALL_INVITE);
                        Integer rejectNotificationId = intent.getIntExtra(INCOMING_CALL_NOTIFICATION_ID, -1);
                        internalReject(rejectInvite, rejectNotificationId);
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

    private void internalAccept(VideoCallInvite invite, Integer notificationId) {
        Log.d(TAG, "internalAccept()");
        Log.d(TAG, "invite: " + invite);
        Log.d(TAG, "notificationId: " + notificationId);

        activeCall = new VideoCall(invite.getSession(), invite.getFrom());

        WritableMap params = Arguments.createMap();
        for (Map.Entry<String, String> entry : invite.getData().entrySet()) {
            params.putString(entry.getKey(), entry.getValue());
        }
        eventManager.sendEvent(EVENT_CONNECTION_DID_CONNECT, params);
        callNotificationManager.removeNotification(getReactApplicationContext(), notificationId);
    }

    @ReactMethod
    public void accept() {
        Log.d(TAG, "Call accepted. Got context?: " + (getReactApplicationContext() != null));
        Activity activity = getCurrentActivity();
        Intent intent = activity.getIntent();
        VideoCallInvite activeCallInvite = intent.getParcelableExtra(INCOMING_CALL_INVITE);

        Log.d(TAG, "Active call invite: " + activeCallInvite);

        if (activeCallInvite != null) {
            intent.putExtra(INCOMING_CALL_INVITE, (String) null);
            Integer notificationId = activeCallInvite.getSession() == null ? null : activeCallInvite.getSession().hashCode();
            internalAccept(activeCallInvite, notificationId);
        }
    }

    private void internalReject(VideoCallInvite invite, Integer notificationId) {
        Log.d(TAG, "internalReject()");
        Log.d(TAG, "invite: " + invite);
        Log.d(TAG, "notificationId: " + notificationId);
        callNotificationManager.removeNotification(getReactApplicationContext(), notificationId);
        WritableMap params = buildRNNotification(invite);
        eventManager.sendEvent(EVENT_CONNECTION_DID_REJECT, params);
    }

    @ReactMethod
    public void reject() {
        WritableMap params = Arguments.createMap();
        Activity activity = getCurrentActivity();
        Intent intent = activity.getIntent();
        VideoCallInvite invite = intent.getParcelableExtra(INCOMING_CALL_INVITE);

        if (invite != null) {
            intent.putExtra(INCOMING_CALL_INVITE, (String) null);

            for (Map.Entry<String, String> entry : invite.getData().entrySet()) {
                params.putString(entry.getKey(), entry.getValue());
            }
            params.putString("call_state", "DISCONNECTED");

            Integer notificationId = invite.getSession() == null ? null : invite.getSession().hashCode();
            callNotificationManager.removeNotification(getReactApplicationContext(), notificationId);
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
        Activity activity = getCurrentActivity();
        Intent intent = activity.getIntent();
        VideoCallInvite activeCallInvite = intent.getParcelableExtra(INCOMING_CALL_INVITE);

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
            } /*else if (action.equals(ACTION_CANCEL_CALL_INVITE)) {
                handleCancelledInvite(intent);
            } else if (action.equals(ACTION_MISSED_CALL)) {
                SharedPreferences sharedPref = getReactApplicationContext().getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE);
                SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();
                sharedPrefEditor.remove(MISSED_CALLS_GROUP);
                sharedPrefEditor.commit();
            }*/ else {
                Log.e(TAG, "received broadcast unhandled action " + action);
            }
        }
    }

    private WritableMap buildRNNotification(VideoCallInvite ci) {
        WritableMap params = Arguments.createMap();
        if (ci != null) {
            for (Map.Entry<String, String> entry : ci.getData().entrySet()) {
                params.putString(entry.getKey(), entry.getValue());
            }
        }
        return params;
    }
//
//    private void handleCancelledInvite(Intent intent) {
//        CanceledVideoCallInvite cancelledCallInvite = intent.getParcelableExtra(CANCELLED_CALL_INVITE);
//        clearIncomingNotification(cancelledCallInvite);
//        WritableMap params = buildRNNotification(cancelledCallInvite);
//        eventManager.sendEvent(EVENT_CALL_INVITE_CANCELLED, params);
//        clearIncomingNotification(activeCallInvite);
//    }

    private void handleIncomingCallIntent(Intent intent) {
        Log.d(TAG, "handleIncomingCallIntent");
        VideoCallInvite activeCallInvite = intent.getParcelableExtra(INCOMING_CALL_INVITE);

        Log.d(TAG, "activeCallInvite: " + activeCallInvite.toString());

        if (activeCallInvite != null) {
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

}
