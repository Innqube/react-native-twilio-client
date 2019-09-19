import {NativeModules,} from 'react-native';
import EventEmitterHelper from '../event-emitter-helper';
import SynchronizationStatus from '../domain/synchronization-status';
import TwilioChatChannel from "./twilio-chat-channel";

const {
    RNTwilioChatClient,
    RNTwilioChatChannels
} = NativeModules;

class TwilioChatClient {

    _channels = {};

    static getInstance() {
        if (!this._instance) {
            this._instance = new TwilioChatClient();
        }
        return this._instance;
    }

    create = (tokenCallback) => {
        this._initEventListeners();
        this._tokenCallback = tokenCallback;
        return new Promise(((resolve, reject) => {
            tokenCallback()
                .then(token => {
                    RNTwilioChatClient
                        .createClient(token, null)
                        .then(client => EventEmitterHelper.addEventListener(
                            'synchronizationStatusUpdated',
                            status => this._synchronizationListener(status, tokenCallback, resolve, reject)
                            )
                        )
                        .catch(error => reject(error))
                })
                .catch(error => reject(error));
        }));
    };

    shutdown = () => {
        this._removeAllListeners();
        RNTwilioChatClient.shutdown();
    };

    register = (token) => RNTwilioChatClient.register(token);

    unRegister = (token) => RNTwilioChatClient.unRegister(token);

    createChannel = (uniqueName, friendlyName, type = 0, attributes = {}) => RNTwilioChatChannels.create(uniqueName, friendlyName, type, attributes);

    getPublicChannels = () => RNTwilioChatClient
        .getPublicChannels()
        .then(channels => Promise.resolve(channels.map(c => this._buildChatChannel(c))));

    getUserChannels = () => RNTwilioChatClient
        .getUserChannels()
        .then(channels => Promise.resolve(channels.map(c => this._buildChatChannel(c))));

    getChannel = (channelSidOrUniqueName) => RNTwilioChatChannels
        .getChannel(channelSidOrUniqueName)
        .then(channel => Promise.resolve(this._buildChatChannel(channel)));

    _buildChatChannel = (channel) => {
        const twilioChatChannel = new TwilioChatChannel(channel);
        this._channels[twilioChatChannel.sid] = twilioChatChannel;
        return twilioChatChannel;
    };

    _synchronizationListener = (status, resolve, reject) => {
        switch (status) {
            case SynchronizationStatus.COMPLETED:
                dispatchEvent(new CustomEvent('synchronizationStatusUpdated', {detail: SynchronizationStatus.COMPLETED}));
                resolve(this);
                break;
            case SynchronizationStatus.STARTED:
                dispatchEvent(new CustomEvent('synchronizationStatusUpdated', {detail: SynchronizationStatus.STARTED}));
                break;
            case SynchronizationStatus.CHANNELS_COMPLETED:
                dispatchEvent(new CustomEvent('synchronizationStatusUpdated', {detail: SynchronizationStatus.CHANNELS_COMPLETED}));
                break;
            case SynchronizationStatus.FAILED:
                dispatchEvent(new CustomEvent('synchronizationStatusUpdated', {detail: SynchronizationStatus.FAILED}));
                reject('Synchronization failed');
        }
    };

// _messageFilter(message, channel) {
//     if (message.channel.sid === channel.sid) {
//         if (channel.onNewMessage) {
//             channel.onNewMessage(message);
//         }
//     }
// }

