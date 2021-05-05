package com.ngs.react.RNTwilioVoice;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.NotificationManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.facebook.react.bridge.*;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.twilio.voice.*;
import android.view.Window;
import android.view.WindowManager;

import java.util.HashMap;
import java.util.Map;

import static com.ngs.react.RNTwilioVoice.EventManager.*;

public class TwilioVoiceModule extends ReactContextBaseJavaModule implements ActivityEventListener, LifecycleEventListener {

    public static String TAG = "RNTwilioVoice";

    private AudioManager audioManager;
    private int originalAudioMode = AudioManager.MODE_NORMAL;

    private boolean isReceiverRegistered = false;
    private BroadcastReceiver broadcastReceiver;

    // Empty HashMap, contains parameters for the Outbound call
    private HashMap<String, String> twiMLParams = new HashMap<>();

    private NotificationManager notificationManager;
    private CallNotificationManager callNotificationManager;
    private ProximityManager proximityManager;

    private String accessToken;

    private String toNumber = "";
    private String toName = "";

    private RegistrationListener registrationListener = registrationListener();
    private Call.Listener callListener = callListener();

    private VoiceCallInvite activeCallInvite;
    private Call activeCall;

    // this variable determines when to create missed calls notifications
    private Boolean callAccepted = false;

    private AudioFocusRequest focusRequest;
    private HeadsetManager headsetManager;
    private EventManager eventManager;

    public TwilioVoiceModule(ReactApplicationContext reactContext,
    boolean shouldAskForMicPermission) {
        super(reactContext);
        if (BuildConfig.DEBUG) {
            Voice.setLogLevel(LogLevel.DEBUG);
        } else {
            Voice.setLogLevel(LogLevel.ERROR);
        }
        reactContext.addActivityEventListener(this);
        reactContext.addLifecycleEventListener(this);

        eventManager = new EventManager(reactContext);
        callNotificationManager = new CallNotificationManager();
        proximityManager = new ProximityManager(reactContext, eventManager);
        headsetManager = new HeadsetManager(eventManager);

        notificationManager = (NotificationManager) reactContext.getSystemService(Context.NOTIFICATION_SERVICE);

        /*
         * Setup the broadcast receiver to be notified of GCM Token updates
         * or incoming call messages in this Activity.
         */
//        voiceBroadcastReceiver = new VoiceBroadcastReceiver();
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(TAG, "Broadcast receiver - action: " + action);
                switch (action) {
                    case VoiceConstants.ACTION_INCOMING_CALL:
                        handleIncomingCallIntent(intent);
                        break;
                    case VoiceConstants.ACTION_CANCEL_CALL_INVITE:
                        handleCancelCallIntent(intent);
                        break;
                    case VoiceConstants.ACTION_ANSWER_CALL:
                        activeCallInvite = intent.getParcelableExtra(VoiceConstants.INCOMING_CALL_INVITE);
                        accept();
                        break;
                    case VoiceConstants.ACTION_REJECT_CALL:
                        VoiceCallInvite invite = intent.getParcelableExtra(VoiceConstants.INCOMING_CALL_INVITE);
                        rejectInternal(invite);
                        break;
                    case VoiceConstants.ACTION_HANGUP_CALL:
                        disconnect();
                        break;
                    case VoiceConstants.ACTION_CLEAR_MISSED_CALLS_COUNT:
                        SharedPreferences sharedPref = context.getSharedPreferences(VoiceConstants.PREFERENCE_KEY, Context.MODE_PRIVATE);
                        SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();
                        sharedPrefEditor.putInt(VoiceConstants.MISSED_CALLS_GROUP, 0);
                        sharedPrefEditor.commit();
                }
                // Dismiss the notification when the user tap on the relative notification action
                // eventually the notification will be cleared anyway
                // but in this way there is no UI lag
//                notificationManager.cancel(intent.getIntExtra(VoiceConstants.INCOMING_CALL_NOTIFICATION_ID, 0));
            }
        };
        registerActionReceiver();

        /*
         * Needed for setting/abandoning audio focus during a call
         */
        audioManager = (AudioManager) reactContext.getSystemService(Context.AUDIO_SERVICE);

        /*
         * Ensure the microphone permission is enabled
         */
        if (shouldAskForMicPermission && !checkPermissionForMicrophone()) {
            requestPermissionForMicrophone();
        }
    }

    @Override
    public void onHostResume() {
        /*
         * Enable changing the volume using the up/down keys during a conversation
         */
        getCurrentActivity().setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
        registerActionReceiver();
    }

    @Override
    public void onHostPause() {
    }

    @Override
    public void onHostDestroy() {
        disconnect();
        unsetAudioFocus();
    }

    @Override
    public void onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy();
        unregisterReceiver();
    }

    @Override
    public String getName() {
        return TAG;
    }

    public void onNewIntent(Intent intent) {
        // This is called only when the App is in the foreground
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onNewIntent " + intent.toString());
        }
