package com.ngs.react.RNTwilioVideo;

import android.app.*;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.WindowManager;
import com.facebook.react.bridge.ReactApplicationContext;
import com.ngs.react.BuildConfig;
import com.ngs.react.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.Context.ACTIVITY_SERVICE;
import static com.ngs.react.RNTwilioVideo.VideoConstants.*;


public class CallNotificationManager {

    private static final String TAG = "RNTwilioVideo";
    private static final String VIDEO_CHANNEL = "video";

    private static Map<String, Integer> callNotificationMap = new HashMap<>();

    private NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

    public CallNotificationManager() {
    }

    public int getApplicationImportance(ReactApplicationContext context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        if (activityManager == null) {
            return 0;
        }
        List<ActivityManager.RunningAppProcessInfo> processInfos = activityManager.getRunningAppProcesses();
        if (processInfos == null) {
            return 0;
        }

        for (ActivityManager.RunningAppProcessInfo processInfo : processInfos) {
            if (processInfo.processName.equals(context.getApplicationInfo().packageName)) {
                return processInfo.importance;
            }
        }
        return 0;
    }

    public Class getMainActivityClass(ReactApplicationContext context) {
        String packageName = context.getPackageName();
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        String className = launchIntent.getComponent().getClassName();
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Intent getLaunchIntent(ReactApplicationContext context,
                                  Integer notificationId,
                                  VideoCallInvite callInvite,
                                  Boolean shouldStartNewTask,
                                  int appImportance
    ) {
        Intent launchIntent = new Intent(context, getMainActivityClass(context));

        int launchFlag = Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP;
        if (shouldStartNewTask || appImportance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
            launchFlag = Intent.FLAG_ACTIVITY_NEW_TASK;
        }

        launchIntent.setAction(ACTION_INCOMING_CALL)
//                .putExtra(INCOMING_CALL_NOTIFICATION_ID, notificationId)
                .addFlags(
                        launchFlag +
                                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED +
                                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD +
                                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON +
                                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                );

        if (callInvite != null) {
            launchIntent.putExtra(INCOMING_CALL_INVITE, callInvite);
        }
        return launchIntent;
    }

    public void createIncomingCallNotification(ReactApplicationContext context,
                                               VideoCallInvite callInvite,
                                               int notificationId,
                                               Intent launchIntent) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "createIncomingCallNotification intent " + launchIntent.getFlags());
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        /*
         * Pass the notification id and call sid to use as an identifier to cancel the
         * notification later
         */
        Bundle extras = new Bundle();
        extras.putInt(INCOMING_CALL_NOTIFICATION_ID, notificationId);
        extras.putString(SESSION, callInvite.getSession());
        extras.putString(NOTIFICATION_TYPE, ACTION_INCOMING_CALL);
        /*
         * Create the notification shown in the notification drawer
         */
        initCallNotificationsChannel(context, notificationManager);

