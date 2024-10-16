import {NativeAppEventEmitter, NativeModules, Platform} from 'react-native';
import SynchronizationStatus from '../domain/synchronization-status';
import TwilioChatChannel from "./twilio-chat-channel";
import EventEmitter from '../event-emitter';

const {
    RNTwilioChatClient,
    RNTwilioChatChannels
} = NativeModules;

class TwilioChatClient {

    _channels = {};
    _eventEmitter;
    _listener;

    static getInstance() {
        if (!this._instance) {
            this._instance = new TwilioChatClient();
        }
        return this._instance;
    }

    constructor() {
        this._eventEmitter = new EventEmitter();
    }

    create = (tokenCallback) => {
        this.shutdown();
        this._initEventListeners();
        this._tokenCallback = tokenCallback;
        return new Promise(((resolve, reject) => {
            tokenCallback()
                .then(token => {

                    const _firstSynchronizationListener = (payload) => {
                        switch (payload.status) {
                            case SynchronizationStatus.COMPLETED:
                                this._listener.remove();
                                resolve(this);
                                break;
                            case SynchronizationStatus.FAILED:
                                this._listener.remove();
                                reject('Synchronization failed');
                                break;
                        }
                    };

                    RNTwilioChatClient
                        .createClient(token, null)
                        .then(payload => {
                            if (payload.status === 'COMPLETED') {
                                resolve(this);
                            } else {
                                this._listener = NativeAppEventEmitter.addListener(
                                    'synchronizationStatusUpdated',
                                    _firstSynchronizationListener
                                )
                            }
                        })
                        .then(() => NativeAppEventEmitter.addListener('synchronizationStatusUpdated', this._synchronizationListener))
                        .catch(error => reject(error))
                })
                .catch(error => reject(error));
        }));
    };

    shutdown = () => {
        this._removeAllListeners();
        RNTwilioChatClient.shutdown();
    };

    register = (token) => Platform.OS === 'ios' ? RNTwilioChatClient.register() : RNTwilioChatClient.register(token);

    unregister = (token) => Platform.OS === 'ios' ? RNTwilioChatClient.unregister() : RNTwilioChatClient.unregister(token);

    createChannel = (uniqueName, friendlyName, type = 0, attributes = {}) => RNTwilioChatChannels
        .create(uniqueName, friendlyName, type, attributes)
        .then(channel => Promise.resolve(this._buildChatChannel(channel)));

    getPublicChannels = () => RNTwilioChatClient
        .getPublicChannels()
        .then(channels => Promise.resolve(channels.map(c => this._buildChatChannel(c))));

    getUserChannels = () => RNTwilioChatClient
        .getUserChannels()
        .then(channels => Promise.resolve(channels.map(c => this._buildChatChannel(c))));

    getChannel = (channelSidOrUniqueName) => RNTwilioChatChannels
        .get(channelSidOrUniqueName)
        .then(channel => Promise.resolve(this._buildChatChannel(channel)));

    getDeviceToken = () => RNTwilioChatClient.getDeviceToken();

    _buildChatChannel = (channel) => {
        return this._channels[channel.sid] = this._channels[channel.sid] || new TwilioChatChannel(channel);
    };

    _synchronizationListener = (status) => {
        switch (status) {
            case SynchronizationStatus.COMPLETED:
                this._eventEmitter.emit('synchronizationStatusUpdated', SynchronizationStatus.COMPLETED);
                break;
            case SynchronizationStatus.STARTED:
                this._eventEmitter.emit('synchronizationStatusUpdated', SynchronizationStatus.STARTED);
                break;
            case SynchronizationStatus.CHANNELS_COMPLETED:
                this._eventEmitter.emit('synchronizationStatusUpdated', SynchronizationStatus.CHANNELS_COMPLETED);
                break;
            case SynchronizationStatus.FAILED:
                this._eventEmitter.emit('synchronizationStatusUpdated', SynchronizationStatus.FAILED);
        }
    };

    // Events delegation
    addListener = (name, handler) => this._eventEmitter.addListener(name, handler);
    removeAllListeners = (name) => this._eventEmitter.removeAllListeners(name);

    _initEventListeners = () => {
        NativeAppEventEmitter.addListener('tokenAboutToExpire', this._onTokenAboutToExpire);
        NativeAppEventEmitter.addListener('tokenExpired', this._onTokenAboutToExpire);
        NativeAppEventEmitter.addListener('channelAdded', this._onChannelAdded);
        NativeAppEventEmitter.addListener('channelUpdated', this._onChannelUpdated);
        NativeAppEventEmitter.addListener('channelDeleted', this._onChannelDeleted);
        NativeAppEventEmitter.addListener('userUpdated', this._onUserUpdated);
        NativeAppEventEmitter.addListener('userSubscribed', this._onUserSubscribed);
        NativeAppEventEmitter.addListener('userUnsubscribed', this._onUserUnsubscribed);
        NativeAppEventEmitter.addListener('newMessageNotification', this._onNewMessageNotification);
        NativeAppEventEmitter.addListener('addedToChannelNotification', this._onAddedToChannelNotification);
        NativeAppEventEmitter.addListener('invitedToChannelNotification', this._onInvitedToChannelNotification);
        NativeAppEventEmitter.addListener('removedFromChannelNotification', this._onRemovedFromChannelNotification);
        NativeAppEventEmitter.addListener('connectionStateUpdated', this._onConnectionStateUpdated);
        NativeAppEventEmitter.addListener('channelSynchronizationStatusUpdated', this._onChannelSynchronizationStatusUpdated);

        NativeAppEventEmitter.addListener('messageAdded', this._onMessageAdded);
        NativeAppEventEmitter.addListener('messageUpdated', this._onMessageUpdated);
        NativeAppEventEmitter.addListener('messageDeleted', this._onMessageDeleted);
        NativeAppEventEmitter.addListener('memberAdded', this._onMemberAdded);
        NativeAppEventEmitter.addListener('memberUpdated', this._onMemberUpdated);
        NativeAppEventEmitter.addListener('memberDeleted', this._onMemberDeleted);
        NativeAppEventEmitter.addListener('typingStarted', this._onTypingStarted);
        NativeAppEventEmitter.addListener('typingEnded', this._onTypingEnded);
        NativeAppEventEmitter.addListener('error', this._onError);
    };

