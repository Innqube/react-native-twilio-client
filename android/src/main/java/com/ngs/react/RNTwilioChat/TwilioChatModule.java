package com.ngs.react.RNTwilioChat;

import android.util.Log;
import com.facebook.react.bridge.*;
import com.facebook.react.modules.core.DeviceEventManagerModule;
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
        return "RNTwilioChatClient";
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

                chatClient.setListener(new ChatClientListener() {

                    // channel events
                    @Override
                    public void onChannelJoined(Channel channel) {
                        Log.d(LOG_TAG, "onChannelJoined: " + channel.getSid());
                        try {
                            JSONObject channelJson = Serializers.channelToJsonObject(channel);
                            WritableMap channelMap = Converters.convertJsonToMap(channelJson);
                            sendEvent(getReactApplicationContext() ,"channelJoined", channelMap);
                        } catch (JSONException e) {
                            Log.e(LOG_TAG, "Could not handle event", e);
                        }
                    }

                    @Override
                    public void onChannelInvited(Channel channel) {
                        Log.d(LOG_TAG, "onChannelInvited: " + channel.getSid());
                        try {
                            JSONObject channelJson = Serializers.channelToJsonObject(channel);
                            WritableMap channelMap = Converters.convertJsonToMap(channelJson);
                            sendEvent(getReactApplicationContext() ,"channelInvited", channelMap);
                        } catch (JSONException e) {
                            Log.e(LOG_TAG, "Could not handle event", e);
                        }
                    }

                    @Override
                    public void onChannelAdded(Channel channel) {
                        Log.d(LOG_TAG, "onChannelAdded: " + channel.getSid());
                        try {
                            JSONObject channelJson = Serializers.channelToJsonObject(channel);
                            WritableMap channelMap = Converters.convertJsonToMap(channelJson);
                            sendEvent(getReactApplicationContext() ,"channelAdded", channelMap);
                        } catch (JSONException e) {
                            Log.e(LOG_TAG, "Could not handle event", e);
                        }
                    }

                    @Override
                    public void onChannelUpdated(Channel channel, Channel.UpdateReason updateReason) {
                        Log.d(LOG_TAG, "onChannelUpdated: " + channel.getSid());
                        try {
                            JSONObject eventJson = new JSONObject();
                            JSONObject channelJson = Serializers.channelToJsonObject(channel);

                            eventJson.put("channel", channelJson);
                            eventJson.put("updateReason", updateReason.name());

                            WritableMap eventMap = Converters.convertJsonToMap(eventJson);
                            sendEvent(getReactApplicationContext() ,"channelUpdate", eventMap);
                        } catch (JSONException e) {
                            Log.e(LOG_TAG, "Could not handle event", e);
                        }
                    }

                    @Override
                    public void onChannelDeleted(Channel channel) {
                        Log.d(LOG_TAG, "onChannelDeleted: " + channel.getSid());
                        try {
                            JSONObject channelJson = Serializers.channelToJsonObject(channel);
                            WritableMap channelMap = Converters.convertJsonToMap(channelJson);
                            sendEvent(getReactApplicationContext() ,"channelDeleted", channelMap);
                        } catch (JSONException e) {
                            Log.e(LOG_TAG, "Could not handle event", e);
                        }
                    }

                    @Override
                    public void onChannelSynchronizationChange(Channel channel) {
                        Log.d(LOG_TAG, "onChannelSynchronizationChange: " + channel.getSid());
                        try {
                            JSONObject channelJson = Serializers.channelToJsonObject(channel);
                            WritableMap channelMap = Converters.convertJsonToMap(channelJson);
                            sendEvent(getReactApplicationContext() ,"channelSynchronizationChange", channelMap);
                        } catch (JSONException e) {
                            Log.e(LOG_TAG, "Could not handle event", e);
                        }
                    }
                    // end of channel events

                    // chat client events
                    @Override
                    public void onError(ErrorInfo errorInfo) {
                        Log.d(LOG_TAG, "onError: " + errorInfo.getCode() + ", " + errorInfo.getMessage());
                        try {
                            JSONObject errorInfoJson = Serializers.errorInfoToJsonObject(errorInfo);
                            WritableMap errorInfoMap = Converters.convertJsonToMap(errorInfoJson);
                            sendEvent(getReactApplicationContext() ,"error", errorInfoMap);
                        } catch (JSONException e) {
                            Log.e(LOG_TAG, "Could not handle event", e);
                        }
                    }

                    @Override
                    public void onClientSynchronization(ChatClient.SynchronizationStatus synchronizationStatus) {
                        Log.d(LOG_TAG, "onClientSynchronization: " + synchronizationStatus);
                        try {
                            JSONObject synchronizationJson = new JSONObject();
                            synchronizationJson.put("synchronizationStatus", synchronizationStatus.name());
                            WritableMap syncronizationMap = Converters.convertJsonToMap(synchronizationJson);
                            sendEvent(getReactApplicationContext() ,"synchronizationStatus", syncronizationMap);
                        } catch (JSONException e) {
                            Log.e(LOG_TAG, "Could not handle event", e);
                        }
                    }

                    @Override
                    public void onConnectionStateChange(ChatClient.ConnectionState connectionState) {
                        Log.d(LOG_TAG, "onConnectionStateChange: " + connectionState);
                        try {
                            JSONObject stateJson = new JSONObject();
                            stateJson.put("connectionState", connectionState.name());
                            WritableMap syncronizationMap = Converters.convertJsonToMap(stateJson);
                            sendEvent(getReactApplicationContext() ,"connectionStateChange", syncronizationMap);
                        } catch (JSONException e) {
                            Log.e(LOG_TAG, "Could not handle event", e);
                        }
                    }

                    @Override
                    public void onTokenExpired() {
                        Log.d(LOG_TAG, "tokenExpired");
                        sendEvent(getReactApplicationContext() ,"tokenExpired", null);
                    }

                    @Override
                    public void onTokenAboutToExpire() {
                        Log.d(LOG_TAG, "tokenAboutToExpire");
                        sendEvent(getReactApplicationContext() ,"tokenAboutToExpire", null);
                    }
                    // end of chat client events

                    // user events
                    @Override
                    public void onUserUpdated(User user, User.UpdateReason updateReason) {
                        Log.d(LOG_TAG, "onUserUpdated: " + user.getIdentity());
                        try {
                            JSONObject eventJson = new JSONObject();
                            JSONObject userJson = Serializers.userToJsonObject(user);

                            eventJson.put("user", userJson);
                            eventJson.put("updateReason", updateReason.name());

                            WritableMap eventMap = Converters.convertJsonToMap(eventJson);
                            sendEvent(getReactApplicationContext() ,"userUpdated", eventMap);
                        } catch (JSONException e) {
                            Log.e(LOG_TAG, "Could not handle event", e);
                        }
                    }

                    @Override
                    public void onUserSubscribed(User user) {
                        Log.d(LOG_TAG, "onUserSubscribed: " + user.getIdentity());
                        try {
                            JSONObject errorInfoJson = Serializers.userToJsonObject(user);
                            WritableMap errorInfoMap = Converters.convertJsonToMap(errorInfoJson);
                            sendEvent(getReactApplicationContext() ,"userSubscribed", errorInfoMap);
                        } catch (JSONException e) {
                            Log.e(LOG_TAG, "Could not handle event", e);
                        }
                    }

                    @Override
                    public void onUserUnsubscribed(User user) {
                        Log.d(LOG_TAG, "onUserUnsubscribed: " + user.getIdentity());
                        try {
                            JSONObject errorInfoJson = Serializers.userToJsonObject(user);
                            WritableMap errorInfoMap = Converters.convertJsonToMap(errorInfoJson);
                            sendEvent(getReactApplicationContext() ,"userUnsubscribed", errorInfoMap);
                        } catch (JSONException e) {
                            Log.e(LOG_TAG, "Could not handle event", e);
                        }
                    }
                    // end of user notifications


                    // Push notifications
                    @Override
                    public void onNewMessageNotification(String channelSid, String messageSid, long messageIndex) {
                        Log.e(LOG_TAG, "Not implemented: onNewMessageNotification. ChannelSid: " +
                                channelSid + ", messageSid: " + messageSid + ", messageIndex: " + messageSid);
                    }


                    @Override
                    public void onAddedToChannelNotification(String channelSid) {
                        Log.d(LOG_TAG, "onAddedToChannelNotification: " + channelSid);
                        WritableMap map = new WritableNativeMap();
                        map.putString("channelSid", channelSid);
                        sendEvent(getReactApplicationContext() ,"addedToChannelNotification", map);
                    }

                    @Override
                    public void onInvitedToChannelNotification(String channelSid) {
                        Log.d(LOG_TAG, "onInvitedToChannelNotification: " + channelSid);
                        WritableMap map = new WritableNativeMap();
                        map.putString("channelSid", channelSid);
                        sendEvent(getReactApplicationContext() ,"invitedToChannelNotification", map);
                    }

                    @Override
                    public void onRemovedFromChannelNotification(String channelSid) {
                        Log.d(LOG_TAG, "onRemovedFromChannelNotification: " + channelSid);
                        WritableMap map = new WritableNativeMap();
                        map.putString("channelSid", channelSid);
                        sendEvent(getReactApplicationContext() ,"removedFromChannelNotification", map);
                    }

                    @Override
                    public void onNotificationSubscribed() {
                        Log.d(LOG_TAG, "onNotificationSubscribed");
                        sendEvent(getReactApplicationContext() ,"notificationSubscribed", null);
                    }

                    @Override
                    public void onNotificationFailed(ErrorInfo errorInfo) {
                        Log.d(LOG_TAG, "onError: " + errorInfo.getCode() + ", " + errorInfo.getMessage());
                        try {
                            JSONObject errorInfoJson = Serializers.errorInfoToJsonObject(errorInfo);
                            WritableMap errorInfoMap = Converters.convertJsonToMap(errorInfoJson);
                            sendEvent(getReactApplicationContext() ,"error", errorInfoMap);
                        } catch (JSONException e) {
                            Log.e(LOG_TAG, "Could not handle event", e);
                        }
                    }
                    // End of push notifications
                });

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

    @ReactMethod
    public void getUnconsumedMessagesCount(String channelSidOrUniqueName, final Promise promise) {
        Log.d(LOG_TAG, "getUnconsumedMessagesCount");
        chatClient
            .getChannels()
            .getChannel(channelSidOrUniqueName, new PromiseCallbackListener<Channel>(promise) {
                    @Override
                    public void onSuccess(Channel channel) {
                        channel.getUnconsumedMessagesCount(new PromiseCallbackListener<Long>(promise) {
                            @Override
                            public void onSuccess(Long unconsumed) {
                                Log.d(LOG_TAG, "success: getUnconsumedMessagesCount: " + unconsumed);
                                promise.resolve(unconsumed != null ? unconsumed.toString() : null);
                            }
                        });
                    }
            });
    }

    private void sendEvent(ReactContext reactContext, String eventName, WritableMap params) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
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