    _initEventListeners = () => {
        EventEmitterHelper.addEventListener('tokenAboutToExpire', this._onTokenAboutToExpire);
        EventEmitterHelper.addEventListener('tokenExpired', this._onTokenAboutToExpire);
        // EventEmitterHelper.addEventListener('channelJoined', this._onChannelJoined);
        // EventEmitterHelper.addEventListener('channelInvited', this._onChannelInvited);
        EventEmitterHelper.addEventListener('channelAdded', this._onChannelAdded);
        EventEmitterHelper.addEventListener('channelUpdated', this._onChannelUpdated);
        EventEmitterHelper.addEventListener('channelDeleted', this._onChannelDeleted);
        EventEmitterHelper.addEventListener('userUpdated', this._onUserUpdated);
        EventEmitterHelper.addEventListener('userSubscribed', this._onUserSubscribed);
        EventEmitterHelper.addEventListener('userUnsubscribed', this._onUserUnsubscribed);
        EventEmitterHelper.addEventListener('newMessageNotification', this._onNewMessageNotification);
        EventEmitterHelper.addEventListener('addedToChannelNotification', this._onAddedToChannelNotification);
        EventEmitterHelper.addEventListener('invitedToChannelNotification', this._onInvitedToChannelNotification);
        EventEmitterHelper.addEventListener('removedFromChannelNotification', this._onRemovedFromChannelNotification);
        // EventEmitterHelper.addEventListener('notificationSubscribed', this._onNotificationSubscribed);
        EventEmitterHelper.addEventListener('connectionStateUpdated', this._onConnectionStateUpdated);
        EventEmitterHelper.addEventListener('channelSynchronizationStatusUpdated', this._onChannelSynchronizationStatusUpdated);

        EventEmitterHelper.addEventListener('messageAdded', this._onMessageAdded);
        EventEmitterHelper.addEventListener('messageUpdated', this._onMessageUpdated);
        EventEmitterHelper.addEventListener('messageDeleted', this._onMessageDeleted);
        EventEmitterHelper.addEventListener('memberAdded', this._onMemberAdded);
        EventEmitterHelper.addEventListener('memberUpdated', this._onMemberUpdated);
        EventEmitterHelper.addEventListener('memberDeleted', this._onMemberDeleted);
        EventEmitterHelper.addEventListener('typingStarted', this._onTypingStarted);
        EventEmitterHelper.addEventListener('typingEnded', this._onTypingEnded);
        EventEmitterHelper.addEventListener('error', this._onError);
    };

    _removeAllListeners = () => {
        EventEmitterHelper.removeEventListener('synchronizationStatusUpdated', this._synchronizationListener);
        EventEmitterHelper.removeEventListener('tokenAboutToExpire', this._onTokenAboutToExpire);
        EventEmitterHelper.removeEventListener('tokenExpired', this._onTokenAboutToExpire);
        // EventEmitterHelper.removeEventListener('channelJoined', this._onChannelJoined);
        // EventEmitterHelper.removeEventListener('channelInvited', this._onChannelInvited);
        EventEmitterHelper.removeEventListener('channelAdded', this._onChannelAdded);
        EventEmitterHelper.removeEventListener('channelUpdated', this._onChannelUpdated);
        EventEmitterHelper.removeEventListener('channelDeleted', this._onChannelDeleted);
        EventEmitterHelper.removeEventListener('userUpdated', this._onUserUpdated);
        EventEmitterHelper.removeEventListener('userSubscribed', this._onUserSubscribed);
        EventEmitterHelper.removeEventListener('userUnsubscribed', this._onUserUnsubscribed);
        EventEmitterHelper.removeEventListener('newMessageNotification', this._onNewMessageNotification);
        EventEmitterHelper.removeEventListener('addedToChannelNotification', this._onAddedToChannelNotification);
        EventEmitterHelper.removeEventListener('invitedToChannelNotification', this._onInvitedToChannelNotification);
        EventEmitterHelper.removeEventListener('removedFromChannelNotification', this._onRemovedFromChannelNotification);
        // EventEmitterHelper.removeEventListener('notificationSubscribed', this._onNotificationSubscribed);
        EventEmitterHelper.removeEventListener('connectionStateUpdated', this._onConnectionStateUpdated);
        EventEmitterHelper.removeEventListener('channelSynchronizationStatusUpdated', this._onChannelSynchronizationStatusUpdated);

        EventEmitterHelper.removeEventListener('messageAdded', this._onMessageAdded);
        EventEmitterHelper.removeEventListener('messageUpdated', this._onMessageUpdated);
        EventEmitterHelper.removeEventListener('messageDeleted', this._onMessageDeleted);
        EventEmitterHelper.removeEventListener('memberAdded', this._onMemberAdded);
        EventEmitterHelper.removeEventListener('memberUpdated', this._onMemberUpdated);
        EventEmitterHelper.removeEventListener('memberDeleted', this._onMemberDeleted);
        EventEmitterHelper.removeEventListener('typingStarted', this._onTypingStarted);
        EventEmitterHelper.removeEventListener('typingEnded', this._onTypingEnded);
        EventEmitterHelper.removeEventListener('error', this._onError);
    };