//        handleIncomingCallIntent(intent);
    }

    private RegistrationListener registrationListener() {
        return new RegistrationListener() {
            @Override
            public void onRegistered(String accessToken, String fcmToken) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Successfully registered FCM");
                }
                eventManager.sendEvent(EVENT_DEVICE_READY, null);
            }

            @Override
            public void onError(RegistrationException error, String accessToken, String fcmToken) {
                Log.e(TAG, String.format("Registration Error: %d, %s", error.getErrorCode(), error.getMessage()));
                WritableMap params = Arguments.createMap();
                params.putString("err", error.getMessage());
                eventManager.sendEvent(EVENT_DEVICE_NOT_READY, params);
            }
        };
    }

    private Call.Listener callListener() {
        return new Call.Listener() {
            /*
             * This callback is emitted once before the Call.Listener.onConnected() callback when
             * the callee is being alerted of a Call. The behavior of this callback is determined by
             * the answerOnBridge flag provided in the Dial verb of your TwiML application
             * associated with this client. If the answerOnBridge flag is false, which is the
             * default, the Call.Listener.onConnected() callback will be emitted immediately after
             * Call.Listener.onRinging(). If the answerOnBridge flag is true, this will cause the
             * call to emit the onConnected callback only after the call is answered.
             * See answeronbridge for more details on how to use it with the Dial TwiML verb. If the
             * twiML response contains a Say verb, then the call will emit the
             * Call.Listener.onConnected callback immediately after Call.Listener.onRinging() is
             * raised, irrespective of the value of answerOnBridge being set to true or false
             */
            @Override
            public void onRinging(Call call) {
                // TODO test this with JS app
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "CALL RINGING callListener().onRinging call state = "+call.getState());
                    Log.d(TAG, call.toString());
                }
                WritableMap params = Arguments.createMap();
                if (call != null) {
                    params.putString("call_sid",   call.getSid());
                    params.putString("call_from",  call.getFrom());
                }
                eventManager.sendEvent(EVENT_CALL_STATE_RINGING, params);
            }

            @Override
            public void onConnected(Call call) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "CALL CONNECTED callListener().onConnected call state = "+call.getState());
                }
                setAudioFocus();
                proximityManager.startProximitySensor();
                headsetManager.startWiredHeadsetEvent(getReactApplicationContext());

                WritableMap params = Arguments.createMap();
                if (call != null) {
                    params.putString("call_sid",   call.getSid());
                    params.putString("call_state", call.getState().name());
                    params.putString("call_from", call.getFrom());
                    params.putString("call_to", call.getTo());
                    String caller = "Show call details in the app";
                    if (!toName.equals("")) {
                        caller = toName;
                    } else if (!toNumber.equals("")) {
                        caller = toNumber;
                    }
                    activeCall = call;
//                    callNotificationManager.createHangupLocalNotification(getReactApplicationContext(),
//                            call.getSid(), caller);
                }
                eventManager.sendEvent(EVENT_CONNECTION_DID_CONNECT, params);
            }

            /**
             * `onReconnecting()` callback is raised when a network change is detected and Call is already in `CONNECTED`
             * `Call.State`. If the call is in `CONNECTING` or `RINGING` when network change happened the SDK will continue
             * attempting to connect, but a reconnect event will not be raised.
             */
            @Override
            public void onReconnecting(@NonNull Call call, @NonNull CallException callException) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "CALL RECONNECTING callListener().onReconnecting call state = "+call.getState());
                }
                WritableMap params = Arguments.createMap();
                if (call != null) {
                    params.putString("call_sid",   call.getSid());
                    params.putString("call_from", call.getFrom());
                    params.putString("call_to", call.getTo());
                }
                eventManager.sendEvent(EVENT_CONNECTION_IS_RECONNECTING, params);

            }

            /**
             * The call is successfully reconnected after reconnecting attempt.
             */
            @Override
            public void onReconnected(@NonNull Call call) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "CALL RECONNECTED callListener().onReconnected call state = "+call.getState());
                }
                WritableMap params = Arguments.createMap();
                if (call != null) {
                    params.putString("call_sid",   call.getSid());
                    params.putString("call_from", call.getFrom());
                    params.putString("call_to", call.getTo());
                }
                eventManager.sendEvent(EVENT_CONNECTION_DID_RECONNECT, params);
            }

            @Override
            public void onDisconnected(Call call, CallException error) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "CALL DISCONNECTED callListener().onDisconnected call state = "+call.getState());
                }
                unsetAudioFocus();
                proximityManager.stopProximitySensor();
                headsetManager.stopWiredHeadsetEvent(getReactApplicationContext());
                callAccepted = false;

                WritableMap params = Arguments.createMap();
                String callSid = "";
                if (call != null) {
                    callSid = call.getSid();
                    params.putString("call_sid", callSid);
                    params.putString("call_state", call.getState().name());
                    params.putString("call_from", call.getFrom());
                    params.putString("call_to", call.getTo());
                }
                if (error != null) {
                    Log.e(TAG, String.format("CallListener onDisconnected error: %d, %s",
                            error.getErrorCode(), error.getMessage()));
                    params.putString("err", error.getMessage());
                }
                if (callSid != null && activeCall != null && activeCall.getSid() != null && activeCall.getSid().equals(callSid)) {
                    activeCall = null;
                }
                eventManager.sendEvent(EVENT_CONNECTION_DID_DISCONNECT, params);
                toNumber = "";
                toName = "";
            }

            @Override
            public void onConnectFailure(Call call, CallException error) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "CALL FAILURE callListener().onConnectFailure call state = "+call.getState());
                }
                unsetAudioFocus();
                proximityManager.stopProximitySensor();
                callAccepted = false;


                Log.e(TAG, String.format("CallListener onConnectFailure error: %d, %s",
                    error.getErrorCode(), error.getMessage()));

                WritableMap params = Arguments.createMap();
                params.putString("err", error.getMessage());
                String callSid = "";
                if (call != null) {
                    callSid = call.getSid();
                    params.putString("call_sid", callSid);
                    params.putString("call_state", call.getState().name());
                    params.putString("call_from", call.getFrom());
                    params.putString("call_to", call.getTo());
                }
                if (callSid != null && activeCall != null && activeCall.getSid() != null && activeCall.getSid().equals(callSid)) {
                    activeCall = null;
                }
                eventManager.sendEvent(EVENT_CONNECTION_DID_DISCONNECT, params);
                toNumber = "";
                toName = "";
            }
        };
    }

    private void unregisterReceiver() {
        if (isReceiverRegistered) {
            Log.d(TAG, "unregisterReceiver");
            getReactApplicationContext().unregisterReceiver(broadcastReceiver);
            isReceiverRegistered = false;
        }
    }

    private void registerActionReceiver() {
        Log.d(TAG, "registerActionReceiver - already registered: " + isReceiverRegistered);
        if (!isReceiverRegistered) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(VoiceConstants.ACTION_ANSWER_CALL);
            intentFilter.addAction(VoiceConstants.ACTION_REJECT_CALL);
            intentFilter.addAction(VoiceConstants.ACTION_HANGUP_CALL);
            intentFilter.addAction(VoiceConstants.ACTION_CLEAR_MISSED_CALLS_COUNT);
            intentFilter.addAction(VoiceConstants.ACTION_INCOMING_CALL);
            intentFilter.addAction(VoiceConstants.ACTION_CANCEL_CALL_INVITE);

            getReactApplicationContext().registerReceiver(broadcastReceiver, intentFilter);
            isReceiverRegistered = true;
        }
    }

    // removed @Override temporarily just to get it working on different versions of RN
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        onActivityResult(requestCode, resultCode, data);
    }

    // removed @Override temporarily just to get it working on different versions of RN
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Ignored, required to implement ActivityEventListener for RN 0.33
    }

    private WritableMap buildRNNotification(VoiceCallInvite ci) {
        WritableMap params = Arguments.createMap();
        if (ci != null) {
            for (Map.Entry<String, String> entry : ci.getData().entrySet()) {
                params.putString(entry.getKey(), entry.getValue());
            }
        }
        return params;
    }

    private void handleIncomingCallIntent(Intent intent) {
        Log.d(TAG, "handleIncomingCallIntent");
//        if (VoiceConstants.ACTION_INCOMING_CALL.equals(intent.getAction())) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "handleIncomingCallIntent");
        }
        activeCallInvite = intent.getParcelableExtra(VoiceConstants.INCOMING_CALL_INVITE);
        Log.d(TAG, "activeCallInvite != null: " + (activeCallInvite != null));
        if (activeCallInvite != null) {
            callAccepted = false;
        }
    }

    private void handleCancelCallIntent(Intent intent) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "activeCallInvite was cancelled by " + activeCallInvite.getFrom());
        }
        activeCallInvite = null;
        if (!callAccepted) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "creating a missed call");
            }
