package com.ngs.react.RNTwilioChat;

import android.util.Log;
import com.facebook.react.bridge.*;
import com.ngs.react.PromiseCallbackListener;
import com.ngs.react.Utils;
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
        return "RNTwilioChatChannels";
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
    public void getChannel(String channelSidOrUniqueName, final Promise promise) {
        Log.d(LOG_TAG, "getChannel: " + channelSidOrUniqueName);
        TwilioChatModule
                .getChatClient()
                .getChannels()
                .getChannel(channelSidOrUniqueName, new PromiseCallbackListener<Channel>(promise) {
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

    @ReactMethod
    public void create(String friendlyName, String uniqueName, Integer type, ReadableMap attributes, final Promise promise) {
        Log.d(LOG_TAG, "createChannel");
        try {
            JSONObject attr = Utils.convertMapToJson(attributes);
            TwilioChatModule
                    .getChatClient()
                    .getChannels()
                    .channelBuilder()
                    .withFriendlyName(friendlyName)
                    .withUniqueName(uniqueName)
                    .withAttributes(attr)
                    .withType(Channel.ChannelType.fromInt(type))
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
            channel.addListener(new TwilioChannelListener(getReactApplicationContext()));
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

    @ReactMethod
    public void getLastMessages(String channelSidOrUniqueName, final Integer count, final Promise promise) {
        Log.d(LOG_TAG, "getMessages");

        getChannel(channelSidOrUniqueName, (Channel channel) -> {
            channel.getMessages().getLastMessages(count, new MessageListPromiseCallbackListener(promise));
        });
    }

    @ReactMethod
    public void getMessagesBefore(String channelSidOrUniqueName, final Long index, final Integer count, final Promise promise) {
        Log.d(LOG_TAG, "getMessagesBefore");

        getChannel(channelSidOrUniqueName, (Channel channel) -> {
            channel.getMessages().getMessagesBefore(index, count, new MessageListPromiseCallbackListener(promise));
        });
    }

    @ReactMethod
    public void getMessagesAfter(String channelSidOrUniqueName, final Long index, final Integer count, final Promise promise) {
        Log.d(LOG_TAG, "getMessagesAfter");

        getChannel(channelSidOrUniqueName, (Channel channel) -> {
            channel.getMessages().getMessagesAfter(index, count, new MessageListPromiseCallbackListener(promise));
        });
    }

    @ReactMethod
    public void setNoMessagesConsumed(String channelSidOrUniqueName, final Promise promise) {
        getChannel(channelSidOrUniqueName, (Channel channel) -> {
            channel.getMessages().setNoMessagesConsumedWithResult(new PromiseCallbackListener<Long>(promise) {
                @Override
                public void onSuccess(Long messageCount) {
                    promise.resolve(messageCount != null ? messageCount.toString() : null);
                }
            });
        });
    }

    @ReactMethod
    public void setAllMessageConsumed(String channelSidOrUniqueName, final Promise promise) {
        getChannel(channelSidOrUniqueName, (Channel channel) -> {
            channel.getMessages().setAllMessagesConsumedWithResult(new PromiseCallbackListener<Long>(promise) {
                @Override
                public void onSuccess(Long messageCount) {
                    promise.resolve(messageCount != null ? messageCount.toString() : null);
                }
            });
        });
    }

    @ReactMethod
    public void setLastConsumedMessage(String channelSidOrUniqueName, Integer index, final Promise promise) {
        getChannel(channelSidOrUniqueName, (Channel channel) -> {
            channel.getMessages().setLastConsumedMessageIndexWithResult(index, new PromiseCallbackListener<Long>(promise) {
                @Override
                public void onSuccess(Long messageCount) {
                    promise.resolve(messageCount != null ? messageCount.toString() : null);
                }
            });
        });
    }

    @ReactMethod
    public void advanceLastConsumedMessage(String channelSidOrUniqueName, Integer index, final Promise promise) {
        getChannel(channelSidOrUniqueName, (Channel channel) -> {
            channel.getMessages().advanceLastConsumedMessageIndexWithResult(index, new PromiseCallbackListener<Long>(promise) {
                @Override
                public void onSuccess(Long messageIndex) {
                    promise.resolve(messageIndex != null ? messageIndex.toString() : null);
                }
            });
        });
    }

    @ReactMethod
    public void getLastConsumedMessageIndex(String channelSidOrUniqueName, final Promise promise) {
        getChannel(channelSidOrUniqueName, (Channel channel) -> {
            Long index = channel.getMessages().getLastConsumedMessageIndex();
            promise.resolve(index != null ? index.toString() : index);
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
                                        JSONObject json = Utils.messageToJsonObject(message);
                                        promise.resolve(json);
                                    } catch (JSONException e) {
                                        promise.reject(e);
                                    }
                                }
                            }
                    );
        });
    }

    private class MessageListPromiseCallbackListener extends PromiseCallbackListener<List<Message>> {

        public MessageListPromiseCallbackListener(Promise promise) {
            super(promise);
        }

        @Override
        public void onSuccess(List<Message> messages) {
            try {
                JSONArray jsonArray = new JSONArray();

                for (Message message : messages) {
                    jsonArray.put(Utils.messageToJsonObject(message));
                }

                promise.resolve(Utils.convertJsonToArray(jsonArray));
            } catch (JSONException je) {
                promise.resolve(je);
            }
        }

    }

}
