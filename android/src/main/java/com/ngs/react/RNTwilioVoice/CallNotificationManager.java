package com.ngs.react.RNTwilioVoice;

import android.app.*;
import android.content.*;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.ngs.react.R;
import com.ngs.react.RNLocalizedStrings.LocalizedKeys;

import java.util.List;

import static android.content.Context.ACTIVITY_SERVICE;
import static com.ngs.react.RNTwilioVoice.VoiceConstants.ACTION_GO_OFFLINE;
import static com.ngs.react.RNTwilioVoice.VoiceConstants.ACTION_ANSWER_CALL;
import static com.ngs.react.RNTwilioVoice.VoiceConstants.ACTION_CANCEL_CALL_INVITE;
import static com.ngs.react.RNTwilioVoice.VoiceConstants.ACTION_REJECT_CALL;
import static com.ngs.react.RNTwilioVoice.VoiceConstants.INCOMING_CALL_INVITE;
import static com.ngs.react.RNTwilioVoice.VoiceConstants.INCOMING_CALL_NOTIFICATION_ID;

public class CallNotificationManager {

    private static final String VOICE_CHANNEL = "voice";
    private static final String TAG = "RNTwilioVoice";

    public int getApplicationImportance(Context context) {
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

    public Class getMainActivityClass(Context context) {
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

    public void createIncomingCallNotification(Context context,
                                               VoiceCallInvite invite,
                                               int notificationId) {
        Log.d(TAG, "createIncomingCallNotification with id " + notificationId);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        /*
         * Create the notification shown in the notification drawer
         */
        initCallNotificationsChannel(context, notificationManager);

        Uri ringtoneSound = Uri.parse(
                ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/" + R.raw.incoming
        );

        Intent callIntent = new Intent(context, IncomingVoiceCallFullscreenActivity.class);
        callIntent.putExtra(INCOMING_CALL_INVITE, invite);
        callIntent.putExtra(INCOMING_CALL_NOTIFICATION_ID, notificationId);

        PendingIntent pendingCallIntent = PendingIntent.getActivity(
                context,
                0,
                callIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        SharedPreferences sharedPref = context.getSharedPreferences("db", Context.MODE_PRIVATE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, VOICE_CHANNEL)
                .setSmallIcon(R.drawable.ic_call_white_48dp)
                .setContentTitle(sharedPref.getString(LocalizedKeys.INCOMING_VOICE_CALL, "Incoming voice call"))
                .setContentText(invite.getFrom(" / ", sharedPref) + sharedPref.getString(LocalizedKeys.IS_CALLING, "is calling"))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(false)
                .setSound(ringtoneSound, AudioManager.STREAM_RING)
                .setColor(Color.argb(255, 0, 147, 213))
                .setLights(Color.argb(255, 0, 147, 213), 1000, 250)
                .setFullScreenIntent(pendingCallIntent, true)
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

        PendingIntent pendingAnswerIntent = buildAnswerIntent(context, invite, notificationId);
        PendingIntent pendingRejectIntent = buildRejectIntent(context, invite, notificationId);
        PendingIntent pendingGoOfflineIntent = buildGoOfflineIntent(context, invite, notificationId);

        notificationBuilder.addAction(0, sharedPref.getString(LocalizedKeys.DECLINE, "DECLINE"), pendingRejectIntent);
        notificationBuilder.addAction(R.drawable.ic_call_white_48dp, sharedPref.getString(LocalizedKeys.ACCEPT, "ACCEPT"), pendingAnswerIntent);
        notificationBuilder.addAction(1, sharedPref.getString(LocalizedKeys.GO_OFFLINE, "GO OFFLINE"), pendingGoOfflineIntent);

        Notification notification = notificationBuilder.build();
        notification.flags |= Notification.FLAG_INSISTENT; // keep the phone ringing
        notification.sound = ringtoneSound; // fix no sound in older android versions

        wakeUpScreen(context);

        Log.d(TAG, "Creating notification with id: " + notificationId);
        notificationManager.notify(notificationId, notification);
    }

    private void wakeUpScreen(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        boolean isScreenOn = pm.isInteractive();
        Log.d(TAG, "Screen on: " + isScreenOn);
        if (!isScreenOn) {
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "com.ngs:notification-call-lock");
            wl.acquire(3000);
        }
    }

    private PendingIntent buildAnswerIntent(Context context, VoiceCallInvite callInvite, Integer notificationId) {
        PendingIntent pendingIntent;
        if (isInForeground(context)) {
            Log.d(TAG, "buildAnswerIntent app is in foreground");
            // If the app is already in the foreground broadcast a notification so that an event is sent to the JS part
            Intent answerIntent = new Intent(ACTION_ANSWER_CALL)
                    .putExtra(INCOMING_CALL_INVITE, callInvite)
                    .putExtra(INCOMING_CALL_NOTIFICATION_ID, notificationId)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            pendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    answerIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
        } else {
            Log.d(TAG, "buildAnswerIntent app is in background");
            // Start the app from the notification as it is in the background currently
            Class clazz = getMainActivityClass(context);
            Intent answerIntent = new Intent(context, clazz)
                    .putExtra(INCOMING_CALL_INVITE, callInvite)
                    .putExtra(INCOMING_CALL_NOTIFICATION_ID, notificationId)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    answerIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
        }

        return pendingIntent;
    }

    private PendingIntent buildRejectIntent(Context context, VoiceCallInvite callInvite, Integer notificationId) {
        PendingIntent pendingIntent;
        if (isInForeground(context)) {
            Log.d(TAG, "buildRejectIntent app is in background");
            Intent rejectIntent = new Intent(ACTION_REJECT_CALL)
                    .putExtra(INCOMING_CALL_INVITE, callInvite)
                    .putExtra(INCOMING_CALL_NOTIFICATION_ID, notificationId)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            pendingIntent = PendingIntent.getBroadcast(
                    context,
                    1,
                    rejectIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
        } else {
            // Start the app from the notification as it is in the background currently
            Log.d(TAG, "buildRejectIntent app is in background");
            Intent rejectIntent = new Intent(context, VoiceMessagingService.class)
                    .putExtra(INCOMING_CALL_INVITE, callInvite)
                    .putExtra(INCOMING_CALL_NOTIFICATION_ID, notificationId + "")
                    .putExtra("action", "reject")
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            pendingIntent = PendingIntent.getService(
                    context,
                    0,
                    rejectIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
        }
        return pendingIntent;
    }

    private PendingIntent buildGoOfflineIntent(Context context, VoiceCallInvite callInvite, Integer notificationId) {
        PendingIntent pendingIntent;
        if (isInForeground(context)) {
            Log.d(TAG, "buildGoOfflineIntent app is in background");
            Intent rejectIntent = new Intent(ACTION_GO_OFFLINE)
                    .putExtra(INCOMING_CALL_INVITE, callInvite)
                    .putExtra(INCOMING_CALL_NOTIFICATION_ID, notificationId)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            pendingIntent = PendingIntent.getBroadcast(
                    context,
                    1,
                    rejectIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
        } else {
            // Start the app from the notification as it is in the background currently
            Log.d(TAG, "buildGoOfflineIntent app is in background");
            Intent rejectIntent = new Intent(context, VoiceMessagingService.class)
                    .putExtra(INCOMING_CALL_INVITE, callInvite)
                    .putExtra(INCOMING_CALL_NOTIFICATION_ID, notificationId + "")
                    .putExtra("action", "goOffline")
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            pendingIntent = PendingIntent.getService(
                    context,
                    2,
                    rejectIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
        }
        return pendingIntent;
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

        NotificationChannel channel = new NotificationChannel(VOICE_CHANNEL,
                "Primary Voice Channel", NotificationManager.IMPORTANCE_HIGH);
        channel.setLightColor(Color.argb(255, 0, 147, 213));
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.setSound(ringtoneSound, audioAttributes);
        channel.setImportance(NotificationManager.IMPORTANCE_HIGH);
        notificationManager.createNotificationChannel(channel);
    }

    public void removeNotification(Context context, Integer id) {
        Log.d(TAG, "Removing notification with id: " + id);

        if (id != null) {
            Intent intent = new Intent(ACTION_CANCEL_CALL_INVITE);
            intent.putExtra(INCOMING_CALL_NOTIFICATION_ID, id);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(id);
        }
    }

    private boolean isInForeground(Context context) {
        int importance = getApplicationImportance(context);
        return importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
    }

}
