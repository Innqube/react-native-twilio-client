package com.ngs.react.RNTwilioChat;

import android.util.Log;
import com.facebook.react.bridge.*;
import com.ngs.react.PromiseCallbackListener;
import com.ngs.react.Utils;
import com.twilio.chat.ChatClient;
import com.twilio.chat.ErrorInfo;
import com.twilio.chat.StatusListener;

public class TwilioChatModule extends ReactContextBaseJavaModule {

    private static final String LOG_TAG = "[Twi-Chat]";
    private static ChatClient CHAT_CLIENT;
    private static ChatClient.SynchronizationStatus SYNCHRONIZATION_STATUS;

    static ChatClient getChatClient() {
        return CHAT_CLIENT;
    }

    TwilioChatModule(ReactApplicationContext context) {
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
    public void createClient(String token, ReadableMap props, final Promise promise) {
        Log.d(LOG_TAG, "creating client. Token: " + token);

        if (CHAT_CLIENT == null) {
            Log.d(LOG_TAG, "No client instance found. Creating new client.");
            ChatClient.Properties.Builder builder = new ChatClient.Properties.Builder();

            if (props != null) {
                if (props.hasKey("region")) {
                    builder.setRegion(props.getString("region"));
                }
                if (props.hasKey("defer")) {
                    builder.setDeferCertificateTrustToPlatform(props.getBoolean("defer"));
                }
            }

            ChatClient.create(getReactApplicationContext(), token, builder.createProperties(), new PromiseCallbackListener<ChatClient>(promise) {
                @Override
                public void onSuccess(ChatClient chatClient) {
                    Log.d(LOG_TAG, "Chat client created");
                    CHAT_CLIENT = chatClient;
                    chatClient.setListener(new TwilioChatClientListener(getReactApplicationContext()) {
                        @Override
                        public void onClientSynchronization(ChatClient.SynchronizationStatus synchronizationStatus) {
                            super.onClientSynchronization(synchronizationStatus);
                            SYNCHRONIZATION_STATUS = synchronizationStatus;
                        }
                    });
                    WritableMap json = new WritableNativeMap();
                    json.putString("synchronizationStatus", null);
                    promise.resolve(json);
                }
            });
        } else {
            Log.d(LOG_TAG, "Found existent client instance");
            WritableMap json = new WritableNativeMap();
            json.putString("synchronizationStatus", SYNCHRONIZATION_STATUS != null ? SYNCHRONIZATION_STATUS.name() : null);
            promise.resolve(json);
            Utils.sendEvent(getReactApplicationContext(), "synchronizationStatusUpdated", SYNCHRONIZATION_STATUS.name());
        }
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
