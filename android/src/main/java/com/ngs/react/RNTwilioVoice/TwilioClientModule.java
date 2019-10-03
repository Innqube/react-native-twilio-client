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
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import com.facebook.react.bridge.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;
import com.twilio.voice.*;

import java.util.HashMap;
import java.util.Map;

import static com.ngs.react.RNTwilioClient.EventManager.*;

public class TwilioClientModule extends ReactContextBaseJavaModule implements ActivityEventListener, LifecycleEventListener {

    public static String TAG = "RNTwilioVoiceClient";
    private static final int MIC_PERMISSION_REQUEST_CODE = 1;
    private AudioManager audioManager;
    private int originalAudioMode = AudioManager.MODE_NORMAL;
    private boolean isReceiverRegistered = false;
    private VoiceBroadcastReceiver voiceBroadcastReceiver;
    private HashMap<String, String> twiMLParams = new HashMap<>();

    public static final String INCOMING_CALL_INVITE = "INCOMING_CALL_INVITE";
    public static final String INCOMING_CALL_NOTIFICATION_ID = "INCOMING_CALL_NOTIFICATION_ID";
    public static final String NOTIFICATION_TYPE = "NOTIFICATION_TYPE";
    public static final String ACTION_INCOMING_CALL = "com.ngs.react.TwilioClient.INCOMING_CALL";
    public static final String ACTION_FCM_TOKEN = "com.ngs.react.TwilioClient.ACTION_FCM_TOKEN";
    public static final String ACTION_MISSED_CALL = "com.ngs.react.TwilioClient.MISSED_CALL";
    public static final String ACTION_ANSWER_CALL = "com.ngs.react.TwilioClient.ANSWER_CALL";
    public static final String ACTION_ANSWER_VIDEO_CALL = "com.ngs.react.TwilioClient.ANSWER_VIDEO_CALL";
    public static final String ACTION_REJECT_CALL = "com.ngs.react.TwilioClient.REJECT_CALL";
    public static final String ACTION_HANGUP_CALL = "com.ngs.react.TwilioClient.HANGUP_CALL";
    public static final String ACTION_CLEAR_MISSED_CALLS_COUNT = "com.ngs.react.TwilioClient.CLEAR_MISSED_CALLS_COUNT";
    public static final String CALL_SID_KEY = "CALL_SID";
    public static final String INCOMING_NOTIFICATION_PREFIX = "Incoming_";
    public static final String MISSED_CALLS_GROUP = "MISSED_CALLS";
    public static final int MISSED_CALLS_NOTIFICATION_ID = 1;
    public static final int HANGUP_NOTIFICATION_ID = 11;
    public static final int CLEAR_MISSED_CALLS_NOTIFICATION_ID = 21;
    public static final String PREFERENCE_KEY = "com.ngs.react.TwilioClient.PREFERENCE_FILE_KEY";

    private NotificationManager notificationManager;
    private CallNotificationManager callNotificationManager;
    private ProximityManager proximityManager;
    private String accessToken;
    private String toNumber = "";
    private String toName = "";
    static Map<String, Integer> callNotificationMap;
    static Map<String, String> dictionaryPayload;
    private RegistrationListener registrationListener = registrationListener();
    private Call.Listener callListener = callListener();
    private CallInvite activeCallInvite;
    private Call activeCall;
    private Boolean callAccepted = false;
    private AudioFocusRequest focusRequest;
    private HeadsetManager headsetManager;
    private EventManager eventManager;

    public TwilioClientModule(ReactApplicationContext reactContext,
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

        notificationManager = (android.app.NotificationManager) reactContext.getSystemService(Context.NOTIFICATION_SERVICE);

        voiceBroadcastReceiver = new VoiceBroadcastReceiver();
        registerReceiver();

        TwilioClientModule.callNotificationMap = new HashMap<>();
        TwilioClientModule.dictionaryPayload = new HashMap<>();

        audioManager = (AudioManager) reactContext.getSystemService(Context.AUDIO_SERVICE);

        if (shouldAskForMicPermission && !checkPermissionForMicrophone()) {
            requestPermissionForMicrophone();
        }
    }