        Uri ringtoneSound = Uri.parse(
                ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/" + R.raw.incoming
        );

//        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
//                ? new NotificationCompat.Builder(context, VIDEO_CHANNEL)
//                : new NotificationCompat.Builder(context)

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, VIDEO_CHANNEL)
                .setSmallIcon(R.drawable.ic_call_white_24dp)
                .setContentTitle("Incoming video call")
                .setContentText(callInvite.getFrom() + " is calling")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(false)
                .setSound(ringtoneSound, AudioManager.STREAM_RING)
                .setExtras(extras)
                .setOngoing(true); // sorted above the regular notifications && do not have an 'X' close button, and are not affected by the "Clear all" button;

        // build notification large icon
        Resources res = context.getResources();
        int largeIconResId = res.getIdentifier("ic_launcher", "mipmap", context.getPackageName());
        Bitmap largeIconBitmap = BitmapFactory.decodeResource(res, largeIconResId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (largeIconResId != 0) {
                notificationBuilder.setLargeIcon(largeIconBitmap);
            }
        }

        // Reject action
        Intent rejectIntent = new Intent(ACTION_REJECT_CALL)
                .putExtra(INCOMING_CALL_NOTIFICATION_ID, notificationId)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingRejectIntent = PendingIntent.getBroadcast(context, 1, rejectIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.addAction(0, "DISMISS", pendingRejectIntent);

        // Answer action
        PendingIntent pendingAnswerIntent;
        int importance = getApplicationImportance(context);

        if (importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
            // If the app is already in the foreground broadcast a notification so that an event is sent to the JS part
            Intent answerIntent = new Intent(ACTION_ANSWER_CALL);
            answerIntent
                    .putExtra(INCOMING_CALL_NOTIFICATION_ID, notificationId)
                    .putExtras(extras)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            pendingAnswerIntent = PendingIntent.getBroadcast(context, 0, answerIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            // Start the app from the notification if it is in the background
            Class clazz = getMainActivityClass(context);
            Intent answerIntent = new Intent(context, clazz);
            answerIntent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            pendingAnswerIntent = PendingIntent.getActivity(
                    context,
                    0,
                    answerIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
        }

        notificationBuilder.addAction(R.drawable.ic_call_white_24dp, "ANSWER", pendingAnswerIntent);

        Notification notification = notificationBuilder.build();
        notification.flags |= Notification.FLAG_INSISTENT; // keep the phone ringing
        notification.sound = ringtoneSound; // fix no sound in older android versions

        notificationManager.notify(notificationId, notification);

        Log.d(TAG, "Creating notification with id: " + notificationId);

        CallNotificationManager.callNotificationMap.put(INCOMING_NOTIFICATION_PREFIX + callInvite.getSession(), notificationId);
    }

    public void initCallNotificationsChannel(Context context, NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }

        Uri ringtoneSound = Uri.parse(
                ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/" + R.raw.incoming
        );

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build();

        NotificationChannel channel = new NotificationChannel(VIDEO_CHANNEL,
                "On Demand video calls", NotificationManager.IMPORTANCE_HIGH);
        channel.setLightColor(Color.GREEN);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.setSound(ringtoneSound, audioAttributes);
        channel.setImportance(NotificationManager.IMPORTANCE_HIGH);
        notificationManager.createNotificationChannel(channel);
    }

    public void createMissedCallNotification(ReactApplicationContext context, VideoCallInvite callInvite) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor sharedPrefEditor = sharedPref.edit();

        /*
         * Create a PendingIntent to specify the action when the notification is
         * selected in the notification drawer
         */
        Intent intent = new Intent(context, getMainActivityClass(context));
        intent.setAction(ACTION_MISSED_CALL)
                .putExtra(INCOMING_CALL_NOTIFICATION_ID, MISSED_CALLS_NOTIFICATION_ID)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent clearMissedCallsCountIntent = new Intent(ACTION_CLEAR_MISSED_CALLS_COUNT)
                .putExtra(INCOMING_CALL_NOTIFICATION_ID, CLEAR_MISSED_CALLS_NOTIFICATION_ID);
        PendingIntent clearMissedCallsCountPendingIntent = PendingIntent.getBroadcast(context, 0, clearMissedCallsCountIntent, 0);
        /*
         * Pass the notification id and call sid to use as an identifier to open the notification
         */
        Bundle extras = new Bundle();
        extras.putInt(INCOMING_CALL_NOTIFICATION_ID, MISSED_CALLS_NOTIFICATION_ID);
        extras.putString(SESSION, callInvite.getSession());
        extras.putString(NOTIFICATION_TYPE, ACTION_MISSED_CALL);

        /*
         * Create the notification shown in the notification drawer
         */
        NotificationCompat.Builder notification =
                new NotificationCompat.Builder(context, VIDEO_CHANNEL)
                        .setGroup(MISSED_CALLS_GROUP)
                        .setGroupSummary(true)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .setSmallIcon(R.drawable.ic_call_missed_white_24dp)
                        .setContentTitle("Missed call")
                        .setContentText(callInvite.getFrom() + " called")
                        .setAutoCancel(true)
                        .setShowWhen(true)
                        .setExtras(extras)
                        .setDeleteIntent(clearMissedCallsCountPendingIntent)
                        .setContentIntent(pendingIntent);

        int missedCalls = sharedPref.getInt(MISSED_CALLS_GROUP, 0);
        missedCalls++;
        if (missedCalls == 1) {
            inboxStyle = new NotificationCompat.InboxStyle();
            inboxStyle.setBigContentTitle("Missed call");
        } else {
            inboxStyle.setBigContentTitle(String.valueOf(missedCalls) + " missed calls");
        }
        inboxStyle.addLine("from: " + callInvite.getFrom());
        sharedPrefEditor.putInt(MISSED_CALLS_GROUP, missedCalls);
        sharedPrefEditor.commit();

        notification.setStyle(inboxStyle);

        // build notification large icon
        Resources res = context.getResources();
        int largeIconResId = res.getIdentifier("ic_launcher", "mipmap", context.getPackageName());
        Bitmap largeIconBitmap = BitmapFactory.decodeResource(res, largeIconResId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && largeIconResId != 0) {
            notification.setLargeIcon(largeIconBitmap);
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(MISSED_CALLS_NOTIFICATION_ID, notification.build());
    }

    public void createHangupLocalNotification(ReactApplicationContext context, String callSid, String caller) {
        PendingIntent pendingHangupIntent = PendingIntent.getBroadcast(
                context,
                0,
                new Intent(ACTION_HANGUP_CALL).putExtra(INCOMING_CALL_NOTIFICATION_ID, HANGUP_NOTIFICATION_ID),
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        Intent launchIntent = new Intent(context, getMainActivityClass(context));
        launchIntent.setAction(ACTION_INCOMING_CALL)
                .putExtra(INCOMING_CALL_NOTIFICATION_ID, HANGUP_NOTIFICATION_ID)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent activityPendingIntent = PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        /*
         * Pass the notification id and call sid to use as an identifier to cancel the
         * notification later
         */
        Bundle extras = new Bundle();
        extras.putInt(INCOMING_CALL_NOTIFICATION_ID, HANGUP_NOTIFICATION_ID);
        extras.putString(SESSION, callSid);
        extras.putString(NOTIFICATION_TYPE, ACTION_HANGUP_CALL);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, VIDEO_CHANNEL)
                .setContentTitle("Call in progress")
                .setContentText(caller)
                .setSmallIcon(R.drawable.ic_call_white_24dp)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setOngoing(true)
                .setUsesChronometer(true)
                .setExtras(extras)
                .setContentIntent(activityPendingIntent);

        notification.addAction(0, "HANG UP", pendingHangupIntent);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // Create notifications channel (required for API > 25)
        initCallNotificationsChannel(context, notificationManager);
        notificationManager.notify(HANGUP_NOTIFICATION_ID, notification.build());
    }

    public void removeIncomingCallNotification(ReactApplicationContext context,
                                               CallInvite callInvite) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "removeIncomingCallNotification");
        }
        if (context == null) {
            Log.e(TAG, "Context is null");
            return;
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Integer notificationId = callNotificationMap.get(INCOMING_NOTIFICATION_PREFIX + callInvite.getSession());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (callInvite != null && notificationId != null) {
                /*
                 * If the incoming call message was cancelled then remove the notification by matching
                 * it with the call sid from the list of notifications in the notification drawer.
                 */
                StatusBarNotification[] activeNotifications = notificationManager.getActiveNotifications();
                for (StatusBarNotification statusBarNotification : activeNotifications) {
                    Notification notification = statusBarNotification.getNotification();
                    String notificationType = notification.extras.getString(NOTIFICATION_TYPE);

                    if (callInvite.getSession().equals(notification.extras.getString(SESSION)) &&
                            notificationType != null && notificationType.equals(ACTION_INCOMING_CALL)) {

//                        int notificationId = notification.extras.getInt(INCOMING_CALL_NOTIFICATION_ID);
                        Log.d(TAG, "Removing (1) notification with id: " + notificationId);
                        notificationManager.cancel(notificationId);
                    }
                }
            } else if (notificationId != null) {
                Log.d(TAG, "Removing (2) notification with id: " + notificationId);
                notificationManager.cancel(notificationId);
            }
        } else {
            if (notificationId != null) {
                Log.d(TAG, "Removing (3) notification with id: " + notificationId);
                notificationManager.cancel(notificationId);
            } else if (callInvite != null) {
                String notificationKey = INCOMING_NOTIFICATION_PREFIX + callInvite.getSession();
                if (callNotificationMap.containsKey(notificationKey)) {
                    notificationId = callNotificationMap.get(notificationKey);
                    Log.d(TAG, "Removing (4) notification with id: " + notificationId);
                    notificationManager.cancel(notificationId);
                    callNotificationMap.remove(notificationKey);
                }
            }
        }
    }

    public void removeHangupNotification(ReactApplicationContext context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(HANGUP_NOTIFICATION_ID);
    }
}
