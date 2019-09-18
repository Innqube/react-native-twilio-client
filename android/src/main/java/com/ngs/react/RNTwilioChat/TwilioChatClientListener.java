package com.ngs.react.RNTwilioChat;

import android.util.Log;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.ngs.react.Utils;
import com.twilio.chat.*;
import org.json.JSONException;
import org.json.JSONObject;

public class TwilioChatClientListener implements ChatClientListener {

    private static final String LOG_TAG = "[IIMobile-ChatListener]";
    private ReactApplicationContext reactApplicationContext;

    public TwilioChatClientListener(ReactApplicationContext reactApplicationContext) {
        this.reactApplicationContext = reactApplicationContext;
    }

    // channel events
    @Override
    public void onChannelJoined(Channel channel) {
        Log.d(LOG_TAG, "onChannelJoined: " + channel.getSid());
        try {
            JSONObject channelJson = Utils.channelToJsonObject(channel);
            WritableMap channelMap = Utils.convertJsonToMap(channelJson);
            Utils.sendEvent(reactApplicationContext, "channelJoined", channelMap);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Could not handle event", e);
        }
    }

    @Override
    public void onChannelInvited(Channel channel) {
        Log.d(LOG_TAG, "onChannelInvited: " + channel.getSid());
        try {
            JSONObject channelJson = Utils.channelToJsonObject(channel);
            WritableMap channelMap = Utils.convertJsonToMap(channelJson);
            Utils.sendEvent(reactApplicationContext, "channelInvited", channelMap);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Could not handle event", e);
        }
    }

    @Override
    public void onChannelAdded(Channel channel) {
        Log.d(LOG_TAG, "onChannelAdded: " + channel.getSid());
        try {
            JSONObject channelJson = Utils.channelToJsonObject(channel);
            WritableMap channelMap = Utils.convertJsonToMap(channelJson);
            Utils.sendEvent(reactApplicationContext, "channelAdded", channelMap);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Could not handle event", e);
        }
    }

    @Override
    public void onChannelUpdated(Channel channel, Channel.UpdateReason updateReason) {
        Log.d(LOG_TAG, "onChannelUpdated: " + channel.getSid());
        try {
            JSONObject eventJson = new JSONObject();
            JSONObject channelJson = Utils.channelToJsonObject(channel);

            eventJson.put("channel", channelJson);
            eventJson.put("updateReason", updateReason.name());

            WritableMap eventMap = Utils.convertJsonToMap(eventJson);
            Utils.sendEvent(reactApplicationContext, "channelUpdate", eventMap);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Could not handle event", e);
        }
    }

    @Override
    public void onChannelDeleted(Channel channel) {
        Log.d(LOG_TAG, "onChannelDeleted: " + channel.getSid());
        try {
            JSONObject channelJson = Utils.channelToJsonObject(channel);
            WritableMap channelMap = Utils.convertJsonToMap(channelJson);
            Utils.sendEvent(reactApplicationContext, "channelDeleted", channelMap);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Could not handle event", e);
        }
    }

    @Override
    public void onChannelSynchronizationChange(Channel channel) {
        Log.d(LOG_TAG, "onChannelSynchronizationChange: " + channel.getSid());
        try {
            JSONObject channelJson = Utils.channelToJsonObject(channel);
            WritableMap channelMap = Utils.convertJsonToMap(channelJson);
            Utils.sendEvent(reactApplicationContext, "channelSynchronizationChange", channelMap);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Could not handle event", e);
        }
    }
    // end of channel events

    // chat client events
    @Override
    public void onError(ErrorInfo errorInfo) {
        Log.d(LOG_TAG, "onError: " + errorInfo.getCode() + ", " + errorInfo.getMessage());
        try {
            JSONObject errorInfoJson = Utils.errorInfoToJsonObject(errorInfo);
            WritableMap errorInfoMap = Utils.convertJsonToMap(errorInfoJson);
            Utils.sendEvent(reactApplicationContext, "error", errorInfoMap);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Could not handle event", e);
        }
    }

    @Override
    public void onClientSynchronization(ChatClient.SynchronizationStatus synchronizationStatus) {
        Log.d(LOG_TAG, "onClientSynchronization: " + synchronizationStatus);
//        try {
//            JSONObject synchronizationJson = new JSONObject();
//            synchronizationJson.put("synchronizationStatus", synchronizationStatus.name());
//            WritableMap syncronizationMap = Utils.convertJsonToMap(synchronizationJson);
            Utils.sendEvent(reactApplicationContext, "synchronizationStatusUpdated", synchronizationStatus.name());
//        } catch (JSONException e) {
//            Log.e(LOG_TAG, "Could not handle event", e);
//        }
    }

