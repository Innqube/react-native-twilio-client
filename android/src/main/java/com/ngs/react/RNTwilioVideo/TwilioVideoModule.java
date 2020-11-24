package com.ngs.react.RNTwilioVideo;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.*;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import com.facebook.react.bridge.*;
import com.ngs.react.RNTwilioVoice.SoundPoolManager;
import com.twilio.voice.BuildConfig;
import com.twilio.voice.Call;

import java.util.Map;

import static com.ngs.react.RNTwilioVideo.EventManager.*;
import static com.ngs.react.RNTwilioVideo.VideoConstants.*;

public class TwilioVideoModule extends ReactContextBaseJavaModule {

    public static String TAG = "RNTwilioVideo";
    public static Map<String, Integer> callNotificationMap;

    private final EventManager eventManager;
    private final CallNotificationManager callNotificationManager;
    private final NotificationManager notificationManager;
    private final VideoBroadcastReceiver videoBroadcastReceiver;

    private VideoCallInvite activeCallInvite;
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
//                    case ACTION_HANGUP_CALL:
//                        disconnect();
//                        break;
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
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "accept()");
            }
//            AcceptOptions acceptOptions = new AcceptOptions.Builder()
//                    .enableDscp(true)
//                    .build();
//            activeCallInvite.accept(getReactApplicationContext(), acceptOptions, callListener);
            clearIncomingNotification(activeCallInvite.getCallSid());
        } else {
            eventManager.sendEvent(EVENT_CONNECTION_DID_DISCONNECT, null);
        }
    }

    @ReactMethod
    public void reject() {
        callAccepted = false;
        SoundPoolManager.getInstance(getReactApplicationContext()).stopRinging();
        WritableMap params = Arguments.createMap();
        if (activeCallInvite != null) {
            params.putString("call_sid",   activeCallInvite.getCallSid());
            params.putString("call_from",  activeCallInvite.getFrom());
//            params.putString("call_to",    activeCallInvite.getTo());
            params.putString("call_state", "DISCONNECTED");
//            activeCallInvite.reject(getReactApplicationContext());
            clearIncomingNotification(activeCallInvite.getCallSid());
        }
        eventManager.sendEvent(EVENT_CONNECTION_DID_DISCONNECT, params);
    }

    private class VideoBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "VideoBroadcastReceiver.onReceive " + action + ". Intent " + intent.getExtras());
            }
            if (action.equals(ACTION_INCOMING_CALL)) {
                handleIncomingCallIntent(intent);
            } else if (action.equals(ACTION_CANCEL_CALL_INVITE)) {
                CanceledVideoCallInvite cancelledCallInvite = intent.getParcelableExtra(CANCELLED_CALL_INVITE);
                clearIncomingNotification(cancelledCallInvite.getCallSid());
                WritableMap params = buildRNNotification(cancelledCallInvite);
                eventManager.sendEvent(EVENT_CALL_INVITE_CANCELLED, params);
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
            params.putString("call_sid", ci.getCallSid());
            params.putString("call_from", ci.getFrom());
            // params.putString("call_to", cancelledCallInvite.getTo());
        }
        return params;
    }

    private void handleIncomingCallIntent(Intent intent) {
        if (ACTION_INCOMING_CALL.equals(intent.getAction())) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "handleIncomingCallIntent");
            }
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
            } else {
                // TODO evaluate what more is needed at this point?
                Log.e(TAG, "ACTION_INCOMING_CALL but not active call");
            }
        } else if (ACTION_CANCEL_CALL_INVITE.equals(intent.getAction())) {
            SoundPoolManager.getInstance(getReactApplicationContext()).stopRinging();
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "activeCallInvite was cancelled by " + activeCallInvite.getFrom());
            }
            if (!callAccepted) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "creating a missed call");
                }
                callNotificationManager.createMissedCallNotification(getReactApplicationContext(), activeCallInvite);
                int appImportance = callNotificationManager.getApplicationImportance(getReactApplicationContext());
                if (appImportance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {
                    WritableMap params = buildRNNotification(activeCallInvite);
                    params.putString("call_state", Call.State.DISCONNECTED.toString());
                    eventManager.sendEvent(EVENT_CONNECTION_DID_DISCONNECT, params);
                }
            }
            clearIncomingNotification(activeCallInvite.getCallSid());
        } /* else if (ACTION_FCM_TOKEN.equals(intent.getAction())) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "handleIncomingCallIntent ACTION_FCM_TOKEN");
            }
            registerForCallInvites();
        } */
    }

    private void clearIncomingNotification(String callSid) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "clearIncomingNotification() callSid: "+ callSid);
        }
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
