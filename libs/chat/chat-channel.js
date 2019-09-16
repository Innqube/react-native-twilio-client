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

    // getMessagesAfter(index, count): Promise<Message[]> {
    //     if (typeof count !== 'number') {
    //         throw new Error('Count is required and must be a number');
    //     }
    //     if (typeof index !== 'number') {
    //         throw new Error('Index is required and must be a number');
    //     }
    //     return RNTwilioChatClient.getMessagesAfter(index, count);
    // },
    // typing() {
    //     RNTwilioChatClient.typing();
    // },
    // getUnreadMessagesCount() {
    //     return RNTwilioChatClient.getUnreadMessagesCount();
    // },
    // getLastConsumedMessageIndex() {
    //     return RNTwilioChatClient.getLastConsumedMessageIndex();
    // },
    // getMessagesCount() {
    //     return RNTwilioChatClient.getMessagesCount();
    // },
    // setNoMessagesConsumed() {
    //     return RNTwilioChatClient.setNoMessagesConsumed();
    // },
    // setAllMessagesConsumed() {
    //     return RNTwilioChatClient.setAllMessagesConsumed();
    // },
    // setLastConsumedMessage(index) {
    //     return RNTwilioChatClient.setLastConsumedMessage(index);
    // },
    // advanceLastConsumedMessage(index) {
    //     return RNTwilioChatClient.advanceLastConsumedMessage(index);
    // }

};

export default ChatChannel;
