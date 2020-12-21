package com.ngs.react.RNTwilioVideo;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.facebook.react.ReactApplication;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.google.firebase.messaging.RemoteMessage;
import com.ngs.react.BuildConfig;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static com.ngs.react.RNTwilioVideo.VideoConstants.*;

public class VideoMessagingService extends Service {

    private static final String TAG = "RNTwilioVideo";
    private CallNotificationManager callNotificationManager;
    private ServiceHandler handler;

    private final class ServiceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "Handling message");
            onMessageReceived(new RemoteMessage(msg.getData()));
            Log.d(TAG, "About to stop VideoFirebaseMessagingService");
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
        Log.d(TAG, "VideoFirebaseMessagingService created");
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

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            ReactContext context  = ((ReactApplication) getApplication())
                    .getReactNativeHost()
                    .getReactInstanceManager()
                    .getCurrentReactContext();

            Map<String, String> data = parseData(remoteMessage);
            String action = data.get("action");

            if ("call".equals(action)) {
                handleIncomingCallNotification(context, data);
            } else if ("cancel".equals(action)) {
                cancelIncomingCallNotification(context, data);
            }
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.e(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }
    }

    private Map<String, String> parseData(RemoteMessage remoteMessage) {
        Map<String, String> remoteMessageData = remoteMessage.getData();
        Map<String, String> data = new HashMap<>();
        for (String key : remoteMessageData.keySet()) {
            data.put(key, remoteMessageData.get(key));
        }
        return data;
    }

    private void cancelIncomingCallNotification(ReactContext context, Map<String, String> data) {
        Log.d(TAG, "cancelIncomingCall");
        try {
            String taskAttributesString = data.get("taskAttributes");

            if (taskAttributesString == null) {
                Log.d(TAG, "no task attributes to cancel call");
                return;
            }

            JSONObject taskAttributes = new JSONObject(taskAttributesString);
            Log.d(TAG, "teamSession: " + taskAttributes.getString("teamSession"));
            CanceledVideoCallInvite invite = new CanceledVideoCallInvite(
                    new HashMap<String, String>() {{
                        put("teamSession", taskAttributes.getString("teamSession"));
                    }}
            );

            callNotificationManager.removeIncomingCallNotification(
                    (ReactApplicationContext) context,
                    invite
            );
        } catch (JSONException e) {
            Log.e(TAG, "Couldn't parse TaskAttributes from push notification", e);
            e.printStackTrace();
        }

    }

    private void handleIncomingCallNotification(ReactContext context, Map<String, String> data) {
        // If notification ID is not provided by the user for push notification, generate one at random
        Random randomNumberGenerator = new Random(System.currentTimeMillis());
        final int notificationId = randomNumberGenerator.nextInt();

        VideoCallInvite callInvite = VideoCallInvite.create(data);

        int appImportance = callNotificationManager.getApplicationImportance((ReactApplicationContext) context);
        Intent launchIntent = callNotificationManager.getLaunchIntent(
                (ReactApplicationContext) context,
                notificationId,
                callInvite,
                false,
                appImportance
        );

        callNotificationManager.createIncomingCallNotification(
                (ReactApplicationContext) context, callInvite, notificationId,
                launchIntent);

        Intent intent = new Intent(ACTION_INCOMING_CALL);
        intent.putExtra(INCOMING_CALL_NOTIFICATION_ID, notificationId);
        intent.putExtra(INCOMING_CALL_INVITE, callInvite);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        // If it's constructed, send a notification
//        if (context != null) {
//            int appImportance = callNotificationManager.getApplicationImportance((ReactApplicationContext) context);
//            if (BuildConfig.DEBUG) {
//                Log.d(TAG, "CONTEXT present appImportance = " + appImportance);
//            }
//            Intent launchIntent = callNotificationManager.getLaunchIntent(
//                    (ReactApplicationContext) context,
//                    notificationId,
//                    callInvite,
//                    false,
//                    appImportance
//            );
//
//            callNotificationManager.createIncomingCallNotification(
//                    (ReactApplicationContext) context, callInvite, notificationId,
//                    launchIntent);
//
//            Intent intent = new Intent(ACTION_INCOMING_CALL);
//            intent.putExtra(INCOMING_CALL_NOTIFICATION_ID, notificationId);
//            intent.putExtra(INCOMING_CALL_INVITE, callInvite);
//            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
//        } else {
//            ReactInstanceManager mReactInstanceManager = ((ReactApplication) getApplication()).getReactNativeHost().getReactInstanceManager();
//            // Otherwise wait for construction, then handle the incoming call
//            mReactInstanceManager.addReactInstanceEventListener(new ReactInstanceManager.ReactInstanceEventListener() {
//                public void onReactContextInitialized(ReactContext context) {
//                    Log.d(TAG, "React context created");
//                    int appImportance = callNotificationManager.getApplicationImportance((ReactApplicationContext) context);
//                    if (BuildConfig.DEBUG) {
//                        Log.d(TAG, "CONTEXT not present appImportance = " + appImportance);
//                    }
//                    Intent launchIntent = callNotificationManager.getLaunchIntent((ReactApplicationContext) context, notificationId, callInvite, true, appImportance);
//                    /*context.startActivity(launchIntent);
//                    Intent intent = new Intent(ACTION_INCOMING_CALL);
//                    intent.putExtra(INCOMING_CALL_NOTIFICATION_ID, notificationId);
//                    intent.putExtra(INCOMING_CALL_INVITE, callInvite);
//                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);*/
//                    callNotificationManager.createIncomingCallNotification(
//                            (ReactApplicationContext) context, callInvite, notificationId,
//                            launchIntent);
//                }
//            });
//            if (!mReactInstanceManager.hasStartedCreatingInitialContext()) {
//                // Construct it in the background
//                Log.d(TAG, "Create react context");
//                mReactInstanceManager.createReactContextInBackground();
//            }
//        }
    }

}
