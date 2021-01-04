package com.ngs.react.RNTwilioVoice;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.firebase.messaging.RemoteMessage;
import com.ngs.react.BuildConfig;
import com.twilio.voice.CancelledCallInvite;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import static com.ngs.react.RNTwilioVoice.TwilioVoiceModule.*;

public class VoiceMessagingService extends Service {

    private static final String TAG = "RNTwilioVoice";
    private CallNotificationManager callNotificationManager;
    private ServiceHandler handler;

    private final class ServiceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "Handling message");
            onMessageReceived(new RemoteMessage(msg.getData()));
            Log.d(TAG, "About to stop VoiceFirebaseMessagingService");
            stopSelf(msg.arg1);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "VoiceFirebaseMessagingService created");
        callNotificationManager = new CallNotificationManager();
        handler = new ServiceHandler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        Message msg = handler.obtainMessage();
        msg.arg1 = startId;
        msg.setData(intent.getExtras());
        handler.sendMessage(msg);

        return START_NOT_STICKY;
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Bundle data: " + remoteMessage.getData());
        }

        Log.d(TAG, "onMessageReceived");

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Map<String, String> data = remoteMessage.getData();
            String action = data.get("action");

            if (action == null) {
                return;
            }

            switch (action) {
                case "call":
                    handleIncomingCallNotification(
                            VoiceCallInvite.create(data)
                    );
                    break;
                case "cancel":
                    handleCancelCallNotification(
                            VoiceCallInvite.create(data)
                    );
                    break;
                case "reject":
                    break;
            }

//            // If notification ID is not provided by the user for push notification, generate one at random
//            boolean valid = Voice.handleMessage(getApplicationContext(), data, new MessageListener() {
//                @Override
//                public void onCallInvite(final CallInvite callInvite) {
//                    Log.d(TAG, "onCallInvite");
//                    // We need to run this on the main thread, as the React code assumes that is true.
//                    // Namely, DevServerHelper constructs a Handler() without a Looper, which triggers:
//                    // "Can't create handler inside thread that has not called Looper.prepare()"
//                    Handler handler = new Handler(Looper.getMainLCallInviteooper());
//                    handler.post(() -> {
//                        ReactInstanceManager mReactInstanceManager = ((ReactApplication) getApplication()).getReactNativeHost().getReactInstanceManager();
//                        ReactContext context = mReactInstanceManager.getCurrentReactContext();
//
//                        if (context == null) {
//                            mReactInstanceManager.addReactInstanceEventListener(rc -> broadcastIncomingCallNotification(callInvite));
//                            mReactInstanceManager.createReactContextInBackground();
//                        } else {
//                            broadcastIncomingCallNotification(callInvite);
//                        }
//                    });
//                }
//
//                @Override
//                public void onCancelledCallInvite(@NonNull CancelledCallInvite cancelledCallInvite, @Nullable CallException callException) {
//                    Log.d(TAG, "onCancelledCallInvite");
//                    Handler handler = new Handler(Looper.getMainLooper());
//                    handler.post(() -> VoiceMessagingService.this.sendCancelledCallInviteToActivity(cancelledCallInvite));
//                }
//            });
//
//            if (!valid) {
//                Log.e(TAG, "The message was not a valid Twilio Voice SDK payload: " + remoteMessage.getData());
//
//                String action = data.get("action");
//
//                if ("reject".equals(action)) {
//                    Log.d(TAG, "Rejecting call");
//                    int notificationId = Integer.parseInt(data.get(INCOMING_CALL_NOTIFICATION_ID));
//                    callNotificationManager.removeNotification(getApplicationContext(), notificationId);
//                }
//            }
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.e(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }
    }

    private void handleIncomingCallNotification(VoiceCallInvite invite) {
        String taskAttributesString = invite.getTaskAttributes();

        if (taskAttributesString == null) {
            Log.d(TAG, "no task attributes to cancel call");
            return;
        }

        String teamSession;
        try {
            JSONObject taskAttributes = new JSONObject(taskAttributesString);
            teamSession = taskAttributes.getString("teamSession");
            Log.d(TAG, "teamSession: " + teamSession);
        } catch (JSONException ex) {
            Log.w(TAG, "No session found. Can not create incoming call notification. Invite data: " + invite);
            return;
        }

        int notificationId = teamSession.hashCode();
        callNotificationManager.createIncomingCallNotification(
                getApplicationContext(),
                invite,
                notificationId
        );

        Intent intent = new Intent(ACTION_INCOMING_CALL);
        intent.putExtra(INCOMING_CALL_NOTIFICATION_ID, notificationId);
        intent.putExtra(INCOMING_CALL_INVITE, invite);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private void handleCancelCallNotification(VoiceCallInvite invite) {
        String taskAttributesString = invite.getTaskAttributes();

        if (taskAttributesString == null) {
            Log.d(TAG, "no task attributes to cancel call");
            return;
        }

        String teamSession;
        try {
            JSONObject taskAttributes = new JSONObject(taskAttributesString);
            teamSession = taskAttributes.getString("teamSession");
            Log.d(TAG, "teamSession: " + teamSession);
        } catch (JSONException ex) {
            Log.w(TAG, "No session found. Can not remove incoming call notification. Invite data: " + invite);
            return;
        }

        int notificationId = teamSession.hashCode();
        callNotificationManager.removeNotification(
                getApplicationContext(),
                notificationId
        );

        Intent intent = new Intent(ACTION_CANCEL_CALL_INVITE);
        intent.putExtra(INCOMING_CALL_NOTIFICATION_ID, notificationId);
        intent.putExtra(CANCELLED_CALL_INVITE, invite);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }


//    private void broadcastIncomingCallNotification(CallInvite callInvite) {
//        int notificationId = callInvite.getCallSid().hashCode();
//        callNotificationManager.createIncomingCallNotification(
//                getApplicationContext(),
//                callInvite,
//                notificationId
//        );
//
//        Intent intent = new Intent(ACTION_INCOMING_CALL);
//        intent.putExtra(INCOMING_CALL_NOTIFICATION_ID, notificationId);
//        intent.putExtra(INCOMING_CALL_INVITE, callInvite);
//        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
//    }

    /*
     * Send the CancelledCallInvite to the TwilioVoiceModule
     */
    private void sendCancelledCallInviteToActivity(CancelledCallInvite cancelledCallInvite) {
//        SoundPoolManager.getInstance((this)).stopRinging();
        Intent intent = new Intent(ACTION_CANCEL_CALL_INVITE);
        intent.putExtra(CANCELLED_CALL_INVITE, cancelledCallInvite);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
