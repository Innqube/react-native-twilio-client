package com.ngs.react.RNTwilioChat;

import android.os.Parcel;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.twilio.chat.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class TwilioChatChannelModule extends ReactContextBaseJavaModule {

    private static final String LOG_TAG = "[IIMobile-Channel]";
    private Channel channel;

    public TwilioChatChannelModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "RNTwilioChatChannel";
    }

    public String getSid() {
        return channel.getSid();
    }

    public String getFriendlyName() {
        return channel.getFriendlyName();
    }

    public Channel.NotificationLevel getNotificationLevel() {
        return channel.getNotificationLevel();
    }

    public void setFriendlyName(String friendlyName, StatusListener listener) {
        channel.setFriendlyName(friendlyName, listener);
    }

    public void setNotificationLevel(Channel.NotificationLevel notificationLevel, StatusListener listener) {
        channel.setNotificationLevel(notificationLevel, listener);
    }

    public Channel.ChannelType getType() {
        return channel.getType();
    }

    public String getUniqueName() {
        return channel.getUniqueName();
    }

    public void setUniqueName(String uniqueName, StatusListener listener) {
        channel.setUniqueName(uniqueName, listener);
    }

    public JSONObject getAttributes() throws JSONException {
        return channel.getAttributes();
    }

    public void setAttributes(JSONObject updatedAttributes, StatusListener listener) {
        channel.setAttributes(updatedAttributes, listener);
    }

    public Messages getMessages() {
        return channel.getMessages();
    }

    public Channel.ChannelStatus getStatus() {
        return channel.getStatus();
    }

    public void addListener(ChannelListener listener) {
        channel.addListener(listener);
    }

    public void removeListener(ChannelListener listener) {
        channel.removeListener(listener);
    }

    public void removeAllListeners() {
        channel.removeAllListeners();
    }

    public Members getMembers() {
        return channel.getMembers();
    }

    public void join(StatusListener listener) {
        channel.join(listener);
    }

    public void leave(StatusListener listener) {
        channel.leave(listener);
    }

    public void destroy(StatusListener listener) {
        channel.destroy(listener);
    }

    public void declineInvitation(StatusListener listener) {
        channel.declineInvitation(listener);
    }

    public void typing() {
        channel.typing();
    }

    public Channel.SynchronizationStatus getSynchronizationStatus() {
        return channel.getSynchronizationStatus();
    }

    public String getDateCreated() {
        return channel.getDateCreated();
    }

    public Date getDateCreatedAsDate() {
        return channel.getDateCreatedAsDate();
    }

    public String getCreatedBy() {
        return channel.getCreatedBy();
    }

    public String getDateUpdated() {
        return channel.getDateUpdated();
    }

    public Date getDateUpdatedAsDate() {
        return channel.getDateUpdatedAsDate();
    }

    public Date getLastMessageDate() {
        return channel.getLastMessageDate();
    }

    public Long getLastMessageIndex() {
        return channel.getLastMessageIndex();
    }

    public void getMessagesCount(CallbackListener<Long> listener) {
        channel.getMessagesCount(listener);
    }

    public void getUnconsumedMessagesCount(CallbackListener<Long> listener) {
        channel.getUnconsumedMessagesCount(listener);
    }

    public void getMembersCount(CallbackListener<Long> listener) {
        channel.getMembersCount(listener);
    }

    public int describeContents() {
        return channel.describeContents();
    }

    public void writeToParcel(Parcel dest, int flags) {
        channel.writeToParcel(dest, flags);
    }

    public void dispose() {
        channel.dispose();
    }
}
