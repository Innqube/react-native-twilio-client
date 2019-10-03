package com.ngs.react.RNTwilioClient.fcm;

import android.annotation.TargetApi;

import android.app.ActivityManager;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.ngs.react.RNTwilioClient.BuildConfig;
import com.ngs.react.RNTwilioClient.CallNotificationManager;
import com.twilio.voice.CallInvite;
import com.twilio.voice.MessageException;
import com.twilio.voice.MessageListener;
import com.twilio.voice.Voice;

import java.util.Map;
import java.util.Random;

import static com.ngs.react.RNTwilioClient.TwilioClientModule.TAG;
import static com.ngs.react.RNTwilioClient.TwilioClientModule.ACTION_INCOMING_CALL;
import static com.ngs.react.RNTwilioClient.TwilioClientModule.INCOMING_CALL_INVITE;
import static com.ngs.react.RNTwilioClient.TwilioClientModule.INCOMING_CALL_NOTIFICATION_ID;

import com.ngs.react.RNTwilioClient.SoundPoolManager;

public class VoiceFirebaseMessagingService extends FirebaseMessagingService {

    private CallNotificationManager callNotificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        callNotificationManager = new CallNotificationManager();
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Bundle data: " + remoteMessage.getData());
        }

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            final Map<String, String> data = remoteMessage.getData();

            // If notification ID is not provided by the user for push notification, generate one at random
            Random randomNumberGenerator = new Random(System.currentTimeMillis());
            final int notificationId = randomNumberGenerator.nextInt();

            // Construct and load our normal React JS code bundle
            final ReactInstanceManager mReactInstanceManager = ((ReactApplication) getApplication()).getReactNativeHost().getReactInstanceManager();
            final ReactContext context = mReactInstanceManager.getCurrentReactContext();

            if (data.get("mode").equals("video")) {

                // If it's constructed, send a notification
                if (context != null) {
                    int appImportance = callNotificationManager.getApplicationImportance((ReactApplicationContext) context);
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "CONTEXT present appImportance = " + appImportance);
                    }
                    Intent launchIntent = callNotificationManager.getLaunchIntent(
                            (ReactApplicationContext) context,
                            notificationId,
                            null,
                            false,
                            appImportance
                    );
                    if (appImportance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        context.startActivity(launchIntent);
                    }

                    callNotificationManager.createIncomingVriCallNotification(context, notificationId, launchIntent, data);
                } else {
                    // Otherwise wait for construction, then handle the incoming call
                    mReactInstanceManager.addReactInstanceEventListener(new ReactInstanceManager.ReactInstanceEventListener() {
                        public void onReactContextInitialized(ReactContext context) {
                            int appImportance = callNotificationManager.getApplicationImportance((ReactApplicationContext) context);
                            if (BuildConfig.DEBUG) {
                                Log.d(TAG, "CONTEXT not present appImportance = " + appImportance);
                            }
                            Intent launchIntent = callNotificationManager.getLaunchIntent((ReactApplicationContext) context, notificationId, null, true, appImportance);
                            context.startActivity(launchIntent);

                            callNotificationManager.createIncomingVriCallNotification(context, notificationId, launchIntent, data);
                        }
                    });
                    if (!mReactInstanceManager.hasStartedCreatingInitialContext()) {
                        mReactInstanceManager.createReactContextInBackground();
                    }
                }



            } else {

                Voice.handleMessage(this, data, new MessageListener() {

                    @Override
                    public void onCallInvite(final CallInvite callInvite) {

                        // We need to run this on the main thread, as the React code assumes that is true.
                        // Namely, DevServerHelper constructs a Handler() without a Looper, which triggers:
                        // "Can't create handler inside thread that has not called Looper.prepare()"
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(new Runnable() {
                            public void run() {

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
                                    VoiceFirebaseMessagingService.this.handleIncomingCall((ReactApplicationContext) context, notificationId, callInvite, launchIntent);
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
                                            VoiceFirebaseMessagingService.this.handleIncomingCall((ReactApplicationContext) context, notificationId, callInvite, launchIntent);
                                        }
                                    });
                                    if (!mReactInstanceManager.hasStartedCreatingInitialContext()) {
                                        // Construct it in the background
                                        mReactInstanceManager.createReactContextInBackground();
                                    }
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(MessageException messageException) {
                        Log.e(TAG, "Error handling FCM message" + messageException.toString());
                    }
                });
            }
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.e(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }
    }

    private void handleIncomingCall(ReactApplicationContext context,
                                    int notificationId,
                                    CallInvite callInvite,
                                    Intent launchIntent
    ) {
        sendIncomingCallMessageToActivity(context, callInvite, notificationId);
        showNotification(context, callInvite, notificationId, launchIntent);
    }

    /*
     * Send the IncomingCallMessage to the TwilioClientModule
     */
    private void sendIncomingCallMessageToActivity(
            ReactApplicationContext context,
            CallInvite callInvite,
            int notificationId
    ) {
        Intent intent = new Intent(ACTION_INCOMING_CALL);
        intent.putExtra(INCOMING_CALL_NOTIFICATION_ID, notificationId);
        intent.putExtra(INCOMING_CALL_INVITE, callInvite);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /*
     * Show the notification in the Android notification drawer
     */
    @TargetApi(20)
    private void showNotification(ReactApplicationContext context,
                                  CallInvite callInvite,
                                  int notificationId,
                                  Intent launchIntent
    ) {
        if (callInvite != null && callInvite.getState() == CallInvite.State.PENDING) {
            callNotificationManager.createIncomingCallNotification(context, callInvite, notificationId, launchIntent);
        } else {
            SoundPoolManager.getInstance(context.getBaseContext()).stopRinging();
            callNotificationManager.removeIncomingCallNotification(context, callInvite, 0);
        }
    }
}
