package com.ngs.react.RNTwilioVideo;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.*;
import android.os.Build;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
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
    private BroadcastReceiver broadcastReceiver;

    private VideoCall activeCall;
    private boolean isReceiverRegistered = false;

    public TwilioVideoModule(ReactApplicationContext rc) {
        super(rc);

        eventManager = new EventManager(rc);
        videoBroadcastReceiver = new VideoBroadcastReceiver();
        callNotificationManager = new CallNotificationManager();
        notificationManager = (NotificationManager) rc.getSystemService(Context.NOTIFICATION_SERVICE);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(TAG, "TwilioVideoModule.BroadCastReceiver.onReceive: " + action);
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
                    case ACTION_GO_OFFLINE:
                        VideoCallInvite goOfflineInvite = intent.getParcelableExtra(INCOMING_CALL_INVITE);
                        Integer goOfflineNotificationId = intent.getIntExtra(INCOMING_CALL_NOTIFICATION_ID, -1);
                        internalGoOffline(goOfflineInvite, goOfflineNotificationId);
                        break;
                }
                notificationManager.cancel(intent.getIntExtra(INCOMING_CALL_NOTIFICATION_ID, 0));
            }
        };

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
        intentFilter.addAction(ACTION_GO_OFFLINE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getReactApplicationContext().registerReceiver(broadcastReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            getReactApplicationContext().registerReceiver(broadcastReceiver, intentFilter);
        }
    }

    private void internalAccept(VideoCallInvite invite, Integer notificationId) {
        Log.d(TAG, "internalAccept()");
        Log.d(TAG, "invite: " + invite);
        Log.d(TAG, "notificationId: " + notificationId);

        activeCall = new VideoCall(invite.getSession(), invite.getFrom("\n", null));

        if (getReactApplicationContext().getCurrentActivity() != null) {
            Window window = getReactApplicationContext().getCurrentActivity().getWindow();
            getReactApplicationContext().getCurrentActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
                };
            });
        }

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

    private void internalGoOffline(VideoCallInvite invite, Integer notificationId) {
        Log.d(TAG, "internalGoOffline()");
        Log.d(TAG, "invite: " + invite);
        Log.d(TAG, "notificationId: " + notificationId);
        callNotificationManager.removeNotification(getReactApplicationContext(), notificationId);
        WritableMap params = buildRNNotification(invite);
        eventManager.sendEvent(EVENT_GO_OFFLINE, params);
    }

    @ReactMethod
    public void disconnect() {
        activeCall = null;
        // Remove layout flags to allow phone to re-lock
        if (getReactApplicationContext().getCurrentActivity() != null) {
            Window window = getReactApplicationContext().getCurrentActivity().getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            );
        }
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
        Log.d(TAG, "getCallInvite");
        Activity activity = getCurrentActivity();
        if (activity != null) {
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
        } else {
            Log.d(TAG, "Warning! getCurrentActivity() is null");
        }
        promise.resolve(null);
    }

    private class VideoBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "VideoBroadcastReceiver.onReceive " + action + ". Intent " + intent.getExtras());
            if (action.equals(ACTION_INCOMING_CALL)) {
                handlePendingIntent(intent, EVENT_DEVICE_DID_RECEIVE_INCOMING);
            } else {
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

    private void handlePendingIntent(Intent intent, String event) {
        Log.d(TAG, "handleIncomingCallIntent");
        VideoCallInvite activeCallInvite = intent.getParcelableExtra(INCOMING_CALL_INVITE);

        Log.d(TAG, "activeCallInvite: " + activeCallInvite.toString());

        if (activeCallInvite != null) {
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
