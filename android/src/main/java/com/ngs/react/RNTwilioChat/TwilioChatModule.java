package com.ngs.react.RNTwilioChat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import com.facebook.react.bridge.*;
import com.ngs.react.PromiseCallbackListener;
import com.ngs.react.RNNotifications.TwilioNotificationsService;
import com.twilio.chat.ChatClient;
import com.twilio.chat.ErrorInfo;
import com.twilio.chat.StatusListener;

public class TwilioChatModule extends ReactContextBaseJavaModule {

    private static ChatClient CHAT_CLIENT;
    private static final String LOG_TAG = "[Twi-Chat]";
    private static ChatClient.SynchronizationStatus SYNCHRONIZATION_STATUS;
    private static TwilioChatModule INSTANCE;
    private TwilioNotificationsService tts;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(LOG_TAG, "TwilioChatModule -> TwilioTokenService connected");

            TwilioNotificationsService.TwilioFCMListenerBinder binder = (TwilioNotificationsService.TwilioFCMListenerBinder) iBinder;
            tts = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(LOG_TAG, "TwilioChatModule -> TwilioTokenService disconnected");
            tts = null;
        }

        @Override
        public void onBindingDied(ComponentName name) {
            Log.d(LOG_TAG, "TwilioChatModule -> onBindingDied: " + name.flattenToString());
        }
    };

    static ChatClient getChatClient() {
        return CHAT_CLIENT;
    }

    static ChatClient.SynchronizationStatus getSynchronizationStatus() {
        return SYNCHRONIZATION_STATUS;
    }

    public TwilioChatModule(ReactApplicationContext context) {
        super(context);
        Log.i(LOG_TAG, "TwilioChatModule instantiated");
        TwilioChatModule.INSTANCE = this;
    }

    public static TwilioChatModule get() {
        return TwilioChatModule.INSTANCE;
    }

    @Override
    public String getName() {
        return "RNTwilioChatClient";
    }

    @Override
    public void initialize() {
        super.initialize();

        Log.d(LOG_TAG, "Binding TwilioChatTokenService");

        Intent intent = new Intent(this.getReactApplicationContext(), TwilioNotificationsService.class);
        this.getReactApplicationContext().bindService(intent, this.serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @ReactMethod
    public static String getSdkVersion() {
        return ChatClient.getSdkVersion();
    }

    @ReactMethod
    public void createClient(String token, ReadableMap props, final Promise promise) {
        Log.d(LOG_TAG, "creating client. Token: " + token);

        if (token != null) {

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

                            if (synchronizationStatus == ChatClient.SynchronizationStatus.COMPLETED) {
                                // Client is now ready for business, start working
                                WritableMap json = new WritableNativeMap();
                                json.putString("status", "COMPLETED");
                                promise.resolve(json);
                            }

                            SYNCHRONIZATION_STATUS = synchronizationStatus;
                        }
                    });
                }
            });
        } else {
            promise.reject("Token cannot be null");
        }
    }

    @ReactMethod
    public void shutdown() {
        Log.d(LOG_TAG, "Shutting down twilio chat client");

        if (CHAT_CLIENT != null) {
            CHAT_CLIENT.shutdown();
            CHAT_CLIENT = null;
        }
    }

    @ReactMethod
    public void register(String token, final Promise promise) {
        if (CHAT_CLIENT == null) {
            Log.d(LOG_TAG, "Setting FCM token for later registration: " + token);
            promise.resolve(null);
        } else {
            Log.d(LOG_TAG, "Registering FCM token: " + token);
            CHAT_CLIENT.registerFCMToken(token, new StatusListener() {
                @Override
                public void onSuccess() {
                    Log.d(LOG_TAG, "FCM token registered");
                    promise.resolve(null);
                }

                @Override
                public void onError(ErrorInfo errorInfo) {
                    Log.d(LOG_TAG, "Could not register FCM token");
                    promise.reject(Integer.valueOf(errorInfo.getCode()).toString(), errorInfo.getMessage());
                }
            });
        }
    }

    @ReactMethod
    public void unregister(String token, final Promise promise) {
        Log.d(LOG_TAG, "Unregistering FCM token: " + token);
        CHAT_CLIENT.unregisterFCMToken(token, new StatusListener() {
            @Override
            public void onSuccess() {
                Log.d(LOG_TAG, "FCM token unregistered");
                promise.resolve(null);
            }

            @Override
            public void onError(ErrorInfo errorInfo) {
                Log.d(LOG_TAG, "Could not unregister FCM token");
                promise.reject(Integer.valueOf(errorInfo.getCode()).toString(), errorInfo.getMessage());
            }
        });
    }

    @ReactMethod
    public void getDeviceToken(final Promise promise) {
        Log.d(LOG_TAG, "getDeviceToken: " + (tts  != null ? tts.getToken() : null));
        promise.resolve(tts != null ? tts.getToken() : null);
    }

    @ReactMethod
    public void updateClient(String token, final Promise promise) {
        Log.d(LOG_TAG, "updateClient with token: " + token);

        if (CHAT_CLIENT == null) {
            Log.w(LOG_TAG, "Trying to update null client!");
            promise.reject("Trying to update null client!");
        } else {
            CHAT_CLIENT.updateToken(token, new StatusListener() {
                @Override
                public void onSuccess() {
                    Log.d(LOG_TAG, "Chat client token updated successfully");
                    promise.resolve(null);
                }

                @Override
                public void onError(ErrorInfo errorInfo) {
                    Log.d(LOG_TAG, "Could not update chat client token");
                    promise.reject(Integer.valueOf(errorInfo.getCode()).toString(), errorInfo.getMessage());
                }
            });
        }
    }

}
