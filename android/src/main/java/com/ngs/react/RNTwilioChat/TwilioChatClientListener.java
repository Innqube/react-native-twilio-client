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

    private static final String LOG_TAG = "[Twi-ChatListener]";
    private ReactApplicationContext reactApplicationContext;

    public TwilioChatClientListener(ReactApplicationContext reactApplicationContext) {
        this.reactApplicationContext = reactApplicationContext;
    }

    // channel events
    @Override
    public void onChannelJoined(Channel channel) {
        Log.w(LOG_TAG, "onChannelJoined: " + channel.getSid() + ". No implementation provided. Use memberAdded?.");
    }

    @Override
    public void onChannelInvited(Channel channel) {
        Log.w(LOG_TAG, "onChannelInvited: " + channel.getSid() + ". No implementation provided. Use onInvitedToChannelNotification?.");
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
            eventJson.put("reason", updateReason.name());

            WritableMap eventMap = Utils.convertJsonToMap(eventJson);
            Utils.sendEvent(reactApplicationContext, "channelUpdated", eventMap);
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
            Utils.sendEvent(reactApplicationContext, "channelSynchronizationUpdated", channelMap);
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
        WritableMap json = new WritableNativeMap();
        json.putString("status", synchronizationStatus.name());
        Utils.sendEvent(reactApplicationContext, "synchronizationStatusUpdated", json);
    }

    @Override
    public void onConnectionStateChange(ChatClient.ConnectionState connectionState) {
        Log.d(LOG_TAG, "onConnectionStateChange: " + connectionState);
        WritableMap json = new WritableNativeMap();
        json.putString("state", adaptConnectionStateName(connectionState));
        Utils.sendEvent(reactApplicationContext, "connectionStateUpdated", json);
    }

    private String adaptConnectionStateName(ChatClient.ConnectionState connectionState) {
        switch (connectionState) {
            case CONNECTING:
                return "CONNECTING";
            case CONNECTED:
                return "COMPLETES";
            case DISCONNECTED:
                return "DISCONNECTED";
            case DENIED:
                return "DENIED";
            case FATAL_ERROR:
                return "FAILED";
            default:
                return "UNKNOWN";
        }
    }

    @Override
    public void onTokenExpired() {
        Log.d(LOG_TAG, "tokenExpired");
        Utils.sendEvent(reactApplicationContext, "tokenExpired");
    }

    @Override
    public void onTokenAboutToExpire() {
        Log.d(LOG_TAG, "tokenAboutToExpire");
        Utils.sendEvent(reactApplicationContext, "tokenAboutToExpire");
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
            eventJson.put("reason", updateReason.name());

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
            JSONObject userJson = Utils.userToJsonObject(user);
            WritableMap userMap = Utils.convertJsonToMap(userJson);
            Utils.sendEvent(reactApplicationContext, "userSubscribed", userMap);
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
        WritableMap map = new WritableNativeMap();
        map.putString("channelSid", channelSid);
        map.putInt("messageIndex", Integer.parseInt(Long.valueOf(messageIndex).toString()));
        Utils.sendEvent(reactApplicationContext, "newMessageNotification", map);
    }


    @Override
    public void onAddedToChannelNotification(String channelSid) {
        Log.d(LOG_TAG, "onAddedToChannelNotification: " + channelSid);
        Utils.sendEvent(reactApplicationContext, "addedToChannelNotification", channelSid);
    }

    @Override
    public void onInvitedToChannelNotification(String channelSid) {
        Log.d(LOG_TAG, "onInvitedToChannelNotification: " + channelSid);
        Utils.sendEvent(reactApplicationContext, "invitedToChannelNotification", channelSid);
    }

    @Override
    public void onRemovedFromChannelNotification(String channelSid) {
        Log.d(LOG_TAG, "onRemovedFromChannelNotification: " + channelSid);
        Utils.sendEvent(reactApplicationContext, "removedFromChannelNotification", channelSid);
    }

    @Override
    public void onNotificationSubscribed() {
        Log.d(LOG_TAG, "onNotificationSubscribed");
        Utils.sendEvent(reactApplicationContext, "notificationSubscribed");
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
