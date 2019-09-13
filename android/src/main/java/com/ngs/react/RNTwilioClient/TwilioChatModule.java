package com.ngs.react.RNTwilioClient;

import android.util.Log;
import com.facebook.react.bridge.*;
import com.ngs.react.Converters;
import com.ngs.react.RNTwilioChat.Serializers;
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
        Log.d(LOG_TAG, "create");
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
    public void createChannel(String friendlyName, String uniqueName, ReadableMap attributes, final Promise promise) {
        Log.d(LOG_TAG, "createChannel");
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
                                JSONObject json = Serializers.channelToJsonObject(channel);
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


    @ReactMethod
    public void getChannel(String channelSidOrUniqueName, final Promise promise) {
        Log.d(LOG_TAG, "getChannel: " + channelSidOrUniqueName);
        chatClient.getChannels().getChannel(channelSidOrUniqueName, new PromiseCallbackListener<Channel>(promise) {
            @Override
            public void onSuccess(Channel channel) {
                try {
                    JSONObject json = Serializers.channelToJsonObject(channel);
                    promise.resolve(Converters.convertJsonToMap(json));
                } catch (JSONException e) {
                    promise.reject(e);
                }
            }
        });
    }

    @ReactMethod
    public void sendMessage(String channelSidOrUniqueName, final String message, final Promise promise) {
        Log.d(LOG_TAG, "sendMessage");
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
                                                    JSONObject json = Serializers.messageToJsonObject(message);
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
        Log.d(LOG_TAG, "getMessages");
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
                                                jsonArray.put(Serializers.messageToJsonObject(message));
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
