package com.ngs.react.RNTwilioVideo;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableNativeMap;
import com.ngs.react.BuildConfig;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import static com.ngs.react.RNTwilioVideo.VideoConstants.*;

interface ReactContextCallback {
    void execute(ReactContext reactContext);
}

public class VideoMessagingService extends Service {

    private static final String TAG = "RNTwilioVideo";
    private CallNotificationManager callNotificationManager;
    private Handler handler;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "VideoFirebaseMessagingService created");
        callNotificationManager = new CallNotificationManager();
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                Log.d(TAG, "Handling message: " + msg.getData());
                VideoCallInvite invite = msg.getData().getParcelable(INCOMING_CALL_INVITE);
                String action = msg.getData().getString("action");
                Log.d(TAG, "invite: " + invite);
                onMessageReceived(action, invite);
                stopSelf(msg.arg1);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        Message msg = handler.obtainMessage();
        msg.arg1 = startId;
        msg.setData(intent.getExtras());
        handler.sendMessage(msg);

        return START_NOT_STICKY;
    }

    public void onMessageReceived(String action, VideoCallInvite invite) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Action: " + action);
            Log.d(TAG, "Bundle data: " + invite);
        }

        // Check if message contains a data payload.
        if (invite != null) {
            if (action == null) {
                Log.w(TAG, "No action to act on from remote message");
                return;
            }

            switch (action) {
                case "call":
                    handleIncomingCallNotification(invite);
                    break;
                case "cancel":
                    cancelIncomingCallNotification(invite);
                    break;
                case "reject":
                    Integer notificationId = invite.getSession().hashCode();
                    callNotificationManager.removeNotification(getApplicationContext(), notificationId);
                    startReactContext(reactContext -> {
                        new Handler(Looper.getMainLooper())
                                .postDelayed(() -> { // give some time to initialize listeners on the JS side
                                    EventManager em = new EventManager((ReactApplicationContext) reactContext);
                                    WritableNativeMap params = new WritableNativeMap();
                                    for (Map.Entry<String, String> entry : invite.getData().entrySet()) {
                                        params.putString(entry.getKey(), entry.getValue());
                                    }
                                    em.sendEvent("performEndVideoCall", params);
                                }, 3000);
                    });
                    break;
            }

        }
    }

    private void startReactContext(ReactContextCallback callback) {
        Log.d(TAG, "startReactContext");
        ReactInstanceManager mReactInstanceManager = ((ReactApplication) getApplication()).getReactNativeHost().getReactInstanceManager();
        ReactContext context = mReactInstanceManager.getCurrentReactContext();

        if (context == null) {
            Log.d(TAG, "React context null");
            mReactInstanceManager.addReactInstanceEventListener(new ReactInstanceManager.ReactInstanceEventListener() {
                @Override
                public void onReactContextInitialized(ReactContext context) {
                    Log.d(TAG, "React context initialized");
                    callback.execute(context);
                }
            });
            mReactInstanceManager.createReactContextInBackground();
        } else {
            Log.d(TAG, "Got react context already");
            callback.execute(context);
        }
    }

    private void cancelIncomingCallNotification(VideoCallInvite invite) {
        Log.d(TAG, "cancelIncomingCall");
        String taskAttributesString = invite.getTaskAttributes();

        if (taskAttributesString == null) {
            Log.d(TAG, "no task attributes to cancel call");
            return;
        }

        String teamSession;
        try {
            JSONObject taskAttributes = new JSONObject(taskAttributesString);
            teamSession = taskAttributes.getString("teamSession");
            Log.d(TAG, "teamSession: " + teamSession);
        } catch (JSONException ex) {
            Log.w(TAG, "No session found. Can not remove incoming call notification. Invite data: " + invite);
            return;
        }

        int notificationId = teamSession.hashCode();
        callNotificationManager.removeNotification(
                getApplicationContext(),
                notificationId
        );
    }

    private void handleIncomingCallNotification(VideoCallInvite invite) {
        // If notification ID is not provided by the user for push notification, generate one at random

        if (invite.getSession() == null) {
            Log.w(TAG, "No session found. Can not create incoming call notification. Invite data: " + invite);
            return;
        }

        int notificationId = invite.getSession().hashCode();
        callNotificationManager.createIncomingCallNotification(
                getApplicationContext(),
                invite,
                notificationId
        );

        Intent intent = new Intent(ACTION_INCOMING_CALL);
        intent.putExtra(INCOMING_CALL_NOTIFICATION_ID, notificationId);
        intent.putExtra(INCOMING_CALL_INVITE, invite);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

}
