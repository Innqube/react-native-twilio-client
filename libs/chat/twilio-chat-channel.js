import {NativeModules} from 'react-native';
import EventEmitter from 'events';

/**
 * @author Enrique Viard.
 *         Copyright © 2019 InnQube. All rights reserved.
 */

const {
    RNTwilioChatChannels,
} = NativeModules;

class TwilioChatChannel {

    _eventEmitter;

    constructor(props) {
        this._eventEmitter = new EventEmitter();
        this.uniqueName = props.uniqueName;
        this.friendlyName = props.friendlyName;
        this.sid = props.sid;
        this.lastMessageIndex = props.lastMessageIndex;
        this.attributes = props.attributes;
        this.type = props.type;
    }

    // Events delegation
    addListener = this._eventEmitter.addListener;
    removeAllListeners = this._eventEmitter.removeAllListeners;

    onNewMessage = (message) => dispatchEvent(new CustomEvent('onNewMessage', {detail: message}));

    get = (uniqueName) => RNTwilioChatChannels.get(uniqueName);

    join = () => RNTwilioChatChannels.join(this.uniqueName);

    leave = () => RNTwilioChatChannels.leave(this.uniqueName);

    typing = () => RNTwilioChatChannels.typing(this.uniqueName);

    getUnconsumedMessagesCount = () => RNTwilioChatChannels.getUnconsumedMessagesCount(this.uniqueName);

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


    _onMessageAdded = (message) => this._eventEmitter.emit('messageAdded', message);
    _onMessageUpdated = (message) => this._eventEmitter.emit('messageUpdated', message);
    _onMessageDeleted = (message) => this._eventEmitter.emit('messageDeleted', message);

    _onMemberAdded = (member) => this._eventEmitter.emit('memberAdded', member);
    _onMemberUpdated = (member) => this._eventEmitter.emit('memberUpdated', member);
    _onMemberDeleted = (member) => this._eventEmitter.emit('memberDeleted', member);

    _onTypingStarted = (member) => this._eventEmitter.emit('typingStarted', member);
    _onTypingEnded = (member) => this._eventEmitter.emit('typingEnded', member);

    _channelSynchronizationStatusUpdated = () => this._eventEmitter('channelSynchronizationStatusUpdated');

}

export default TwilioChatChannel;