    @Override
    public void onConnectionStateChange(ChatClient.ConnectionState connectionState) {
        Log.d(LOG_TAG, "onConnectionStateChange: " + connectionState);
        try {
            JSONObject stateJson = new JSONObject();
            stateJson.put("connectionState", connectionState.name());
            WritableMap syncronizationMap = Utils.convertJsonToMap(stateJson);
            Utils.sendEvent(reactApplicationContext, "connectionStateChanged", syncronizationMap);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Could not handle event", e);
        }
    }

    @Override
    public void onTokenExpired() {
        Log.d(LOG_TAG, "tokenExpired");
        Utils.sendEvent(reactApplicationContext, "tokenExpired", null);
    }

    @Override
    public void onTokenAboutToExpire() {
        Log.d(LOG_TAG, "tokenAboutToExpire");
        Utils.sendEvent(reactApplicationContext, "tokenAboutToExpire", null);
    }
    // end of chat client events

    // user events
    @Override
    public void onUserUpdated(User user, User.UpdateReason updateReason) {
        Log.d(LOG_TAG, "onUserUpdated: " + user.getIdentity());
        try {
            JSONObject eventJson = new JSONObject();
            JSONObject userJson = Utils.userToJsonObject(user);

            eventJson.put("user", userJson);
            eventJson.put("updateReason", updateReason.name());

            WritableMap eventMap = Utils.convertJsonToMap(eventJson);
            Utils.sendEvent(reactApplicationContext, "userUpdated", eventMap);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Could not handle event", e);
        }
    }

    @Override
    public void onUserSubscribed(User user) {
        Log.d(LOG_TAG, "onUserSubscribed: " + user.getIdentity());
        try {
            JSONObject errorInfoJson = Utils.userToJsonObject(user);
            WritableMap errorInfoMap = Utils.convertJsonToMap(errorInfoJson);
            Utils.sendEvent(reactApplicationContext, "userSubscribed", errorInfoMap);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Could not handle event", e);
        }
    }

    @Override
    public void onUserUnsubscribed(User user) {
        Log.d(LOG_TAG, "onUserUnsubscribed: " + user.getIdentity());
        try {
            JSONObject userJson = Utils.userToJsonObject(user);
            WritableMap userMap = Utils.convertJsonToMap(userJson);
            Utils.sendEvent(reactApplicationContext, "userUnsubscribed", userMap);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Could not handle event", e);
        }
    }
    // end of user notifications


    // Push notifications
    @Override
    public void onNewMessageNotification(String channelSid, String messageSid, long messageIndex) {
        Log.e(LOG_TAG, "Not implemented: onNewMessageNotification. ChannelSid: " +
                channelSid + ", messageSid: " + messageSid + ", messageIndex: " + messageSid);
    }


    @Override
    public void onAddedToChannelNotification(String channelSid) {
        Log.d(LOG_TAG, "onAddedToChannelNotification: " + channelSid);
        WritableMap map = new WritableNativeMap();
        map.putString("channelSid", channelSid);
        Utils.sendEvent(reactApplicationContext, "addedToChannelNotification", map);
    }

    @Override
    public void onInvitedToChannelNotification(String channelSid) {
        Log.d(LOG_TAG, "onInvitedToChannelNotification: " + channelSid);
        WritableMap map = new WritableNativeMap();
        map.putString("channelSid", channelSid);
        Utils.sendEvent(reactApplicationContext, "invitedToChannelNotification", map);
    }

    @Override
    public void onRemovedFromChannelNotification(String channelSid) {
        Log.d(LOG_TAG, "onRemovedFromChannelNotification: " + channelSid);
        WritableMap map = new WritableNativeMap();
        map.putString("channelSid", channelSid);
        Utils.sendEvent(reactApplicationContext, "removedFromChannelNotification", map);
    }

    @Override
    public void onNotificationSubscribed() {
        Log.d(LOG_TAG, "onNotificationSubscribed");
        Utils.sendEvent(reactApplicationContext, "notificationSubscribed", null);
    }

    @Override
    public void onNotificationFailed(ErrorInfo errorInfo) {
        Log.d(LOG_TAG, "onError: " + errorInfo.getCode() + ", " + errorInfo.getMessage());
        try {
            JSONObject errorInfoJson = Utils.errorInfoToJsonObject(errorInfo);
            WritableMap errorInfoMap = Utils.convertJsonToMap(errorInfoJson);
            Utils.sendEvent(reactApplicationContext, "error", errorInfoMap);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Could not handle event", e);
        }
    }
    // End of push notifications
}
