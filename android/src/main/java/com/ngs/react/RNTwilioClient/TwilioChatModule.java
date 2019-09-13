package com.ngs.react.RNTwilioClient;

import android.util.Log;
import com.facebook.react.bridge.*;
import com.ngs.react.Converters;
import com.twilio.chat.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class TwilioChatModule extends ReactContextBaseJavaModule {

    private static final String LOG_TAG = "[IIMobile-Chat]";
    private ChatClient chatClient;

    public TwilioChatModule(ReactApplicationContext context) {
        super(context);
    }

    @Override
    public String getName() {
        return "RNTwilioChat";
    }

    @ReactMethod
    public static String getSdkVersion() {
        return ChatClient.getSdkVersion();
    }

    @ReactMethod
    public void create(String token, final Promise promise) {
        Log.d(LOG_TAG, "About to create client");
        ChatClient.Properties properties = new ChatClient.Properties.Builder().createProperties();
        ChatClient.create(getReactApplicationContext(), token, properties, new PromiseCallbackListener<ChatClient>(promise) {
            @Override
            public void onSuccess(ChatClient chatClient) {
                Log.d(LOG_TAG, "Chat client created");
                TwilioChatModule.this.chatClient = chatClient;
                promise.resolve(null);
            }
        });
    }

    @ReactMethod
    public ChatClient.Properties getProperties() {
        return chatClient.getProperties();
    }

    @ReactMethod
    public void updateToken(String token, StatusListener listener) {
        chatClient.updateToken(token, listener);
    }

    @ReactMethod
    public void shutdown() {
        chatClient.shutdown();
    }

    @ReactMethod
    public void setListener(ChatClientListener listener) {
        chatClient.setListener(listener);
    }

    @ReactMethod
    public void removeListener() {
        chatClient.removeListener();
    }

    @ReactMethod
    public Channels getChannels(Promise promise) {
        return chatClient.getChannels();
    }

    @ReactMethod
    public void registerGCMToken(String token, StatusListener listener) {
        chatClient.registerGCMToken(token, listener);
    }

    @ReactMethod
    public void unregisterGCMToken(String token, StatusListener listener) {
        chatClient.unregisterGCMToken(token, listener);
    }

    @ReactMethod
    public void registerFCMToken(String token, StatusListener listener) {
        chatClient.registerFCMToken(token, listener);
    }

    @ReactMethod
    public void unregisterFCMToken(String token, StatusListener listener) {
        chatClient.unregisterFCMToken(token, listener);
    }

    @ReactMethod
    public void handleNotification(NotificationPayload notification) {
        chatClient.handleNotification(notification);
    }

    @ReactMethod
    public ChatClient.ConnectionState getConnectionState() {
        return chatClient.getConnectionState();
    }

    @ReactMethod
    public String getMyIdentity() {
        return chatClient.getMyIdentity();
    }

    @ReactMethod
    public Users getUsers() {
        return chatClient.getUsers();
    }

    @ReactMethod
    public boolean isReachabilityEnabled() {
        return chatClient.isReachabilityEnabled();
    }


    // Channels delegation
    @ReactMethod
    public void createChannel(String friendlyName, String uniqueName, ReadableMap attributes, final Promise promise) {
        try {
            JSONObject attr = Converters.convertMapToJson(attributes);
            this.chatClient
                    .getChannels()
                    .channelBuilder()
                    .withFriendlyName(friendlyName)
                    .withUniqueName(uniqueName)
                    .withAttributes(attr)
                    .build(new PromiseCallbackListener<Channel>(promise) {
                        @Override
                        public void onSuccess(Channel channel) {
                            try {
                                JSONObject json = channelToJsonObject(channel);
                                promise.resolve(Converters.convertJsonToMap(json));
                            } catch (JSONException e) {
                                promise.reject(e);
                            }
                        }
                    });
        } catch (JSONException e) {
            promise.reject(e);
        }
    }

    private JSONObject channelToJsonObject(Channel channel) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("uniqueName", channel.getUniqueName());
        json.put("friendlyName", channel.getFriendlyName());
        json.put("sid", channel.getSid());
        json.put("lastMessageIndex", channel.getLastMessageIndex());
        json.put("attributes", channel.getAttributes());
        return json;
    }

    private JSONObject messageToJsonObject(Message message) throws JSONException {
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

    @ReactMethod
    public void getChannel(String channelSidOrUniqueName, final Promise promise) {
        chatClient.getChannels().getChannel(channelSidOrUniqueName, new PromiseCallbackListener<Channel>(promise) {
            @Override
            public void onSuccess(Channel channel) {
                try {
                    JSONObject json = channelToJsonObject(channel);
                    promise.resolve(Converters.convertJsonToMap(json));
                } catch (JSONException e) {
                    promise.reject(e);
                }
            }
        });
    }

    @ReactMethod
    public void sendMessage(String channelSidOrUniqueName, final String message, final Promise promise) {
        chatClient
                .getChannels()
                .getChannel(channelSidOrUniqueName, new PromiseCallbackListener<Channel>(promise) {
                    @Override
                    public void onSuccess(Channel channel) {
                        channel
                                .getMessages()
                                .sendMessage(
                                        Message.options().withBody(message),
                                        new PromiseCallbackListener<Message>(promise) {
                                            @Override
                                            public void onSuccess(Message message) {
                                                try {
                                                    JSONObject json = messageToJsonObject(message);
                                                    promise.resolve(json);
                                                } catch (JSONException e) {
                                                    promise.reject(e);
                                                }
                                            }
                                        }
                                );
                    }
                });
    }

    @ReactMethod
    public void getMessages(String channelSidOrUniqueName, final Long index, final Integer count, final Promise promise) {
        chatClient
                .getChannels()
                .getChannel(channelSidOrUniqueName, new PromiseCallbackListener<Channel>(promise) {
                    @Override
                    public void onSuccess(Channel channel) {
                        channel.getMessages()
                                .getMessagesAfter(index, count, new PromiseCallbackListener<List<Message>>(promise) {
                                    @Override
                                    public void onSuccess(List<Message> messages) {
                                        try {
                                            JSONArray jsonArray = new JSONArray();

                                            for (Message message : messages) {
                                                jsonArray.put(messageToJsonObject(message));
                                            }

                                            promise.resolve(Converters.convertJsonToArray(jsonArray));
                                        } catch (JSONException je) {
                                            promise.resolve(je);
                                        }
                                    }
                                });
                    }
                });
    }

    abstract class PromiseCallbackListener<T> extends CallbackListener<T> {

        private Promise promise;

        PromiseCallbackListener(Promise promise) {
            this.promise = promise;
        }

        @Override
        public void onError(ErrorInfo errorInfo) {
            Log.d(LOG_TAG, "Error creating client: " + errorInfo.getCode() + ": " + errorInfo.getMessage());
            promise.reject(Integer.valueOf(errorInfo.getCode()).toString(), errorInfo.getMessage());
        }

    }

}