//                callNotificationManager.createMissedCallNotification(getReactApplicationContext(), activeCallInvite);
            int appImportance = callNotificationManager.getApplicationImportance(getReactApplicationContext());
            if (appImportance != RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {
                WritableMap params = buildRNNotification(activeCallInvite);
                params.putString("call_state", Call.State.DISCONNECTED.toString());
                eventManager.sendEvent(EVENT_CONNECTION_DID_DISCONNECT, params);
            }
        }
    }

    @ReactMethod
    public void initWithAccessToken(final String accessToken, Promise promise) {
        if (accessToken == null || accessToken.equals("")) {
            promise.reject(new JSApplicationIllegalArgumentException("Invalid access token"));
            return;
        }

        if(!checkPermissionForMicrophone()) {
            promise.reject(new AssertionException("Allow microphone permission"));
            return;
        }

        TwilioVoiceModule.this.accessToken = accessToken;
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "initWithAccessToken");
        }
        registerForCallInvites();
        WritableMap params = Arguments.createMap();
        params.putBoolean("initialized", true);
        promise.resolve(params);
    }

    private void clearIncomingNotification(String callSid) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "clearIncomingNotification() callSid: "+ callSid);
        }
        int notificationId = callSid.hashCode();
        callNotificationManager.removeNotification(getReactApplicationContext(), notificationId);
        activeCallInvite = null;
    }

    /*
     * Register your FCM token with Twilio to receive incoming call invites
     *
     * If a valid google-services.json has not been provided or the FirebaseInstanceId has not been
     * initialized the fcmToken will be null.
     *
     */
    private void registerForCallInvites() {
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "getInstanceId failed", task.getException());
                            return;
                        }

                        // Get new Instance ID token
                        String fcmToken = task.getResult().getToken();
                        if (fcmToken != null) {
                            Log.d(TAG, "Registering with FCM token " + fcmToken);
                            Voice.register(accessToken, Voice.RegistrationChannel.FCM, fcmToken, registrationListener);
                        }
                    }
                });
    }

    @ReactMethod
    public void accept() {
        callAccepted = true;
        Log.d(TAG, "Accepting call " + activeCallInvite);
        if (activeCallInvite != null) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "accept()");
            }
            if (getReactApplicationContext().getCurrentActivity() != null) {
                Window window = getReactApplicationContext().getCurrentActivity().getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                );
            }

            WritableMap params = buildRNNotification(activeCallInvite);
