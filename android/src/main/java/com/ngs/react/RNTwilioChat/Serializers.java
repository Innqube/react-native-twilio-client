package com.ngs.react.RNTwilioChat;

import com.twilio.chat.Channel;
import com.twilio.chat.ErrorInfo;
import com.twilio.chat.Message;
import com.twilio.chat.User;
import org.json.JSONException;
import org.json.JSONObject;

public class Serializers {

    public static JSONObject channelToJsonObject(Channel channel) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("uniqueName", channel.getUniqueName());
        json.put("friendlyName", channel.getFriendlyName());
        json.put("sid", channel.getSid());
        json.put("lastMessageIndex", channel.getLastMessageIndex());
        json.put("attributes", channel.getAttributes());
        return json;
    }

    public static JSONObject messageToJsonObject(Message message) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("author", message.getAuthor());
        json.put("channelSid", message.getChannelSid());
        json.put("messageBody", message.getMessageBody());
        json.put("messageIndex", message.getMessageIndex());
        json.put("sid", message.getSid());
        json.put("attributes", message.getAttributes());
        json.put("dateCreated", message.getDateCreated());
        return json;
    }

    public static JSONObject errorInfoToJsonObject(ErrorInfo errorInfo) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("code", errorInfo.getCode());
        json.put("message", errorInfo.getMessage());
        json.put("status", errorInfo.getStatus());
        return json;
    }

    public static JSONObject userToJsonObject(User user) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("friendlyName", user.getFriendlyName());
        json.put("identity", user.getIdentity());
        json.put("attributes", user.getAttributes());
        return json;
    }

}
