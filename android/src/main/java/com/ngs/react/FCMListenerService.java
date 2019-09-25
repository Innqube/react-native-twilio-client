package com.ngs.react;

import android.util.Log;
import com.facebook.react.bridge.ReactApplicationContext;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.ngs.react.RNNotifications.NotificationsModule;

public class FCMListenerService extends FirebaseMessagingService {

    private static final String LOG_TAG = "[Twi-Push]";

    public FCMListenerService() {
        Log.d(LOG_TAG, "FCM Listener service instantiated");
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

        ReactApplicationContext rac = (ReactApplicationContext) getApplicationContext();
        NotificationsModule notificationsModule = rac.getNativeModule(NotificationsModule.class);
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