//            eventManager.sendEvent(EVENT_CONNECTION_DID_CONNECT, params);
            eventManager.sendEvent(EVENT_CALL_ACCEPTED, params);

            clearIncomingNotification(activeCallInvite.getSession());
        } else {
            eventManager.sendEvent(EVENT_CONNECTION_DID_DISCONNECT, null);
        }
    }

    private void rejectInternal(VoiceCallInvite invite) {
        WritableMap params = buildRNNotification(invite);
        clearIncomingNotification(invite.getSession());
        eventManager.sendEvent(EVENT_CALL_REJECTED, params);
    }

    @ReactMethod
    public void reject() {
        callAccepted = false;
        Log.d(TAG, "activeCallInvite != null: " + (activeCallInvite != null));
        if (activeCallInvite != null) {
            rejectInternal(activeCallInvite);
            activeCallInvite = null;
        }
    }

    @ReactMethod
    public void ignore() {
        callAccepted = false;
        if (activeCallInvite != null) {
            WritableMap params = buildRNNotification(activeCallInvite);
            params.putString("call_state", "BUSY");
            clearIncomingNotification(activeCallInvite.getSession());
            eventManager.sendEvent(EVENT_CONNECTION_DID_DISCONNECT, params);
        }
    }

    @ReactMethod
    public void connect(ReadableMap params, String accessToken) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "connect params: "+params);
        }
        WritableMap errParams = Arguments.createMap();
        if (accessToken == null) {
            errParams.putString("err", "Invalid access token");
            eventManager.sendEvent(EVENT_DEVICE_NOT_READY, errParams);
            return;
        }
