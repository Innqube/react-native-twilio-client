package com.ngs.react.RNTwilioVideo;

import android.app.ActionBar;
import android.app.PendingIntent;
import android.content.*;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ngs.react.R;
import com.ngs.react.RNLocalizedStrings.LocalizedKeys;

import static com.ngs.react.RNTwilioVideo.VideoConstants.*;

public class IncomingVideoCallFullscreenActivity extends AppCompatActivity {
    private static final String TAG = "RNTwilioVideo";

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 100;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = () -> {
        // Delayed removal of status and navigation bar

        // Note that some of these constants are new as of API 16 (Jelly Bean)
        // and API 19 (KitKat). It is safe to use them, as they are inlined
        // at compile-time and do nothing on earlier devices.
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    };
    private BroadcastReceiver cancelCallBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "onReceive");
            if (ACTION_CANCEL_CALL_INVITE.equals(intent.getAction())) {
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_incoming_video_call_fullscreen);

        mContentView = findViewById(R.id.fullscreen_content);

        TextView caller = findViewById(R.id.caller);
        TextView appName = findViewById(R.id.app_name);
        TextView incomingVideoCall = (TextView) findViewById(R.id.incomingVideoCall);
        TextView decline = (TextView) findViewById(R.id.decline_text);
        TextView answer = (TextView) findViewById(R.id.answer_text);
        TextView goOffline = (TextView) findViewById(R.id.go_offline_text);

        Context context = getApplicationContext();
        SharedPreferences sharedPref = context.getSharedPreferences("db", Context.MODE_PRIVATE);

        incomingVideoCall.setText(sharedPref.getString(LocalizedKeys.INCOMING_VIDEO_CALL, "Incoming video call"));
        decline.setText(sharedPref.getString(LocalizedKeys.DECLINE, "Decline"));
        answer.setText(sharedPref.getString(LocalizedKeys.ACCEPT, "Accept"));
        goOffline.setText(sharedPref.getString(LocalizedKeys.GO_OFFLINE, "Go offline"));

        VideoCallInvite callInvite = getIntent().getParcelableExtra(INCOMING_CALL_INVITE);
        caller.setText(callInvite.getFrom("\n", sharedPref));

        try {
            ApplicationInfo info = getPackageManager().getApplicationInfo(
                    getPackageName(), PackageManager.GET_META_DATA
            );
            appName.setText(getPackageManager().getApplicationLabel(info).toString().toUpperCase());

            ImageView background = findViewById(R.id.background_image);
            background.setImageResource(info.metaData.getInt("com.ngs.react.APP_BACKGROUND"));
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, e.getMessage());
        }

        turnScreenOn();

        Log.d(TAG, "Attaching touch listeners");
        findViewById(R.id.answer_button).setOnClickListener(answer());
        findViewById(R.id.decline_button).setOnClickListener(decline());
        findViewById(R.id.go_offline_button).setOnClickListener(goOffline());
    }

    private View.OnClickListener answer() {
        return (event) -> {
            Log.i(TAG, "answer");
            VideoCallInvite callInvite = getIntent().getParcelableExtra(INCOMING_CALL_INVITE);
            int notificationId = getIntent().getIntExtra(INCOMING_CALL_NOTIFICATION_ID, -1);

            Intent answerIntent = new Intent(ACTION_ANSWER_CALL)
                    .putExtra(INCOMING_CALL_INVITE, callInvite)
                    .putExtra(INCOMING_CALL_NOTIFICATION_ID, notificationId)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.DONUT) {
                answerIntent.setPackage(getApplicationContext().getPackageName());
            }

            Log.i(TAG, "getBroadcast");
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    0,
                    answerIntent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );

            try {
                pendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                Log.i(TAG, e.getMessage());
                e.printStackTrace();
            } finally {
                finish();
            }
        };
    }

    private View.OnClickListener decline() {
        return (event) -> {
            Log.i(TAG, "decline");
            VideoCallInvite callInvite = getIntent().getParcelableExtra(INCOMING_CALL_INVITE);
            int notificationId = getIntent().getIntExtra(INCOMING_CALL_NOTIFICATION_ID, -1);

            Intent rejectIntent = new Intent(ACTION_REJECT_CALL)
                    .putExtra(INCOMING_CALL_INVITE, callInvite)
                    .putExtra(INCOMING_CALL_NOTIFICATION_ID, notificationId)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.DONUT) {
                rejectIntent.setPackage(getApplicationContext().getPackageName());
            }

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    1,
                    rejectIntent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );

            try {
                pendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                Log.i(TAG, e.getMessage());
                e.printStackTrace();
            } finally {
                finish();
            }
        };
    }

    private View.OnClickListener goOffline() {
        return (event) -> {
            Log.i(TAG, "go offline");
            VideoCallInvite callInvite = getIntent().getParcelableExtra(INCOMING_CALL_INVITE);
            int notificationId = getIntent().getIntExtra(INCOMING_CALL_NOTIFICATION_ID, -1);

            Intent rejectIntent = new Intent(ACTION_GO_OFFLINE)
                    .putExtra(INCOMING_CALL_INVITE, callInvite)
                    .putExtra(INCOMING_CALL_NOTIFICATION_ID, notificationId)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.DONUT) {
                rejectIntent.setPackage(getApplicationContext().getPackageName());
            }

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    1,
                    rejectIntent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );

            try {
                pendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                Log.i(TAG, e.getMessage());
                e.printStackTrace();
            } finally {
                finish();
            }
        };
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        hide();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_CANCEL_CALL_INVITE);

        Log.i(TAG, "Register broadcast receiver");
        LocalBroadcastManager.getInstance(this).registerReceiver(cancelCallBroadcastReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        turnScreenOff();
        Log.i(TAG, "Unregister broadcast receiver");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(cancelCallBroadcastReceiver);
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    private void turnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            );
        }
    }

    private void turnScreenOff() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(false);
            setTurnScreenOn(false);
        } else {
            getWindow().clearFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            );
        }
    }
}
