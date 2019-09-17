import {NativeModules} from 'react-native';

/**
 * @author Enrique Viard.
 *         Copyright © 2019 InnQube. All rights reserved.
 */

const {
    RNTwilioChatClient,
} = NativeModules;

const ChatChannel = function (props) {

    this.uniqueName = props.uniqueName;
    this.friendlyName = props.friendlyName;
    this.sid = props.sid;
    this.lastMessageIndex = props.lastMessageIndex;
    this.attributes = props.attributes;

    this.getMessages = function (index, count) {
        return RNTwilioChatClient.getMessages(this.uniqueName, index, count);
    };

    this.getUnconsumedMessagesCount = function () {
        return RNTwilioChatClient.getUnconsumedMessagesCount(this.uniqueName);
    };

    this.onNewMessage = function (message) {
        dispatchEvent(new CustomEvent('onNewMessage', message));
    };

    this.getChannelMembers = function () {
        return RNTwilioChatClient.getChannelMembers();
    };

    this.getLastMessages = function (count) {
        if (typeof count !== 'number') {
            throw new Error('Count is required and must be a number');
        }
        return RNTwilioChatClient.getLastMessages(count);
    };

    this.getMessagesBefore = function (index, count) {
        if (typeof count !== 'number') {
            throw new Error('Count is required and must be a number');
        }
        if (typeof index !== 'number') {
            throw new Error('Index is required and must be a number');
        }
        return RNTwilioChatClient.getMessagesBefore(index, count);
    };

    this.getMessagesAfter = function (index, count) {
        if (typeof count !== 'number') {
            throw new Error('Count is required and must be a number');
        }
        if (typeof index !== 'number') {
            throw new Error('Index is required and must be a number');
        }
        return RNTwilioChatClient.getMessagesAfter(index, count);
    };

    this.typing = function () {
        RNTwilioChatClient.typing();
    };

    this.getUnreadMessagesCount = function () {
        return RNTwilioChatClient.getUnreadMessagesCount();
    };

    this.getLastConsumedMessageIndex = function () {
        return RNTwilioChatClient.getLastConsumedMessageIndex();
    };

    this.getMessagesCount = function () {
        return RNTwilioChatClient.getMessagesCount();
    };

    this.setNoMessagesConsumed = function () {
        return RNTwilioChatClient.setNoMessagesConsumed();
    };

    this.setAllMessagesConsumed = function () {
        return RNTwilioChatClient.setAllMessagesConsumed();
    };

    this.setLastConsumedMessage = function (index) {
        return RNTwilioChatClient.setLastConsumedMessage(index);
    };

    this.advanceLastConsumedMessage = function (index) {
        return RNTwilioChatClient.advanceLastConsumedMessage(index);
    };
};

export default ChatChannel;