    @Override
    public void onHostResume() {
        getCurrentActivity().setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
        registerReceiver();
    }

    @Override
    public void onHostPause() {
//        unregisterReceiver();
    }

    @Override
    public void onHostDestroy() {
        disconnect();
        callNotificationManager.removeHangupNotification(getReactApplicationContext());
        unsetAudioFocus();
    }

    @Override
    public String getName() {
        return TAG;
    }

    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult " + activity);
    }

    public void onNewIntent(Intent intent) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onNewIntent " + intent.toString());
        }
        handleIncomingCallIntent(intent);
    }

    // ******** Listeners ********
    private RegistrationListener registrationListener() {
        return new RegistrationListener() {
            @Override
            public void onRegistered(String accessToken, String fcmToken) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Successfully registered FCM");
                }
                eventManager.sendEvent(EVENT_DEVICE_READY);

                WritableMap params = Arguments.createMap();
                params.putString("token", fcmToken);
                eventManager.sendEvent(EVENT_VOIP_REMOTE_NOTIFICATION_REGISTERED, fcmToken);
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
            @Override
            public void onConnected(Call call) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "CALL CONNECTED callListener().onConnected call state = " + call.getState());
                }
                setAudioFocus();
                proximityManager.startProximitySensor();
                headsetManager.startWiredHeadsetEvent(getReactApplicationContext());

                WritableMap params = Arguments.createMap();
                if (call != null) {
                    params.putString("call_sid", call.getSid());
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
                    callNotificationManager.createHangupLocalNotification(getReactApplicationContext(),
                            call.getSid(), caller);
                }
                eventManager.sendEvent(EVENT_CONNECTION_DID_CONNECT, params);
            }

            @Override
            public void onDisconnected(Call call, CallException error) {
                unsetAudioFocus();
                proximityManager.stopProximitySensor();
                headsetManager.stopWiredHeadsetEvent(getReactApplicationContext());
                callAccepted = false;

                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "call disconnected");
                }

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
                callNotificationManager.removeHangupNotification(getReactApplicationContext());
                toNumber = "";
                toName = "";
            }

            @Override
            public void onConnectFailure(Call call, CallException error) {
                unsetAudioFocus();
                proximityManager.stopProximitySensor();
                callAccepted = false;
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "connect failure");
                }

                Log.e(TAG, String.format("CallListener onDisconnected error: %d, %s",
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
                callNotificationManager.removeHangupNotification(getReactApplicationContext());
                toNumber = "";
                toName = "";
            }
        };
    }

    // ******** End Listeners ********

    /**
     * Register the Voice broadcast receiver
     */
    private void registerReceiver() {
        if (!isReceiverRegistered) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_INCOMING_CALL);
            intentFilter.addAction(ACTION_MISSED_CALL);
            LocalBroadcastManager.getInstance(getReactApplicationContext()).registerReceiver(
                    voiceBroadcastReceiver, intentFilter);
            registerActionReceiver();
            isReceiverRegistered = true;
        }
    }

    private void registerActionReceiver() {

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_ANSWER_CALL);
        intentFilter.addAction(ACTION_ANSWER_VIDEO_CALL);
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
                    case ACTION_ANSWER_VIDEO_CALL:
                        acceptVri();
                        break;
                    case ACTION_REJECT_CALL:
                        reject();
                        break;
                    case ACTION_HANGUP_CALL:
                        disconnect();
                        break;
                    case ACTION_CLEAR_MISSED_CALLS_COUNT:
                        SharedPreferences sharedPref = context.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE);
                        SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();
                        sharedPrefEditor.putInt(MISSED_CALLS_GROUP, 0);
                        sharedPrefEditor.commit();
                }
                notificationManager.cancel(intent.getIntExtra(INCOMING_CALL_NOTIFICATION_ID, 0));
            }
        }, intentFilter);
    }

    private void handleIncomingCallIntent(Intent intent) {
        if (intent == null || intent.getAction() == null) {
            Log.e(TAG, "handleIncomingCallIntent intent is null");
            return;
        }

        if (intent.getAction().equals(ACTION_INCOMING_CALL)) {
            activeCallInvite = intent.getParcelableExtra(INCOMING_CALL_INVITE);

            if (activeCallInvite != null && (activeCallInvite.getState() == CallInvite.State.PENDING)) {
                callAccepted = false;
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "handleIncomingCallIntent state = PENDING");
                }
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
                if (appImportance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND ||
                        appImportance == RunningAppProcessInfo.IMPORTANCE_SERVICE) {

                    WritableMap params = Arguments.createMap();
                    params.putString("call_sid", activeCallInvite.getCallSid());
                    params.putString("call_from", activeCallInvite.getFrom());
                    params.putString("call_to", activeCallInvite.getTo());
                    params.putString("call_state", activeCallInvite.getState().name());
                    eventManager.sendEvent(EVENT_DEVICE_DID_RECEIVE_INCOMING, params);
                }


            } else {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "====> BEGIN handleIncomingCallIntent when activeCallInvite != PENDING");
                }
                // this block is executed when the callInvite is cancelled and:
                //   - the call is answered (activeCall != null)
                //   - the call is rejected

                SoundPoolManager.getInstance(getReactApplicationContext()).stopRinging();

                // the call is not active yet
                if (activeCall == null) {

                    if (activeCallInvite != null) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "activeCallInvite state = " + activeCallInvite.getState());
                        }
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "activeCallInvite was cancelled by " + activeCallInvite.getFrom());
                        }
                        if (!callAccepted) {
                            if (BuildConfig.DEBUG) {
                                Log.d(TAG, "creating a missed call, activeCallInvite state: " + activeCallInvite.getState());
                            }
                            callNotificationManager.createMissedCallNotification(getReactApplicationContext(), activeCallInvite);
                            int appImportance = callNotificationManager.getApplicationImportance(getReactApplicationContext());
                            if (appImportance != RunningAppProcessInfo.IMPORTANCE_BACKGROUND) {
                                WritableMap params = Arguments.createMap();
                                params.putString("call_sid", activeCallInvite.getCallSid());
                                params.putString("call_from", activeCallInvite.getFrom());
                                params.putString("call_to", activeCallInvite.getTo());
                                params.putString("call_state", activeCallInvite.getState().name());
                                eventManager.sendEvent(EVENT_CONNECTION_DID_DISCONNECT, params);
                            }
                        }
                    }
                    clearIncomingNotification(activeCallInvite);
                } else {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "activeCallInvite was answered. Call " + activeCall);
                    }
                }
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "====> END");
                }
            }
        } else if (intent.getAction().equals(ACTION_FCM_TOKEN)) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "handleIncomingCallIntent ACTION_FCM_TOKEN");
            }
            registerForCallInvites();
        }
    }

    private class VoiceBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_INCOMING_CALL)) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "VoiceBroadcastReceiver.onReceive ACTION_INCOMING_CALL. Intent " + intent.getExtras());
                }
                handleIncomingCallIntent(intent);
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

    @ReactMethod
    public void initWithAccessToken(final String accessToken, Promise promise) {
        if (accessToken.equals("")) {
            promise.reject(new JSApplicationIllegalArgumentException("Invalid access token"));
            return;
        }

        if (!checkPermissionForMicrophone()) {
            promise.reject(new AssertionException("Can't init without microphone permission"));
        }

        TwilioClientModule.this.accessToken = accessToken;
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "initWithAccessToken ACTION_FCM_TOKEN");
        }
        registerForCallInvites();
        WritableMap params = Arguments.createMap();
        params.putBoolean("initialized", true);
        promise.resolve(params);
    }

    private void clearIncomingNotification(CallInvite callInvite) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "clearIncomingNotification() callInvite state: " + callInvite.getState());
        }
        if (callInvite != null && callInvite.getCallSid() != null) {
            // remove incoming call notification
            String notificationKey = INCOMING_NOTIFICATION_PREFIX + callInvite.getCallSid();
            int notificationId = 0;
            if (TwilioClientModule.callNotificationMap.containsKey(notificationKey)) {
                notificationId = TwilioClientModule.callNotificationMap.get(notificationKey);
            }
            callNotificationManager.removeIncomingCallNotification(getReactApplicationContext(), null, notificationId);
            TwilioClientModule.callNotificationMap.remove(notificationKey);
        }
