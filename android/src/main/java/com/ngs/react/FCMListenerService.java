package com.ngs.react;

import android.util.Log;
import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.ReactContext;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.ngs.react.RNNotifications.NotificationsModule;

public class FCMListenerService extends FirebaseMessagingService {

    private static final String LOG_TAG = "[Twi-Push]";
    private ReactContext reactContext;

    public FCMListenerService() {
        Log.d(LOG_TAG, "FCM Listener service instantiated");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        final ReactInstanceManager mReactInstanceManager = ((ReactApplication) getApplication()).getReactNativeHost().getReactInstanceManager();
        mReactInstanceManager.addReactInstanceEventListener(validContext -> reactContext = validContext);
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(LOG_TAG, "On new token: " + token);
        TokenHolder.get().setToken(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(LOG_TAG, "Received push notification: " + remoteMessageToString(remoteMessage));

        NotificationsModule notificationsModule = reactContext.getNativeModule(NotificationsModule.class);
        notificationsModule.sendNotification(remoteMessage);
    }

    private String remoteMessageToString(RemoteMessage msg) {
        return "RemoteMessage[from:" + msg.getFrom() + ", " +
                "messageId: " + msg.getMessageId() + ", " +
                "messageType: " + msg.getMessageType() + ", " +
                "to: " + msg.getTo() + ", " +
                "data: " + (msg.getData() == null ? "null" : msg.getData().toString()) + "]";
    }


}
