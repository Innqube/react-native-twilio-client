package com.ngs.react;

import com.facebook.react.bridge.*;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.firebase.messaging.RemoteMessage;
import com.twilio.chat.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class Utils {

    public static void sendEvent(ReactContext reactContext, String eventName) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, null);
    }

    public static void sendEvent(ReactContext reactContext, String eventName, String value) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, value);
    }

    public static void sendEvent(ReactContext reactContext, String eventName, WritableMap params) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    public static WritableMap convertJsonToMap(JSONObject jsonObject) throws JSONException {
        WritableMap map = new WritableNativeMap();

        Iterator<String> iterator = jsonObject.keys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject) {
                map.putMap(key, convertJsonToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                map.putArray(key, convertJsonToArray((JSONArray) value));
            } else if (value instanceof Boolean) {
                map.putBoolean(key, (Boolean) value);
            } else if (value instanceof Long) {
                map.putDouble(key, ((Long) value).doubleValue());
            } else if (value instanceof Integer) {
                map.putInt(key, (Integer) value);
            } else if (value instanceof Double) {
                map.putDouble(key, (Double) value);
            } else if (value instanceof String) {
                map.putString(key, (String) value);
            } else {
                map.putString(key, value.toString());
            }
        }
        return map;
    }

    public static WritableArray convertJsonToArray(JSONArray jsonArray) throws JSONException {
        WritableArray array = new WritableNativeArray();

        for (int i = 0; i < jsonArray.length(); i++) {
            Object value = jsonArray.get(i);
            if (value instanceof JSONObject) {
                array.pushMap(convertJsonToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                array.pushArray(convertJsonToArray((JSONArray) value));
            } else if (value instanceof Boolean) {
                array.pushBoolean((Boolean) value);
            } else if (value instanceof Integer) {
                array.pushInt((Integer) value);
            } else if (value instanceof Double) {
                array.pushDouble((Double) value);
            } else if (value instanceof String) {
                array.pushString((String) value);
            } else {
                array.pushString(value.toString());
            }
        }
        return array;
    }

    public static WritableMap convertRemoteMessageDataToMap(RemoteMessage msg) {
        WritableMap data = new WritableNativeMap();
        msg.getData().forEach(data::putString);

        WritableMap map = new WritableNativeMap();
        map.putString("collapseKey", msg.getCollapseKey());
        map.putString("from", msg.getFrom());
        map.putString("messageId", msg.getMessageId());
        map.putString("messageType", msg.getMessageType());
        map.putString("to", msg.getTo());
        map.putInt("originalPriority", msg.getOriginalPriority());
        map.putInt("priority", msg.getPriority());
        map.putInt("ttl", msg.getTtl());
        map.putInt("sentTime", Long.valueOf(msg.getSentTime()).intValue());
        map.putMap("data", data);

        return map;
    }

    public static JSONObject convertMapToJson(ReadableMap readableMap) throws JSONException {
        JSONObject object = new JSONObject();
        ReadableMapKeySetIterator iterator = readableMap.keySetIterator();
        while (iterator.hasNextKey()) {
            String key = iterator.nextKey();
            switch (readableMap.getType(key)) {
                case Null:
                    object.put(key, JSONObject.NULL);
                    break;
                case Boolean:
                    object.put(key, readableMap.getBoolean(key));
                    break;
                case Number:
                    object.put(key, readableMap.getDouble(key));
                    break;
                case String:
                    object.put(key, readableMap.getString(key));
                    break;
                case Map:
                    object.put(key, convertMapToJson(readableMap.getMap(key)));
                    break;
                case Array:
                    object.put(key, convertArrayToJson(readableMap.getArray(key)));
                    break;
            }
        }
        return object;
    }

    public static JSONArray convertArrayToJson(ReadableArray readableArray) throws JSONException {
        JSONArray array = new JSONArray();
        for (int i = 0; i < readableArray.size(); i++) {
            switch (readableArray.getType(i)) {
                case Null:
                    break;
                case Boolean:
                    array.put(readableArray.getBoolean(i));
                    break;
                case Number:
                    array.put(readableArray.getDouble(i));
                    break;
                case String:
                    array.put(readableArray.getString(i));
                    break;
                case Map:
                    array.put(convertMapToJson(readableArray.getMap(i)));
                    break;
                case Array:
                    array.put(convertArrayToJson(readableArray.getArray(i)));
                    break;
            }
        }
        return array;
    }

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
        json.put("body", message.getMessageBody());
        json.put("index", message.getMessageIndex());
        json.put("sid", message.getSid());
        json.put("attributes", message.getAttributes());
        json.put("dateCreated", message.getDateCreated());
        json.put("timestampAsDate", message.getDateCreatedAsDate().getTime());
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

    public static JSONObject memberToJsonObject(Member member) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("identity", member.getIdentity());
        json.put("sid", member.getSid());
        json.put("lastConsumedMessageIndex", member.getLastConsumedMessageIndex());
        json.put("lastConsumptionTimestamp", member.getLastConsumptionTimestamp());
        json.put("type", member.getType() != null ? member.getType().name() : null);
        return json;
    }

}
