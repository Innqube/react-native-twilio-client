package com.ngs.react.RNTwilioChat;

import android.util.Log;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.ngs.react.Utils;
import com.twilio.chat.Channel;
import com.twilio.chat.ChannelListener;
import com.twilio.chat.Member;
import com.twilio.chat.Message;
import org.json.JSONException;
import org.json.JSONObject;

public class TwilioChannelListener implements ChannelListener {

    private static final String LOG_TAG = "[IIMobile-ChnlListener]";
    private ReactApplicationContext reactApplicationContext;

    public TwilioChannelListener(ReactApplicationContext reactApplicationContext) {
        this.reactApplicationContext = reactApplicationContext;
    }

    @Override
    public void onMessageAdded(Message message) {
        Log.d(LOG_TAG, "onMessageAdded: " + message.getSid());
        try {
            WritableMap wrapper = new WritableNativeMap();

            JSONObject messageJson = Utils.messageToJsonObject(message);
            WritableMap messageMap = Utils.convertJsonToMap(messageJson);

            wrapper.putString("channelSid", message.getChannelSid());
            wrapper.putMap("message", messageMap);

            Utils.sendEvent(reactApplicationContext ,"messageAdded", wrapper);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Could not handle event", e);
        }
    }

    @Override
    public void onMessageUpdated(Message message, Message.UpdateReason updateReason) {
        Log.d(LOG_TAG, "onMessageUpdated: " + message.getSid());
        try {
            WritableMap wrapper = new WritableNativeMap();

            JSONObject messageJson = Utils.messageToJsonObject(message);
            WritableMap messageMap = Utils.convertJsonToMap(messageJson);

            wrapper.putString("channelSid", message.getChannelSid());
            wrapper.putString("reason", updateReason.name());
            wrapper.putMap("message", messageMap);

            Utils.sendEvent(reactApplicationContext ,"messageUpdated", wrapper);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Could not handle event", e);
        }
    }

    @Override
    public void onMessageDeleted(Message message) {
        Log.d(LOG_TAG, "onMessageDeleted: " + message.getSid());
        try {
            WritableMap wrapper = new WritableNativeMap();

            JSONObject messageJson = Utils.messageToJsonObject(message);
            WritableMap messageMap = Utils.convertJsonToMap(messageJson);

            wrapper.putString("channelSid", message.getChannelSid());
            wrapper.putMap("message", messageMap);

            Utils.sendEvent(reactApplicationContext ,"messageDeleted", wrapper);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Could not handle event", e);
        }
    }

    @Override
    public void onMemberAdded(Member member) {
        Log.d(LOG_TAG, "onMemberAdded: " + member.getSid());
        try {
            WritableMap wrapper = new WritableNativeMap();
            JSONObject memberJson = Utils.memberToJsonObject(member);

            WritableMap memberMap = Utils.convertJsonToMap(memberJson);

            wrapper.putString("channelSid", member.getChannel().getSid());
            wrapper.putMap("member", memberMap);

            Utils.sendEvent(reactApplicationContext ,"memberAdded", wrapper);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Could not handle event", e);
        }
    }

    @Override
    public void onMemberUpdated(Member member, Member.UpdateReason updateReason) {
        Log.d(LOG_TAG, "onMemberUpdated: " + member.getSid());
        try {
            WritableMap wrapper = new WritableNativeMap();
            JSONObject memberJson = Utils.memberToJsonObject(member);

            WritableMap memberMap = Utils.convertJsonToMap(memberJson);

            wrapper.putString("channelSid", member.getChannel().getSid());
            wrapper.putMap("member", memberMap);
            wrapper.putString("reason", updateReason.name());

            Utils.sendEvent(reactApplicationContext ,"memberUpdated", wrapper);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Could not handle event", e);
        }
    }

    @Override
    public void onMemberDeleted(Member member) {
        Log.d(LOG_TAG, "onMemberDeleted: " + member.getSid());
        try {
            WritableMap wrapper = new WritableNativeMap();
            JSONObject memberJson = Utils.memberToJsonObject(member);

            WritableMap memberMap = Utils.convertJsonToMap(memberJson);

            wrapper.putString("channelSid", member.getChannel().getSid());
            wrapper.putMap("member", memberMap);

            Utils.sendEvent(reactApplicationContext ,"memberDeleted", wrapper);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Could not handle event", e);
        }
    }

    @Override
    public void onTypingStarted(Channel channel, Member member) {
        Log.d(LOG_TAG, "onTypingStarted: " + member.getSid());
        try {
            JSONObject memberJson = Utils.memberToJsonObject(member);
            WritableMap channelMap = Utils.convertJsonToMap(memberJson);
            Utils.sendEvent(reactApplicationContext ,"typingStarted", channelMap);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Could not handle event", e);
        }
    }

    @Override
    public void onTypingEnded(Channel channel, Member member) {
        Log.d(LOG_TAG, "onTypingEnded: " + member.getSid());
        try {
            JSONObject memberJson = Utils.memberToJsonObject(member);
            WritableMap channelMap = Utils.convertJsonToMap(memberJson);
            Utils.sendEvent(reactApplicationContext ,"typingEnded", channelMap);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Could not handle event", e);
        }
    }

    @Override
    public void onSynchronizationChanged(Channel channel) {
        Log.d(LOG_TAG, "onSynchronizationChanged");
        Utils.sendEvent(reactApplicationContext ,"synchronizationStatusUpdated");
    }

}
