package com.ngs.react.RNTwilioVoice;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import androidx.annotation.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

public class VoiceMessagingService extends Service {

    private static final String TAG = "RNTwilioVoice";
    private CallNotificationManager callNotificationManager;
    private ServiceHandler handler;

    private final class ServiceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "Handling message");
            VoiceCallInvite invite = msg.getData().getParcelable(VoiceConstants.INCOMING_CALL_INVITE);
            String action = msg.getData().getString("action");
            onMessageReceived(action, invite);
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
    public void onMessageReceived(String action, VoiceCallInvite invite) {
        Log.d(TAG, "onMessageReceived");

        // Check if message contains a data payload.
        if (action == null) {
            return;
        }

        switch (action) {
            case "call":
                handleIncomingCallNotification(invite);
                break;
            case "cancel":
                handleCancelCallNotification(invite);
                break;
            case "reject":
                handleRejectCall(invite);
                break;
        }
    }

    private void handleIncomingCallNotification(VoiceCallInvite invite) {
        Log.d(TAG, "handleIncomingCallNotification");
        Integer notificationId = notificationIdFromInvite(invite);

        if (notificationId == null) {
            Log.d(TAG, "Could not create notificationId from invite");
            return;
        }

        callNotificationManager.createIncomingCallNotification(
                getApplicationContext(),
                invite,
                notificationId
        );

        Intent intent = new Intent(VoiceConstants.ACTION_INCOMING_CALL);
        intent.putExtra(VoiceConstants.INCOMING_CALL_INVITE, invite);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private void handleCancelCallNotification(VoiceCallInvite invite) {
        Log.d(TAG, "handleCancelCallNotification");
        removeIncomingCallNotification(invite);

        Intent intent = new Intent(VoiceConstants.ACTION_CANCEL_CALL_INVITE);
        intent.putExtra(VoiceConstants.CANCELLED_CALL_INVITE, invite);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private void handleRejectCall(VoiceCallInvite invite) {
        Log.d(TAG, "handleRejectCall");
        removeIncomingCallNotification(invite);

        Intent intent = new Intent(VoiceConstants.ACTION_REJECT_CALL);
        intent.putExtra(VoiceConstants.REJECTED_CALL_INVITE, invite);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private void removeIncomingCallNotification(VoiceCallInvite invite) {
        Integer notificationId = notificationIdFromInvite(invite);

        if (notificationId != null) {
            callNotificationManager.removeNotification(
                    getApplicationContext(),
                    notificationId
            );
        }
    }

    private Integer notificationIdFromInvite(VoiceCallInvite invite) {
        String taskAttributesString = invite.getTaskAttributes();

        if (taskAttributesString == null) {
            Log.d(TAG, "no task attributes to cancel call");
            return null;
        }

        try {
            JSONObject taskAttributes = new JSONObject(taskAttributesString);
            String teamSession = taskAttributes.getString("teamSession");
            Log.d(TAG, "teamSession: " + teamSession);
            return teamSession.hashCode();
        } catch (JSONException ex) {
            Log.w(TAG, "No session found. Can not remove incoming call notification. Invite data: " + invite);
            return null;
        }
    }

}
