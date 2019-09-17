package com.ngs.react.RNTwilioChat;

import android.util.Log;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.ngs.react.Converters;
import com.ngs.react.PromiseCallbackListener;
import com.twilio.chat.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class TwilioChatChannelModule extends ReactContextBaseJavaModule {

    private static final String LOG_TAG = "[IIMobile-Channel]";

    public TwilioChatChannelModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "RNTwilioChatChannel";
    }

    interface GotChannel {
        void gotChannel(Channel channel);
    }

    private void getChannel(String channelSidOrUniqueName, final GotChannel gotChannel) {
        TwilioChatModule
                .getChatClient()
                .getChannels()
                .getChannel(channelSidOrUniqueName, new CallbackListener<Channel>() {
                    @Override
                    public void onSuccess(Channel channel) {
                        gotChannel.gotChannel(channel);
                    }

                    @Override
                    public void onError(ErrorInfo errorInfo) {
                        Log.d(LOG_TAG, "Error getting channel. Code: " + errorInfo.getCode() +
                                "Message: " + errorInfo.getMessage() + ", " +
                                "Status: " + errorInfo.getStatus());
                    }
                });
    }


    @ReactMethod
    public void sendMessage(String channelSidOrUniqueName, final String message, final Promise promise) {
        Log.d(LOG_TAG, "sendMessage");

        getChannel(channelSidOrUniqueName, (Channel channel) -> {
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
        });
    }

    @ReactMethod
    public void getMessages(String channelSidOrUniqueName, final Long index, final Integer count, final Promise promise) {
        Log.d(LOG_TAG, "getMessages");

        getChannel(channelSidOrUniqueName, (Channel channel) -> {
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
        });
    }

    @ReactMethod
    public void join(String channelSidOrUniqueName, final Promise promise) {
        Log.d(LOG_TAG, "join");
        getChannel(channelSidOrUniqueName, (Channel channel) -> {
            channel.join(new StatusListener() {
                @Override
                public void onSuccess() {
                    promise.resolve(null);
                }

                @Override
                public void onError(ErrorInfo errorInfo) {
                    promise.reject(Integer.valueOf(errorInfo.getCode()).toString(), errorInfo.getMessage());
                }
            });
        });
    }

    @ReactMethod
    public void leave(String channelSidOrUniqueName, final Promise promise) {
        Log.d(LOG_TAG, "leave");
        getChannel(channelSidOrUniqueName, (Channel channel) -> {
            channel.leave(new StatusListener() {
                @Override
                public void onSuccess() {
                    promise.resolve(null);
                }

                @Override
                public void onError(ErrorInfo errorInfo) {
                    promise.reject(Integer.valueOf(errorInfo.getCode()).toString(), errorInfo.getMessage());
                }
            });
        });
    }

    @ReactMethod
    public void typing(String channelSidOrUniqueName) {
        Log.d(LOG_TAG, "typing");
        getChannel(channelSidOrUniqueName, Channel::typing);
    }

    @ReactMethod
    public void getMessagesCount(String channelSidOrUniqueName, final Promise promise) {
        Log.d(LOG_TAG, "getMessagesCount");
        getChannel(channelSidOrUniqueName, (Channel channel) -> {
            channel.getMessagesCount(new PromiseCallbackListener<Long>(promise) {
                @Override
                public void onSuccess(Long messageCount) {
                    Log.d(LOG_TAG, "success: getMessagesCount: " + messageCount);
                    promise.resolve(messageCount != null ? messageCount.toString() : null);
                }
            });
        });
    }

    @ReactMethod
    public void getUnconsumedMessagesCount(String channelSidOrUniqueName, final Promise promise) {
        Log.d(LOG_TAG, "getUnconsumedMessagesCount");
        getChannel(channelSidOrUniqueName, (Channel channel) -> {
            channel.getUnconsumedMessagesCount(new PromiseCallbackListener<Long>(promise) {
                @Override
                public void onSuccess(Long unconsumed) {
                    Log.d(LOG_TAG, "success: getUnconsumedMessagesCount: " + unconsumed);
                    promise.resolve(unconsumed != null ? unconsumed.toString() : null);
                }
            });
        });
    }

    @ReactMethod
    public void getMembersCount(String channelSidOrUniqueName, final Promise promise) {
        Log.d(LOG_TAG, "getMembersCount");
        getChannel(channelSidOrUniqueName, (Channel channel) -> {
            channel.getMembersCount(new PromiseCallbackListener<Long>(promise) {
                @Override
                public void onSuccess(Long membersCount) {
                    Log.d(LOG_TAG, "success: membersCount: " + membersCount);
                    promise.resolve(membersCount != null ? membersCount.toString() : null);
                }
            });
        });
    }

}
