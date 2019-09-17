import {NativeModules} from 'react-native';
import EventEmitterHelper from '../event-emitter-helper';

/**
 * @author Enrique Viard.
 *         Copyright © 2019 InnQube. All rights reserved.
 */

const {
    RNTwilioChatChannels,
} = NativeModules;

class TwilioChatChannel {

    constructor(props) {
        this.uniqueName = props.uniqueName;
        this.friendlyName = props.friendlyName;
        this.sid = props.sid;
        this.lastMessageIndex = props.lastMessageIndex;
        this.attributes = props.attributes;
        this.type = props.type;

        this._initListeners();
    }

    onNewMessage = (message) => dispatchEvent(new CustomEvent('onNewMessage', message));

    join = () => RNTwilioChatChannels.join(this.uniqueName);

    leave = () => RNTwilioChatChannels.leave(this.uniqueName);

    typing = () => RNTwilioChatChannels.typing(this.uniqueName);

    getUnreadMessagesCount = () => RNTwilioChatChannels.getUnreadMessagesCount(this.uniqueName);

    getMessagesCount = () => RNTwilioChatChannels.getMessagesCount(this.uniqueName);

    getMembersCount = () => RNTwilioChatChannels.getMembersCount(this.uniqueName);

    getLastMessages = (count = 10) => RNTwilioChatChannels.getLastMessages(this.uniqueName, count);

    getMessagesBefore = (index, count) => RNTwilioChatChannels.getMessagesBefore(this.uniqueName, index, count);

    getMessagesAfter = (index, count) => RNTwilioChatChannels.getMessagesAfter(this.uniqueName, index, count);

    setNoMessagesConsumed = () => RNTwilioChatChannels.setNoMessagesConsumed(this.uniqueName);

    setAllMessagesConsumed = () => RNTwilioChatChannels.setAllMessagesConsumed(this.uniqueName);

    setLastConsumedMessage = (index) => RNTwilioChatChannels.setLastConsumedMessage(this.uniqueName, index);

    advanceLastConsumedMessage = (index) => RNTwilioChatChannels.advanceLastConsumedMessage(this.uniqueName, index);

    getLastConsumedMessageIndex = () => RNTwilioChatChannels.getLastConsumedMessageIndex(this.uniqueName, );

    sendMessage = (message) => RNTwilioChatChannels.sendMessage(message);

    getChannelMembers = () => RNTwilioChatChannels.getChannelMembers(this.uniqueName);

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

    _onTypingEndedOnChannel = (evt) => {
        dispatchEvent(new CustomEvent('typingEndedOnChannel', evt));
    };
};

export default TwilioChatChannel;
