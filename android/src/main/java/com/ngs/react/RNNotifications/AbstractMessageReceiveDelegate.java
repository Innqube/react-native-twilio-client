package com.ngs.react.RNNotifications;

import android.app.NotificationManager;
import android.content.Context;

public abstract class AbstractMessageReceiveDelegate implements MessageReceivedDelegate {

    private NotificationManager notificationManager;
    private Context context;

    public AbstractMessageReceiveDelegate(Context context, NotificationManager notificationManager) {
        this.notificationManager = notificationManager;
        this.context = context;
    }

    @Override
    public NotificationManager getNotificationManager() {
        return notificationManager;
    }

    public void setNotificationManager(NotificationManager notificationManager) {
        this.notificationManager = notificationManager;
    }

    @Override
    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public void createNotificationGroup() {

    }
}