    _onTokenAboutToExpire = async () => RNTwilioChatClient.updateClient(await this._tokenCallback().jwt);
    _onChannelJoined = (payload) => dispatchEvent(new CustomEvent('channelJoined', {detail: payload}));
    _onChannelInvited = (payload) => dispatchEvent(new CustomEvent('channelInvited', {detail: payload}));
    _onChannelAdded = (payload) => dispatchEvent(new CustomEvent('channelAdded', {detail: payload}));
    _onChannelUpdated = (payload) => dispatchEvent(new CustomEvent('channelUpdated', {detail: payload}));
    _onChannelDeleted = (payload) => dispatchEvent(new CustomEvent('channelDeleted', {detail: payload}));
    _onUserUpdated = (payload) => dispatchEvent(new CustomEvent('userUpdated', {detail: payload}));
    _onUserSubscribed = (payload) => dispatchEvent(new CustomEvent('userSubscribed', {detail: payload}));
    _onUserUnsubscribed = (payload) => dispatchEvent(new CustomEvent('userUnsubscribed', {detail: payload}));
    _onNewMessageNotification = (payload) => dispatchEvent(new CustomEvent('newMessageNotification', {detail: payload}));
    _onAddedToChannelNotification = (payload) => dispatchEvent(new CustomEvent('addedToChannelNotification', {detail: payload}));
    _onInvitedToChannelNotification = (payload) => dispatchEvent(new CustomEvent('invitedToChannelNotification', {detail: payload}));
    _onRemovedFromChannelNotification = (payload) => dispatchEvent(new CustomEvent('removedFromChannelNotification', {detail: payload}));
    _onNotificationSubscribed = (payload) => dispatchEvent(new CustomEvent('notificationSubscribed', {detail: payload}));
    _onConnectionStateUpdated = (payload) => dispatchEvent(new CustomEvent('connectionStateUpdated', {detail: payload}));
    _onChannelSynchronizationStatusUpdated = (payload) => dispatchEvent(new CustomEvent('channelSynchronizationStatusUpdated', {detail: payload}));

    _onMessageAdded = (payload) => this._channels[payload.channelSid]._onMessageAdded(payload.message);
    _onMessageDeleted = (payload) => this._channels[payload.channelSid]._onMessageDeleted(payload.message);
    _onMessageUpdated = (payload) => this._channels[payload.channelSid]._onMessageUpdated(payload.message);
    _onMemberAdded = (payload) => this._channels[payload.channelSid]._onMemberAdded(payload.message);
    _onMemberUpdated = (payload) => this._channels[payload.channelSid]._onMemberUpdated(payload.message);
    _onMemberDeleted = (payload) => this._channels[payload.channelSid]._onMemberDeleted(payload.message);
    _onTypingStarted = (payload) => this._channels[payload.channelSid]._onTypingStarted(payload.member);
    _onTypingEnded = (payload) => this._channels[payload.channelSid]._onTypingEnded(payload.member);

    _onError = (payload) => dispatchEvent(new CustomEvent('error', {detail: payload}));
}

export default TwilioChatClient;
