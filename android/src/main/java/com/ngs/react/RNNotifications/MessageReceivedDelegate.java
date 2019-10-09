package com.ngs.react.RNNotifications;

import com.google.firebase.messaging.RemoteMessage;

import java.io.Serializable;

public interface MessageReceivedDelegate extends Serializable {

    void createNotificationChannel();

    void onMessageReceived(RemoteMessage remoteMessage);

}
