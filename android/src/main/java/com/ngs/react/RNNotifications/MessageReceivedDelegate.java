package com.ngs.react.RNNotifications;

import android.app.NotificationManager;
import android.content.Context;
import com.google.firebase.messaging.RemoteMessage;

import java.io.Serializable;

public interface MessageReceivedDelegate extends Serializable {

    void createNotificationChannel();

    void onMessageReceived(RemoteMessage remoteMessage);

    NotificationManager getNotificationManager();

    Context getContext();

}