//        activeCallInvite = null;
    }

    /*
     * Register your FCM token with Twilio to receive incoming call invites
     *
     * If a valid google-services.json has not been provided or the FirebaseInstanceId has not been
     * initialized the fcmToken will be null.
     *
     * In the case where the FirebaseInstanceId has not yet been initialized the
     * VoiceFirebaseInstanceIDService.onTokenRefresh should result in a LocalBroadcast to this
     * activity which will attempt registerForCallInvites again.
     *
     */
    private void registerForCallInvites() {
        FirebaseApp.initializeApp(getReactApplicationContext());
        final String fcmToken = FirebaseInstanceId.getInstance().getToken();
        if (fcmToken != null) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Registering with FCM");
            }
            Voice.register(getReactApplicationContext(), accessToken, Voice.RegistrationChannel.FCM, fcmToken, registrationListener);
        }
    }

    private void setAudioFocus() {
        if (audioManager == null) {
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
                        public void onAudioFocusChange(int i) {
                        }
                    })
                    .build();
            audioManager.requestAudioFocus(focusRequest);
        } else {
            audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            );
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
        if (!ActivityCompat.shouldShowRequestPermissionRationale(getCurrentActivity(), Manifest.permission.RECORD_AUDIO)) {
            ActivityCompat.requestPermissions(getCurrentActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION_REQUEST_CODE);
        }
    }

    // **** TwilioClient public methods ****

    @ReactMethod
    public void accept() {
        callAccepted = true;
        SoundPoolManager.getInstance(getReactApplicationContext()).stopRinging();
        if (activeCallInvite != null) {
            if (activeCallInvite.getState() == CallInvite.State.PENDING) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "accept() activeCallInvite.getState() PENDING");
                }
                activeCallInvite.accept(getReactApplicationContext(), callListener);
                clearIncomingNotification(activeCallInvite);
            } else {
                // when the user answers a call from a notification before the react-native App
                // is completely initialised, and the first event has been skipped
                // re-send connectionDidConnect message to JS
                WritableMap params = Arguments.createMap();
                params.putString("call_sid", activeCallInvite.getCallSid());
                params.putString("call_from", activeCallInvite.getFrom());
                params.putString("call_to", activeCallInvite.getTo());
                params.putString("call_state", activeCallInvite.getState().name());
                callNotificationManager.createHangupLocalNotification(getReactApplicationContext(),
                        activeCallInvite.getCallSid(),
                        activeCallInvite.getFrom());
                eventManager.sendEvent(EVENT_CONNECTION_DID_CONNECT, params);
            }
        } else {
            eventManager.sendEvent(EVENT_CONNECTION_DID_DISCONNECT);
        }
    }

    @ReactMethod
    public void acceptVri() {
        WritableMap params = Arguments.createMap();
        params.putString("session", dictionaryPayload.get("session"));
        params.putString("mode", dictionaryPayload.get("mode"));
        params.putString("displayName", dictionaryPayload.get("displayName"));
        params.putString("companyUuid", dictionaryPayload.get("companyUuid"));
        params.putString("reservationSid", dictionaryPayload.get("reservationSid"));
        params.putString("vriToken", dictionaryPayload.get("vriToken"));
        eventManager.sendEvent(EVENT_VOIP_REMOTE_NOTIFICATION_RECEIVED, params);
    }

    @ReactMethod
    public void reject() {
        callAccepted = false;
        SoundPoolManager.getInstance(getReactApplicationContext()).stopRinging();
        WritableMap params = Arguments.createMap();
        if (activeCallInvite != null) {
            params.putString("call_sid", activeCallInvite.getCallSid());
            params.putString("call_from", activeCallInvite.getFrom());
            params.putString("call_to", activeCallInvite.getTo());
            params.putString("call_state", activeCallInvite.getState().name());
            activeCallInvite.reject(getReactApplicationContext());
            clearIncomingNotification(activeCallInvite);
        }
        eventManager.sendEvent(EVENT_CONNECTION_DID_DISCONNECT, params);
    }

    @ReactMethod
    public void ignore() {
        callAccepted = false;
        SoundPoolManager.getInstance(getReactApplicationContext()).stopRinging();
        WritableMap params = Arguments.createMap();
        if (activeCallInvite != null) {
            params.putString("call_sid", activeCallInvite.getCallSid());
            params.putString("call_from", activeCallInvite.getFrom());
            params.putString("call_to", activeCallInvite.getTo());
            params.putString("call_state", activeCallInvite.getState().name());
            clearIncomingNotification(activeCallInvite);
        }
        eventManager.sendEvent(EVENT_CONNECTION_DID_DISCONNECT, params);
    }

    @ReactMethod
    public void connect(ReadableMap params) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "connect params: " + params);
        }
        WritableMap errParams = Arguments.createMap();
        if (accessToken == null) {
            errParams.putString("err", "Invalid access token");
            eventManager.sendEvent(EVENT_DEVICE_NOT_READY, errParams);
            return;
        }
        if (params == null) {
            errParams.putString("err", "Invalid parameters");
            eventManager.sendEvent(EVENT_CONNECTION_DID_DISCONNECT, errParams);
            return;
        } else if (!params.hasKey("To")) {
            errParams.putString("err", "Invalid To parameter");
            eventManager.sendEvent(EVENT_CONNECTION_DID_DISCONNECT, errParams);
            return;
        }
        toNumber = params.getString("To");
        if (params.hasKey("ToName")) {
            toName = params.getString("ToName");
        }
        // optional parameter that will be delivered to the server
        if (params.hasKey("CallerId")) {
            twiMLParams.put("CallerId", params.getString("CallerId"));
        }
        twiMLParams.put("To", params.getString("To"));
        activeCall = Voice.call(getReactApplicationContext(), accessToken, twiMLParams, callListener);
    }

    @ReactMethod
    public void disconnect() {
        if (activeCall != null) {
            activeCall.disconnect();
            activeCall = null;
        }
    }

    @ReactMethod
    public void setMuted(Boolean muteValue) {
        if (activeCall != null) {
            activeCall.mute(muteValue);
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
                Log.d(TAG, "Active call found state = " + activeCall.getState());
            }
            WritableMap params = Arguments.createMap();
            params.putString("call_sid", activeCall.getSid());
            params.putString("call_from", activeCall.getFrom());
            params.putString("call_to", activeCall.getTo());
            params.putString("call_state", activeCall.getState().name());
            promise.resolve(params);
            return;
        }
        if (activeCallInvite != null) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Active call invite found state = " + activeCallInvite.getState());
            }
            WritableMap params = Arguments.createMap();
            params.putString("call_sid", activeCallInvite.getCallSid());
            params.putString("call_from", activeCallInvite.getFrom());
            params.putString("call_to", activeCallInvite.getTo());
            params.putString("call_state", activeCallInvite.getState().name());
            promise.resolve(params);
            return;
        }
        promise.resolve(null);
    }

    @ReactMethod
    public void setSpeakerPhone(Boolean value) {
        audioManager.setSpeakerphoneOn(value);
    }

    @ReactMethod
    public void sendMessage(String message) {
    }

    @ReactMethod
    public void getDeviceToken(Promise promise) {
        promise.resolve("");
    }

    @ReactMethod
    public void deviceReadyForCalls() {
    }

    // *** End TwilioClient public methods ****

}