//        if (params == null) {
//            errParams.putString("err", "Invalid parameters");
//            eventManager.sendEvent(EVENT_CONNECTION_DID_DISCONNECT, errParams);
//            return;
//        } else if (!params.hasKey("To")) {
//            errParams.putString("err", "Invalid To parameter");
//            eventManager.sendEvent(EVENT_CONNECTION_DID_DISCONNECT, errParams);
//            return;
//        }
//        toNumber = params.getString("To");
//        if (params.hasKey("ToName")) {
//            toName = params.getString("ToName");
//        }

        twiMLParams.clear();

        ReadableMapKeySetIterator iterator = params.keySetIterator();
        while (iterator.hasNextKey()) {
            String key = iterator.nextKey();
            ReadableType readableType = params.getType(key);
            switch (readableType) {
                case Null:
                    twiMLParams.put(key, "");
                    break;
                case Boolean:
                    twiMLParams.put(key, String.valueOf(params.getBoolean(key)));
                    break;
                case Number:
                    // Can be int or double.
                    twiMLParams.put(key, String.valueOf(params.getDouble(key)));
                    break;
                case String:
                    twiMLParams.put(key, params.getString(key));
                    break;
                default:
                    Log.d(TAG, "Could not convert with key: " + key + ".");
                    break;
            }
        }

        ConnectOptions connectOptions = new ConnectOptions.Builder(accessToken)
            .enableDscp(true)
            .params(twiMLParams)
            .build();

        activeCall = Voice.connect(getReactApplicationContext(), connectOptions, callListener);
    }

    @ReactMethod
    public void disconnect() {
        if (activeCall != null) {
            activeCall.disconnect();
            activeCall = null;
        }
        activeCallInvite = null;
    }

    @ReactMethod
    public void setMuted(Boolean value) {
        if (activeCall != null) {
            activeCall.mute(value);
        }
    }

    @ReactMethod
    public void sendDigits(String digits) {
        if (activeCall != null) {
            activeCall.sendDigits(digits);
        }
    }

    @ReactMethod
    public void getActiveCall(Promise promise) {
        if (activeCall != null) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Active call found state = "+activeCall.getState());
            }
            WritableMap params = Arguments.createMap();
            String toNum = activeCall.getTo();
            if (toNum == null) {
                toNum = toNumber;
            }
            params.putString("call_sid",   activeCall.getSid());
            params.putString("call_from",  activeCall.getFrom());
            params.putString("call_to",    toNum);
            params.putString("call_state", activeCall.getState().name());
            promise.resolve(params);
            return;
        }
        promise.resolve(null);
    }

    @ReactMethod
    public void getCallInvite(Promise promise) {
        Log.d(TAG, "getCallInvite");

        Activity activity = getCurrentActivity();
        Intent intent = activity.getIntent();
        activeCallInvite = intent.getParcelableExtra(VoiceConstants.INCOMING_CALL_INVITE);

        if (activeCallInvite != null) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Call invite found "+ activeCallInvite);
            }
            intent.removeExtra(VoiceConstants.INCOMING_CALL_INVITE);
            WritableMap params = buildRNNotification(activeCallInvite);
            promise.resolve(params);
            return;
        }
        promise.resolve(null);
    }

    @ReactMethod
    public void setSpeakerPhone(Boolean value) {
        // TODO check whether it is necessary to call setAudioFocus again
//        setAudioFocus();
        audioManager.setSpeakerphoneOn(value);
    }

    @ReactMethod
    public void setOnHold(Boolean value) {
        if (activeCall != null) {
            activeCall.hold(value);
        }
    }

    private void setAudioFocus() {
        if (audioManager == null) {
            audioManager.setMode(originalAudioMode);
            audioManager.abandonAudioFocus(null);
            return;
        }
        originalAudioMode = audioManager.getMode();
        // Request audio focus before making any device switch
        if (Build.VERSION.SDK_INT >= 26) {
            AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build();
            focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(new AudioManager.OnAudioFocusChangeListener() {
                    @Override
                    public void onAudioFocusChange(int i) { }
                })
                .build();
            audioManager.requestAudioFocus(focusRequest);
        } else {
            int focusRequestResult = audioManager.requestAudioFocus(new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {}
            },
            AudioManager.STREAM_VOICE_CALL,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        }
        /*
         * Start by setting MODE_IN_COMMUNICATION as default audio mode. It is
         * required to be in this mode when playout and/or recording starts for
         * best possible VoIP performance. Some devices have difficulties with speaker mode
         * if this is not set.
         */
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
    }

    private void unsetAudioFocus() {
        if (audioManager == null) {
            audioManager.setMode(originalAudioMode);
            audioManager.abandonAudioFocus(null);
            return;
        }
        audioManager.setMode(originalAudioMode);
        if (Build.VERSION.SDK_INT >= 26) {
            if (focusRequest != null) {
                audioManager.abandonAudioFocusRequest(focusRequest);
            }
        } else {
            audioManager.abandonAudioFocus(null);
        }
    }

    private boolean checkPermissionForMicrophone() {
        int resultMic = ContextCompat.checkSelfPermission(getReactApplicationContext(), Manifest.permission.RECORD_AUDIO);
        return resultMic == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissionForMicrophone() {
        if (getCurrentActivity() == null) {
            return;
        }
        if (ActivityCompat.shouldShowRequestPermissionRationale(getCurrentActivity(), Manifest.permission.RECORD_AUDIO)) {
//            Snackbar.make(coordinatorLayout,
//                    "Microphone permissions needed. Please allow in your application settings.",
//                    SNACKBAR_DURATION).show();
        } else {
            ActivityCompat.requestPermissions(getCurrentActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, VoiceConstants.MIC_PERMISSION_REQUEST_CODE);
        }
    }

    @ReactMethod
    public void unregister(String token, Promise promise) {
        Log.d(TAG, "unregistering with token: " + token);

        FirebaseInstanceId
            .getInstance()
            .getInstanceId()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "getInstanceId failed", task.getException());
                    promise.reject("666", "getInstanceId failed");
                    return;
                }

                // Get new Instance ID token
                String fcmToken = task.getResult().getToken();
                if (fcmToken != null) {

                    Log.d(TAG, "Unregistering with FCM token: " + fcmToken + " and access token: " + accessToken);

                    Voice.unregister(token, Voice.RegistrationChannel.FCM, fcmToken, new UnregistrationListener() {
                        @Override
                        public void onUnregistered(String accessToken, String fcmToken) {
                            WritableMap json = new WritableNativeMap();
                            json.putString("accessToken", accessToken);
                            json.putString("fcmToken", fcmToken);
                            promise.resolve(json);
                        }

                        @Override
                        public void onError(RegistrationException registrationException, String accessToken, String fcmToken) {
                            promise.reject(registrationException);
                        }
                    });
                }
            });
    }
}
