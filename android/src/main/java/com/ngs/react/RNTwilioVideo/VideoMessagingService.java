package com.ngs.react.RNTwilioVideo;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.google.firebase.messaging.RemoteMessage;
import com.ngs.react.BuildConfig;

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

        return START_STICKY;
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
            Map<String, String> data = remoteMessage.getData();

            // If notification ID is not provided by the user for push notification, generate one at random
            Random randomNumberGenerator = new Random(System.currentTimeMillis());
            final int notificationId = randomNumberGenerator.nextInt();

//            Handler handler = new Handler(Looper.getMainLooper());
//            handler.post(new Runnable() {
//                public void run() {
                    // Construct and load our normal React JS code bundle
                    ReactInstanceManager mReactInstanceManager = ((ReactApplication) getApplication()).getReactNativeHost().getReactInstanceManager();
                    ReactContext context = mReactInstanceManager.getCurrentReactContext();
                    VideoCallInvite callInvite = VideoCallInvite.create(remoteMessage.getData());
                    // If it's constructed, send a notification
                    if (context != null) {
                        int appImportance = callNotificationManager.getApplicationImportance((ReactApplicationContext) context);
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "CONTEXT present appImportance = " + appImportance);
                        }
                        Intent launchIntent = callNotificationManager.getLaunchIntent(
                                (ReactApplicationContext) context,
                                notificationId,
                                callInvite,
                                false,
                                appImportance
                        );
                        // app is not in foreground
                        if (appImportance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                            context.startActivity(launchIntent);
                        }
                        Intent intent = new Intent(ACTION_INCOMING_CALL);
                        intent.putExtra(INCOMING_CALL_NOTIFICATION_ID, notificationId);
                        intent.putExtra(INCOMING_CALL_INVITE, callInvite);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                    } else {
                        // Otherwise wait for construction, then handle the incoming call
                        mReactInstanceManager.addReactInstanceEventListener(new ReactInstanceManager.ReactInstanceEventListener() {
                            public void onReactContextInitialized(ReactContext context) {
                                int appImportance = callNotificationManager.getApplicationImportance((ReactApplicationContext) context);
                                if (BuildConfig.DEBUG) {
                                    Log.d(TAG, "CONTEXT not present appImportance = " + appImportance);
                                }
                                Intent launchIntent = callNotificationManager.getLaunchIntent((ReactApplicationContext) context, notificationId, callInvite, true, appImportance);
                                context.startActivity(launchIntent);
                                Intent intent = new Intent(ACTION_INCOMING_CALL);
                                intent.putExtra(INCOMING_CALL_NOTIFICATION_ID, notificationId);
                                intent.putExtra(INCOMING_CALL_INVITE, callInvite);
                                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                                callNotificationManager.createIncomingCallNotification(
                                        (ReactApplicationContext) context, callInvite, notificationId,
                                        launchIntent);
                            }
                        });
                        if (!mReactInstanceManager.hasStartedCreatingInitialContext()) {
                            // Construct it in the background
                            mReactInstanceManager.createReactContextInBackground();
                        }
                    }
//                }
//            });
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.e(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }
    }

    /*
     * Send the CancelledCallInvite to the TwilioVideoModule
     */
//    private void sendCancelledCallInviteToActivity(CancelledCallInvite cancelledCallInvite) {
//        SoundPoolManager.getInstance((this)).stopRinging();
//        Intent intent = new Intent(ACTION_CANCEL_CALL_INVITE);
//        intent.putExtra(CANCELLED_CALL_INVITE, cancelledCallInvite);
//        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
//    }
}
