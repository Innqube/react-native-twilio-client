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

    _initListeners = () => {
        EventEmitterHelper.addEventListener('messageAdded', this._onMessageAdded);
        EventEmitterHelper.addEventListener('messageUpdated', this._onMessageUpdated);
        EventEmitterHelper.addEventListener('messageRemoved', this._onMessageRemoved);
        EventEmitterHelper.addEventListener('memberAdded', this._onMemberAdded);
        EventEmitterHelper.addEventListener('memberUpdated', this._onMemberUpdated);
        EventEmitterHelper.addEventListener('memberRemoved', this._onMemberRemoved);
        EventEmitterHelper.addEventListener('typingStarted', this._onTypingStarted);
        EventEmitterHelper.addEventListener('typingEnded', this._onTypingEnded);
    };

    _removeListeners = () => {
        EventEmitterHelper.removeEventListener('messageAdded', this._onMessageAdded);
        EventEmitterHelper.removeEventListener('messageUpdated', this._onMessageUpdated);
        EventEmitterHelper.removeEventListener('messageRemoved', this._onMessageRemoved);
        EventEmitterHelper.removeEventListener('memberAdded', this._onMemberAdded);
        EventEmitterHelper.removeEventListener('memberUpdated', this._onMemberUpdated);
        EventEmitterHelper.removeEventListener('memberRemoved', this._onMemberRemoved);
        EventEmitterHelper.removeEventListener('typingStarted', this._onTypingStarted);
        EventEmitterHelper.removeEventListener('typingEnded', this._onTypingEnded);
    };

    _onMessageAdded = (payload) => dispatchEvent(new CustomEvent('messageAdded', {detail: payload}));
    _onMessageUpdated = (payload) => dispatchEvent(new CustomEvent('messageUpdated', {detail: payload}));
    _onMessageRemoved = (payload) => dispatchEvent(new CustomEvent('messageRemoved', {detail: payload}));
    _onMemberAdded = (payload) => dispatchEvent(new CustomEvent('memberAdded', {detail: payload}));
    _onMemberUpdated = (payload) => dispatchEvent(new CustomEvent('memberUpdated', {detail: payload}));
    _onMemberRemoved = (payload) => dispatchEvent(new CustomEvent('memberRemoved', {detail: payload}));
    _onTypingStarted = (payload) => dispatchEvent(new CustomEvent('typingStarted', {detail: payload}));
    _onTypingEnded = (payload) => dispatchEvent(new CustomEvent('typingEnded', {detail: payload}));
}

export default TwilioChatChannel;
