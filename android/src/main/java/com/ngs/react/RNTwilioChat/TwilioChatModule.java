package com.ngs.react.RNTwilioChat;

import android.util.Log;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.ngs.react.PromiseCallbackListener;
import com.twilio.chat.ChatClient;
import com.twilio.chat.ErrorInfo;
import com.twilio.chat.StatusListener;

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

}
