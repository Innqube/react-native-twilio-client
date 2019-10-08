package com.ngs.react;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.ngs.react.RNTwilioClient.R;

public class FCMListenerService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "II";
    private TwilioChatTokenService tts;
    private static final String LOG_TAG = "[Twi-Push]";
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(LOG_TAG, "FCMListenerService -> TwilioTokenService connected");

            TwilioChatTokenService.TwilioFCMListenerBinder binder = (TwilioChatTokenService.TwilioFCMListenerBinder) iBinder;
            tts = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(LOG_TAG, "FCMListenerService -> TwilioTokenService disconnected");
            tts = null;
        }
    };

    public FCMListenerService() {
        Log.d(LOG_TAG, "FCM Listener service instantiated");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();

        Log.d(LOG_TAG, "Starting twilio chat token service");
        Intent ttsIntent = new Intent(getApplicationContext(), TwilioChatTokenService.class);
        startService(ttsIntent);

        Intent intent = new Intent(getApplicationContext(), TwilioChatTokenService.class);
        getApplicationContext().bindService(intent, this.serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(LOG_TAG, "On new token: " + token);

        if (tts != null) {
            tts.setToken(token);
        } else {
            Log.w(LOG_TAG, "Twilio token service was null!");
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(LOG_TAG, "Received push notification: " + remoteMessageToString(remoteMessage));

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_call_black_24dp)
                .setContentTitle(remoteMessage.getData().get("ii_author"))
                .setContentText("ii_body")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify((int)(Math.random() * 1000), builder.build());
    }

    private String remoteMessageToString(RemoteMessage msg) {
        return "RemoteMessage[from:" + msg.getFrom() + ", " +
                "messageId: " + msg.getMessageId() + ", " +
                "messageType: " + msg.getMessageType() + ", " +
                "to: " + msg.getTo() + ", " +
                "data: " + (msg.getData() == null ? "null" : msg.getData().toString()) + "]";
    }

    private void createNotificationChannel() {
        Log.d(LOG_TAG, "Creating notification channel");
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Interpreter Intelligence";
            String description = "Interpreter Intelligence notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }


}
