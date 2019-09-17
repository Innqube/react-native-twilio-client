import {NativeModules} from 'react-native';
import EventEmitterHelper from '../event-emitter-helper';

/**
 * @author Enrique Viard.
 *         Copyright © 2019 InnQube. All rights reserved.
 */

const {
    RNTwilioChatChannel,
} = NativeModules;

class TwilioChatChannel {

    constructor(props) {
        this.uniqueName = props.uniqueName;
        this.friendlyName = props.friendlyName;
        this.sid = props.sid;
        this.lastMessageIndex = props.lastMessageIndex;
        this.attributes = props.attributes;

        this._initListeners();
    }

    onNewMessage = (message) => dispatchEvent(new CustomEvent('onNewMessage', message));

    join = (uniqueName, friendlyName, type) => RNTwilioChatChannel.join(uniqueName, friendlyName, type);

    leave = () => RNTwilioChatChannel.leave();

    typing = () => RNTwilioChatChannel.typing();

    getUnreadMessagesCount = () => RNTwilioChatChannel.getUnreadMessagesCount();

    getMessagesCount = () => RNTwilioChatChannel.getMessagesCount();

    getMembersCount = () => RNTwilioChatChannel.getMembersCount();

    getLastMessages = (count = 10) => RNTwilioChatChannel.getLastMessages(count);

    getMessages = (index, count) => RNTwilioChatChannel.getMessages(uniqueName, index, count);

    getMessagesBefore = (index, count) => RNTwilioChatChannel.getMessagesBefore(index, count);

    getMessagesAfter = (index, count) => RNTwilioChatChannel.getMessagesAfter(index, count);

    setNoMessagesConsumed = () => RNTwilioChatChannel.setNoMessagesConsumed();

    setAllMessagesConsumed = () => RNTwilioChatChannel.setAllMessagesConsumed();

    setLastConsumedMessage = (index) => RNTwilioChatChannel.setLastConsumedMessage(index);

    advanceLastConsumedMessage = (index) => RNTwilioChatChannel.advanceLastConsumedMessage(index);

    getLastConsumedMessageIndex = () => RNTwilioChatChannel.getLastConsumedMessageIndex();

    sendMessage = (message) => RNTwilioChatChannel.sendMessage(message);

    getChannelMembers = () => RNTwilioChatChannel.getChannelMembers();

    _initListeners = () => {
        EventEmitterHelper.addEventListener('typingStartedOnChannel', this._onTypingStartedOnChannel);
        EventEmitterHelper.addEventListener('typingEndedOnChannel', this._onTypingEndedOnChannel);
    };

    _removeListeners = () => {
        EventEmitterHelper.removeEventListener('typingStartedOnChannel', this._onTypingStartedOnChannel);
        EventEmitterHelper.removeEventListener('typingEndedOnChannel', this._onTypingEndedOnChannel);
    };

    _onTypingStartedOnChannel = (evt) => {
        dispatchEvent(new CustomEvent('typingStartedOnChannel', evt));
    };

    _onTypingStartedOnChannel = (evt) => {
        dispatchEvent(new CustomEvent('typingEndedOnChannel', evt));
    };
};

export default TwilioChatChannel;
