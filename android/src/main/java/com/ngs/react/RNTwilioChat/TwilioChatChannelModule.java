package com.ngs.react.RNTwilioChat;

import android.util.Log;
import com.facebook.react.bridge.*;
import com.ngs.react.PromiseCallbackListener;
import com.ngs.react.Utils;
import com.twilio.chat.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TwilioChatChannelModule extends ReactContextBaseJavaModule implements ChannelListener {

    private static final String LOG_TAG = "[Twi-ChatChannel]";
    private List<String> channelListeners = new ArrayList<>();

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

    private void addListener(Channel channel) {
        if (!channelListeners.contains(channel.getSid())) {
            channel.addListener(this);
            channelListeners.add(channel.getSid());
        }
    }

    private void getChannel(String channelSidOrUniqueName, final GotChannel gotChannel) {
        Log.d(LOG_TAG, "Looking for channel " + channelSidOrUniqueName);
        TwilioChatModule
                .getChatClient()
                .getChannels()
                .getChannel(channelSidOrUniqueName, new CallbackListener<Channel>() {
                    @Override
                    public void onSuccess(Channel channel) {
                        Log.d(LOG_TAG, "Get channel success: " + channelSidOrUniqueName);
                        gotChannel.gotChannel(channel);
                    }

                    @Override
                    public void onError(ErrorInfo errorInfo) {
                        Log.d(LOG_TAG, "Get channel error: " + channelSidOrUniqueName);
                        Log.d(LOG_TAG, "Error getting channel. Code: " + errorInfo.getCode() +
                                "Message: " + errorInfo.getMessage() + ", " +
                                "Status: " + errorInfo.getStatus());
                    }
                });
    }

    @ReactMethod
    public void get(String channelSidOrUniqueName, final Promise promise) {
        Log.d(LOG_TAG, "getChannel: " + channelSidOrUniqueName);
        TwilioChatModule
                .getChatClient()
                .getChannels()
                .getChannel(channelSidOrUniqueName, new PromiseCallbackListener<Channel>(promise) {
                    @Override
                    public void onSuccess(Channel channel) {
                        addListener(channel);
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
    public void create(String friendlyName, String uniqueName, String type, ReadableMap attributes, final Promise promise) {
        Log.d(LOG_TAG, "createChannel");

        if (friendlyName == null || uniqueName == null || type == null) {
            promise.resolve("Check that friendlyName, uniqueName and type are provided");
        }

        try {
            JSONObject attr = attributes == null ? null : Utils.convertMapToJson(attributes);
            TwilioChatModule
                    .getChatClient()
                    .getChannels()
                    .channelBuilder()
                    .withFriendlyName(friendlyName)
                    .withUniqueName(uniqueName)
                    .withAttributes(attr)
                    .withType(Channel.ChannelType.valueOf(type.toUpperCase()))
                    .build(new PromiseCallbackListener<Channel>(promise) {
                        @Override
                        public void onSuccess(Channel channel) {
                            try {
                                channel.removeAllListeners();
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
        Log.d(LOG_TAG, "join channel: " + channelSidOrUniqueName);
        getChannel(channelSidOrUniqueName, (Channel channel) -> {
            channel.join(new StatusListener() {
                @Override
                public void onSuccess() {
                    addListener(channel);
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
        Log.d(LOG_TAG, "leave channel: " + channelSidOrUniqueName);
        getChannel(channelSidOrUniqueName, (Channel channel) -> {
            channel.leave(new StatusListener() {
                @Override
                public void onSuccess() {
                    channelListeners.remove(channel.getSid());
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
        Log.d(LOG_TAG, "typing in channel: " + channelSidOrUniqueName);
        getChannel(channelSidOrUniqueName, Channel::typing);
    }

    @ReactMethod
    public void getUnconsumedMessagesCount(String channelSidOrUniqueName, final Promise promise) {
        Log.d(LOG_TAG, "getUnconsumedMessagesCount for channel: " + channelSidOrUniqueName);
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
        Log.d(LOG_TAG, "getMessagesCount for channel: " + channelSidOrUniqueName);
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
        Log.d(LOG_TAG, "getMembersCount for channel: " + channelSidOrUniqueName);
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
        Log.d(LOG_TAG, "getMessages for channel: " + channelSidOrUniqueName);

        getChannel(channelSidOrUniqueName, (Channel channel) -> {
            if (channel.getMessages() != null) {
                channel.getMessages().getLastMessages(count, new MessageListPromiseCallbackListener(promise));
            } else {
                promise.reject("No messages instance obtained");
            }
        });
    }

    @ReactMethod
    public void getMessagesBefore(String channelSidOrUniqueName, final Integer index, final Integer count, final Promise promise) {
        Log.d(LOG_TAG, "getMessagesBefore for channel: " + channelSidOrUniqueName);

        getChannel(channelSidOrUniqueName, (Channel channel) -> {
            if (channel.getMessages() != null) {
                channel.getMessages().getMessagesBefore(index, count, new MessageListPromiseCallbackListener(promise));
            } else {
                promise.reject("No messages instance obtained");
            }
        });
    }

    @ReactMethod
    public void getMessagesAfter(String channelSidOrUniqueName, final Integer index, final Integer count, final Promise promise) {
        Log.d(LOG_TAG, "getMessagesAfter for channel: " + channelSidOrUniqueName);

        getChannel(channelSidOrUniqueName, (Channel channel) -> {
            if (channel.getMessages() != null) {
                channel.getMessages().getMessagesAfter(index, count, new MessageListPromiseCallbackListener(promise));
            } else {
                promise.reject("No messages instance obtained");
            }
        });
    }

    @ReactMethod
    public void setNoMessagesConsumed(String channelSidOrUniqueName, final Promise promise) {
        Log.d(LOG_TAG, "setNoMessagesConsumed for channel: " + channelSidOrUniqueName);
        getChannel(channelSidOrUniqueName, (Channel channel) -> {
            if (channel.getMessages() != null) {
                channel.getMessages().setNoMessagesConsumedWithResult(new PromiseCallbackListener<Long>(promise) {
                    @Override
                    public void onSuccess(Long messageCount) {
                        promise.resolve(messageCount != null ? messageCount.toString() : null);
                    }
                });
            } else {
                promise.reject("No messages instance obtained");
            }
        });
    }

    @ReactMethod
    public void setAllMessageConsumed(String channelSidOrUniqueName, final Promise promise) {
        Log.d(LOG_TAG, "setAllMessageConsumed for channel: " + channelSidOrUniqueName);
        getChannel(channelSidOrUniqueName, (Channel channel) -> {
            if (channel.getMessages() != null) {
                channel.getMessages().setAllMessagesConsumedWithResult(new PromiseCallbackListener<Long>(promise) {
                    @Override
                    public void onSuccess(Long messageCount) {
                        promise.resolve(messageCount != null ? messageCount.toString() : null);
                    }
                });
            } else {
                promise.reject("No messages instance obtained");
            }
        });
    }

    @ReactMethod
    public void setLastConsumedMessage(String channelSidOrUniqueName, Integer index, final Promise promise) {
        Log.d(LOG_TAG, "setLastConsumedMessage for channel: " + channelSidOrUniqueName + ", index: " + index);
        getChannel(channelSidOrUniqueName, (Channel channel) -> {
            if (channel.getMessages() != null) {
                channel.getMessages().setLastConsumedMessageIndexWithResult(index, new PromiseCallbackListener<Long>(promise) {
                    @Override
                    public void onSuccess(Long messageCount) {
                        promise.resolve(messageCount != null ? messageCount.toString() : null);
                    }
                });
            } else {
                promise.reject("No messages instance obtained");
            }
        });
    }

    @ReactMethod
    public void advanceLastConsumedMessage(String channelSidOrUniqueName, Integer index, final Promise promise) {
        Log.d(LOG_TAG, "advanceLastConsumedMessage for channel: " + channelSidOrUniqueName + ", index: " + index);
        getChannel(channelSidOrUniqueName, (Channel channel) -> {
            if (channel.getMessages() != null) {
                channel.getMessages().advanceLastConsumedMessageIndexWithResult(index, new PromiseCallbackListener<Long>(promise) {
                    @Override
                    public void onSuccess(Long messageIndex) {
                        promise.resolve(messageIndex != null ? messageIndex.intValue() : null);
                    }
                });
            } else {
                promise.reject("No messages instance obtained");
            }
        });
    }

    @ReactMethod
    public void getLastConsumedMessageIndex(String channelSidOrUniqueName, final Promise promise) {
        Log.d(LOG_TAG, "getLastConsumedMessageIndex for channel: " + channelSidOrUniqueName);
        getChannel(channelSidOrUniqueName, (Channel channel) -> {
            Log.d(LOG_TAG, "getLastConsumedMessageIndex: channelSid -> " + channel.getSid());
            Log.d(LOG_TAG, "getLastConsumedMessageIndex: status -> " + channel.getStatus());
            Log.d(LOG_TAG, "getLastConsumedMessageIndex: synchronizationStatus -> " + channel.getSynchronizationStatus());
            Log.d(LOG_TAG, "getLastConsumedMessageIndex: messages -> " + channel.getMessages());
            if (channel.getMessages() != null) {
                Long index = channel.getMessages().getLastConsumedMessageIndex();
                promise.resolve(index != null ? index.toString() : null);
            } else {
                promise.reject("No messages instance obtained");
            }
        });
    }

    @ReactMethod
    public void sendMessage(String channelSidOrUniqueName, final String message, ReadableMap attributes, final Promise promise) {
        Log.d(LOG_TAG, "sendMessage");

        getChannel(channelSidOrUniqueName, (Channel channel) -> {
            if (channel.getMessages() != null) {
                Message.Options options = Message.options().withBody(message);

                if (attributes != null) {
                    try {
                        options = options.withAttributes(Utils.convertMapToJson(attributes));
                    } catch (JSONException e) {
                        promise.reject(e);
                    }
                }

                channel
                        .getMessages()
                        .sendMessage(
                                options,
                                new PromiseCallbackListener<Message>(promise) {
                                    @Override
                                    public void onSuccess(Message message) {
                                        try {
                                            JSONObject json = Utils.messageToJsonObject(message);
                                            WritableMap map = Utils.convertJsonToMap(json);
                                            promise.resolve(map);
                                        } catch (JSONException e) {
                                            promise.reject(e);
                                        }
                                    }
                                }
                        );
            } else {
                promise.reject("No messages instance obtained");
            }
        });
    }

    @ReactMethod
    public void getMembers(String channelSidOrUniqueName, final Promise promise) {
        Log.d(LOG_TAG, "getMembers for channel: " + channelSidOrUniqueName);

        getChannel(channelSidOrUniqueName, (Channel channel) -> {
            JSONArray jsonArray = new JSONArray();
            WritableArray wArray = new WritableNativeArray();
            try {
                for (Member member : channel.getMembers().getMembersList()) {
                    JSONObject jsonObject = Utils.memberToJsonObject(member);
                    jsonArray.put(jsonObject);
                }
                wArray = Utils.convertJsonToArray(jsonArray);
            } catch (JSONException e) {
                promise.reject(e);
            }

            promise.resolve(wArray);
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


    // Channel listener implementation
    @Override
    public void onMessageAdded(Message message) {
        Log.d(LOG_TAG, "onMessageAdded: " + message.getSid());
        try {
            WritableMap wrapper = new WritableNativeMap();

            JSONObject messageJson = Utils.messageToJsonObject(message);
            WritableMap messageMap = Utils.convertJsonToMap(messageJson);

            wrapper.putString("channelSid", message.getChannelSid());
            wrapper.putMap("message", messageMap);

            Utils.sendEvent(getReactApplicationContext(), "messageAdded", wrapper);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Could not handle event", e);
        }
    }

    @Override
    public void onMessageUpdated(Message message, Message.UpdateReason updateReason) {
        Log.d(LOG_TAG, "onMessageUpdated: " + message.getSid());
        try {
            WritableMap wrapper = new WritableNativeMap();

            JSONObject messageJson = Utils.messageToJsonObject(message);
            WritableMap messageMap = Utils.convertJsonToMap(messageJson);

            wrapper.putString("channelSid", message.getChannelSid());
            wrapper.putString("reason", updateReason.name());
            wrapper.putMap("message", messageMap);

            Utils.sendEvent(getReactApplicationContext(), "messageUpdated", wrapper);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Could not handle event", e);
        }
    }

    @Override
    public void onMessageDeleted(Message message) {
        Log.d(LOG_TAG, "onMessageDeleted: " + message.getSid());
        try {
            WritableMap wrapper = new WritableNativeMap();

            JSONObject messageJson = Utils.messageToJsonObject(message);
            WritableMap messageMap = Utils.convertJsonToMap(messageJson);

            wrapper.putString("channelSid", message.getChannelSid());
            wrapper.putMap("message", messageMap);

            Utils.sendEvent(getReactApplicationContext(), "messageDeleted", wrapper);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Could not handle event", e);
        }
    }

    @Override
    public void onMemberAdded(Member member) {
        Log.d(LOG_TAG, "onMemberAdded: " + member.getSid());
        try {
            WritableMap wrapper = new WritableNativeMap();
            JSONObject memberJson = Utils.memberToJsonObject(member);

            WritableMap memberMap = Utils.convertJsonToMap(memberJson);

            wrapper.putString("channelSid", member.getChannel().getSid());
            wrapper.putMap("member", memberMap);

            Utils.sendEvent(getReactApplicationContext(), "memberAdded", wrapper);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Could not handle event", e);
        }
    }

    @Override
    public void onMemberUpdated(Member member, Member.UpdateReason updateReason) {
        Log.d(LOG_TAG, "onMemberUpdated: " + member.getSid());
        try {
            WritableMap wrapper = new WritableNativeMap();
            JSONObject memberJson = Utils.memberToJsonObject(member);

            WritableMap memberMap = Utils.convertJsonToMap(memberJson);

            wrapper.putString("channelSid", member.getChannel().getSid());
            wrapper.putMap("member", memberMap);
            wrapper.putString("reason", updateReason.name());

            Utils.sendEvent(getReactApplicationContext(), "memberUpdated", wrapper);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Could not handle event", e);
        }
    }

    @Override
    public void onMemberDeleted(Member member) {
        Log.d(LOG_TAG, "onMemberDeleted: " + member.getSid());
        try {
            WritableMap wrapper = new WritableNativeMap();
            JSONObject memberJson = Utils.memberToJsonObject(member);

            WritableMap memberMap = Utils.convertJsonToMap(memberJson);

            wrapper.putString("channelSid", member.getChannel().getSid());
            wrapper.putMap("member", memberMap);

            Utils.sendEvent(getReactApplicationContext(), "memberDeleted", wrapper);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Could not handle event", e);
        }
    }

    @Override
    public void onTypingStarted(Channel channel, Member member) {
        Log.d(LOG_TAG, "onTypingStarted: " + member.getSid());
        try {
            WritableMap wrapper = new WritableNativeMap();
            JSONObject memberJson = Utils.memberToJsonObject(member);

            WritableMap memberMap = Utils.convertJsonToMap(memberJson);

            wrapper.putString("channelSid", channel.getSid());
            wrapper.putMap("member", memberMap);

            Utils.sendEvent(getReactApplicationContext(), "typingStarted", wrapper);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Could not handle event", e);
        }
    }

    @Override
    public void onTypingEnded(Channel channel, Member member) {
        Log.d(LOG_TAG, "onTypingEnded: " + member.getSid());
        try {
            WritableMap wrapper = new WritableNativeMap();
            JSONObject memberJson = Utils.memberToJsonObject(member);

            WritableMap memberMap = Utils.convertJsonToMap(memberJson);

            wrapper.putString("channelSid", channel.getSid());
            wrapper.putMap("member", memberMap);

            Utils.sendEvent(getReactApplicationContext(), "typingEnded", wrapper);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Could not handle event", e);
        }
    }

    @Override
    public void onSynchronizationChanged(Channel channel) {
        Log.d(LOG_TAG, "onSynchronizationChanged");
        WritableMap wrapper = new WritableNativeMap();
        wrapper.putString("channelSid", channel.getSid());
        wrapper.putString("status", channel.getSynchronizationStatus() == null ? null : channel.getSynchronizationStatus().name());
        Utils.sendEvent(getReactApplicationContext(), "channelSynchronizationStatusUpdated", wrapper);
    }
}
