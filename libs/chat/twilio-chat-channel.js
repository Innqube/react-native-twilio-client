import {NativeModules} from 'react-native';

/**
 * @author Enrique Viard.
 *         Copyright © 2019 InnQube. All rights reserved.
 */

const {
    RNTwilioChatChannel,
    RNTwilioChatMessage,
    RNTwilioChatMember
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

    this.getLastMessages = (count = 10) =>  RNTwilioChatMessage.getLastMessages(count);
    this.getMessages = (index, count) => RNTwilioChatMessage.getMessages(this.uniqueName, index, count);
    this.getMessagesBefore = (index, count) => RNTwilioChatMessage.getMessagesBefore(index, count);
    this.getMessagesAfter = (index, count) => RNTwilioChatMessage.getMessagesAfter(index, count);
    this.setNoMessagesConsumed = () => RNTwilioChatMessage.setNoMessagesConsumed();
    this.setAllMessagesConsumed = () => RNTwilioChatMessage.setAllMessagesConsumed();
    this.setLastConsumedMessage = (index) => RNTwilioChatMessage.setLastConsumedMessage(index);
    this.advanceLastConsumedMessage = (index) => RNTwilioChatMessage.advanceLastConsumedMessage(index);
    this.getLastConsumedMessageIndex = () => RNTwilioChatMessage.getLastConsumedMessageIndex();
    this.sendMessage = (message) =>  RNTwilioChatMessage.sendMessage(message);

    this.getChannelMembers = () => RNTwilioChatMember.getChannelMembers();
};

export default TwilioChatChannel;