    _removeAllListeners = () => {
        this.removeAllListeners('synchronizationStatusUpdated');
        this.removeAllListeners('tokenAboutToExpire');
        this.removeAllListeners('tokenExpired');
        this.removeAllListeners('channelAdded');
        this.removeAllListeners('channelUpdated');
        this.removeAllListeners('channelDeleted');
        this.removeAllListeners('userUpdated');
        this.removeAllListeners('userSubscribed');
        this.removeAllListeners('userUnsubscribed');
        this.removeAllListeners('newMessageNotification');
        this.removeAllListeners('addedToChannelNotification');
        this.removeAllListeners('invitedToChannelNotification');
        this.removeAllListeners('removedFromChannelNotification');
        this.removeAllListeners('notificationSubscribed');
        this.removeAllListeners('connectionStateUpdated');
        this.removeAllListeners('channelSynchronizationStatusUpdated');
        this.removeAllListeners('messageAdded');
        this.removeAllListeners('messageUpdated');
        this.removeAllListeners('messageDeleted');
        this.removeAllListeners('memberAdded');
        this.removeAllListeners('memberUpdated');
        this.removeAllListeners('memberDeleted');
        this.removeAllListeners('typingStarted');
        this.removeAllListeners('typingEnded');
        this.removeAllListeners('error');
    };

    _onTokenAboutToExpire = async () =>
        RNTwilioChatClient
            .updateClient(await this._tokenCallback())
            .catch(error => this._eventEmitter.emit('updateClientError', error.message));
    _onChannelJoined = (payload) => this._eventEmitter.emit('channelJoined', payload);
    _onChannelInvited = (payload) => this._eventEmitter.emit('channelInvited', payload);
    _onChannelAdded = (payload) => this._eventEmitter.emit('channelAdded', payload);
    _onChannelUpdated = (payload) => this._eventEmitter.emit('channelUpdated', payload);
    _onChannelDeleted = (payload) => this._eventEmitter.emit('channelDeleted', payload);
    _onUserUpdated = (payload) => this._eventEmitter.emit('userUpdated', payload);
    _onUserSubscribed = (payload) => this._eventEmitter.emit('userSubscribed', payload);
    _onUserUnsubscribed = (payload) => this._eventEmitter.emit('userUnsubscribed', payload);
    _onNewMessageNotification = (payload) => this._eventEmitter.emit('newMessageNotification', payload);
    _onAddedToChannelNotification = (payload) => this._eventEmitter.emit('addedToChannelNotification', payload);
    _onInvitedToChannelNotification = (payload) => this._eventEmitter.emit('invitedToChannelNotification', payload);
    _onRemovedFromChannelNotification = (payload) => this._eventEmitter.emit('removedFromChannelNotification', payload);
    _onNotificationSubscribed = (payload) => this._eventEmitter.emit('notificationSubscribed', payload);
    _onConnectionStateUpdated = (payload) => this._eventEmitter.emit('clientConnectionStateUpdated', payload.state);

    _onChannelSynchronizationStatusUpdated = (payload) => {
        let channel = this._channels[payload.channelSid];

        // Some channels events may arrive before the JS counterpart is created
        if (channel) {
            channel._onChannelSynchronizationStatusUpdated(payload.status);
        }

        this._eventEmitter.emit('channelSynchronizationStatusUpdated', payload);
    };
    _onMessageAdded = (payload) => this._channels[payload.channelSid]?._onMessageAdded(payload.message);
    _onMessageDeleted = (payload) => this._channels[payload.channelSid]?._onMessageDeleted(payload.message);
    _onMessageUpdated = (payload) => this._channels[payload.channelSid]?._onMessageUpdated(payload.message);
    _onMemberAdded = (payload) => this._channels[payload.channelSid]?._onMemberAdded(payload.message);
    _onMemberUpdated = (payload) => this._channels[payload.channelSid]?._onMemberUpdated(payload.message);
    _onMemberDeleted = (payload) => this._channels[payload.channelSid]?._onMemberDeleted(payload.message);
    _onTypingStarted = (payload) => this._channels[payload.channelSid]?._onTypingStarted(payload.member);
    _onTypingEnded = (payload) => this._channels[payload.channelSid]?._onTypingEnded(payload.member);

    _onError = (payload) => this._eventEmitter.emit('error', payload);
}

export default TwilioChatClient;
