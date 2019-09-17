import {NativeModules} from 'react-native';

/**
 * @author Enrique Viard.
 *         Copyright © 2019 InnQube. All rights reserved.
 */

const {
    RNTwilioChatChannel,
} = NativeModules;

const TwilioChatChannel = (props) => {

    this.uniqueName = props.uniqueName;
    this.friendlyName = props.friendlyName;
    this.sid = props.sid;
    this.lastMessageIndex = props.lastMessageIndex;
    this.attributes = props.attributes;

    this.onNewMessage = (message) => dispatchEvent(new CustomEvent('onNewMessage', message));

    this.join = (uniqueName, friendlyName, type) => RNTwilioChatChannel.join(uniqueName, friendlyName, type);

    this.leave = () => RNTwilioChatChannel.leave();

    this.typing = () => RNTwilioChatChannel.typing();

    this.getUnreadMessagesCount = () => RNTwilioChatChannel.getUnreadMessagesCount();

    this.getMessagesCount = () => RNTwilioChatChannel.getMessagesCount();

    this.getMembersCount = () => RNTwilioChatChannel.getMembersCount();

    this.getLastMessages = (count = 10) =>  RNTwilioChatChannel.getLastMessages(count);

    this.getMessages = (index, count) => RNTwilioChatChannel.getMessages(this.uniqueName, index, count);

    this.getMessagesBefore = (index, count) => RNTwilioChatChannel.getMessagesBefore(index, count);

    this.getMessagesAfter = (index, count) => RNTwilioChatChannel.getMessagesAfter(index, count);

    this.setNoMessagesConsumed = () => RNTwilioChatChannel.setNoMessagesConsumed();

    this.setAllMessagesConsumed = () => RNTwilioChatChannel.setAllMessagesConsumed();

    this.setLastConsumedMessage = (index) => RNTwilioChatChannel.setLastConsumedMessage(index);

    this.advanceLastConsumedMessage = (index) => RNTwilioChatChannel.advanceLastConsumedMessage(index);

    this.getLastConsumedMessageIndex = () => RNTwilioChatChannel.getLastConsumedMessageIndex();

    this.sendMessage = (message) =>  RNTwilioChatChannel.sendMessage(message);

    this.getChannelMembers = () => RNTwilioChatChannel.getChannelMembers();
};

export default TwilioChatChannel;
