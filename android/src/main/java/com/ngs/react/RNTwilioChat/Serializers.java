package com.ngs.react.RNTwilioChat;

import com.twilio.chat.Channel;
import com.twilio.chat.Message;
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

}
