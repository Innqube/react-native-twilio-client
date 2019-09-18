package com.ngs.react.RNTwilioChat;

import android.util.Log;
import com.facebook.react.bridge.*;
import com.ngs.react.PromiseCallbackListener;
import com.ngs.react.Utils;
import com.twilio.chat.Channel;
import com.twilio.chat.ChatClient;
import com.twilio.chat.ErrorInfo;
import com.twilio.chat.StatusListener;
import org.json.JSONException;
import org.json.JSONObject;

public class TwilioChatModule extends ReactContextBaseJavaModule {

    private static final String LOG_TAG = "[IIMobile-Chat]";
    private static ChatClient CHAT_CLIENT;

    public static ChatClient getChatClient() {
        return CHAT_CLIENT;
    }

    public TwilioChatModule(ReactApplicationContext context) {
        super(context);
    }

    @Override
    public String getName() {
        return "RNTwilioChatClient";
    }

    @ReactMethod
    public static String getSdkVersion() {
        return ChatClient.getSdkVersion();
    }

    @ReactMethod
    public void createClient(String token, final Promise promise) {
        Log.d(LOG_TAG, "create");
        ChatClient.Properties properties = new ChatClient.Properties.Builder().createProperties();
        ChatClient.create(getReactApplicationContext(), token, properties, new PromiseCallbackListener<ChatClient>(promise) {
            @Override
            public void onSuccess(ChatClient chatClient) {
                Log.d(LOG_TAG, "Chat client created");
                CHAT_CLIENT = chatClient;
                chatClient.setListener(new TwilioChatClientListener(getReactApplicationContext()));
                promise.resolve(null);
            }
        });
    }

    @ReactMethod
    public void shutdown() {
        Log.d(LOG_TAG, "Shutting down twilio chat client");
        CHAT_CLIENT.shutdown();
    }

    @ReactMethod
    public void register(String token, final Promise promise) {
        CHAT_CLIENT.registerFCMToken(token, new StatusListener() {
            @Override
            public void onSuccess() {
                promise.resolve(null);
            }

            @Override
            public void onError(ErrorInfo errorInfo) {
                promise.reject(Integer.valueOf(errorInfo.getCode()).toString(), errorInfo.getMessage());
            }
        });
    }

    @ReactMethod
    public void unregister(String token, final Promise promise) {
        CHAT_CLIENT.unregisterFCMToken(token, new StatusListener() {
            @Override
            public void onSuccess() {
                promise.resolve(null);
            }

            @Override
            public void onError(ErrorInfo errorInfo) {
                promise.reject(Integer.valueOf(errorInfo.getCode()).toString(), errorInfo.getMessage());
            }
        });
    }

    // FIXME
    @ReactMethod
    public void createChannel(String friendlyName, String uniqueName, ReadableMap attributes, final Promise promise) {
        Log.d(LOG_TAG, "createChannel");
        try {
            JSONObject attr = Utils.convertMapToJson(attributes);
            CHAT_CLIENT
                    .getChannels()
                    .channelBuilder()
                    .withFriendlyName(friendlyName)
                    .withUniqueName(uniqueName)
                    .withAttributes(attr)
                    .build(new PromiseCallbackListener<Channel>(promise) {
                        @Override
                        public void onSuccess(Channel channel) {
                            try {
                                channel.removeAllListeners();
                                channel.addListener(new TwilioChannelListener(getReactApplicationContext()));
                                JSONObject json = Utils.channelToJsonObject(channel);
                                promise.resolve(Utils.convertJsonToMap(json));
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
        CHAT_CLIENT.getChannels().getChannel(channelSidOrUniqueName, new PromiseCallbackListener<Channel>(promise) {
            @Override
            public void onSuccess(Channel channel) {
                try {
                    JSONObject json = Utils.channelToJsonObject(channel);
                    promise.resolve(Utils.convertJsonToMap(json));
                } catch (JSONException e) {
                    promise.reject(e);
                }
            }
        });
    }

}
