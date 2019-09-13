package com.ngs.react.RNTwilioClient;

import android.util.Log;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.twilio.chat.*;

public class TwilioChatModule extends ReactContextBaseJavaModule {

    private static final String LOG_TAG = "[IIMobile-Chat]";
    private ChatClient chatClient;
    private CallbackListener chatCallbackListener;

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
        ChatClient.create(getReactApplicationContext(), token, properties, new CallbackListener<ChatClient>() {
            @Override
            public void onSuccess(ChatClient chatClient) {
                Log.d(LOG_TAG, "Chat client created");
                promise.resolve(null);
            }

            @Override
            public void onError(ErrorInfo errorInfo) {
                Log.d(LOG_TAG, "Error creating client: " + errorInfo.getCode() + ": " + errorInfo.getMessage());
                promise.reject(
                        Integer.valueOf(errorInfo.getCode()).toString(),
                        errorInfo.getMessage()
                );
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
    public Channels getChannels() {
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

}
