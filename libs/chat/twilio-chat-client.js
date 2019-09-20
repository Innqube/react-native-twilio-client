import {NativeModules,} from 'react-native';
import SynchronizationStatus from '../domain/synchronization-status';
import TwilioChatChannel from "./twilio-chat-channel";

const {
    RNTwilioChatClient,
    RNTwilioChatChannels
} = NativeModules;

class TwilioChatClient {

    _channels = {};
    _eventEmitter;

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
        this._initEventListeners();
        this._tokenCallback = tokenCallback;
        return new Promise(((resolve, reject) => {
            tokenCallback()
                .then(token => {
                    RNTwilioChatClient
                        .createClient(token, null)
                        .then(client => this.addListener(
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
        this.removeAllListeners();
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
                this._eventEmitter.emit('synchronizationStatusUpdated', SynchronizationStatus.COMPLETED);
                resolve(this);
                break;
            case SynchronizationStatus.STARTED:
                this._eventEmitter.emit('synchronizationStatusUpdated', SynchronizationStatus.STARTED);
                break;
            case SynchronizationStatus.CHANNELS_COMPLETED:
                this._eventEmitter.emit('synchronizationStatusUpdated', SynchronizationStatus.CHANNELS_COMPLETED);
                break;
            case SynchronizationStatus.FAILED:
                this._eventEmitter.emit('synchronizationStatusUpdated', SynchronizationStatus.FAILED);
                reject('Synchronization failed');
        }
    };

    // Events delegation
    addListener = (name, handler) => this._eventEmitter.addListener(name, handler);
    removeAllListeners = (name) => this._eventEmitter.removeAllListeners(name);

    _initEventListeners = () => {
        this.addListener('tokenAboutToExpire', this._onTokenAboutToExpire);
        this.addListener('tokenExpired', this._onTokenAboutToExpire);
        // EventEmitterHelper.addEventListener('channelJoined', this._onChannelJoined);
        // EventEmitterHelper.addEventListener('channelInvited', this._onChannelInvited);
        this.addListener('channelAdded', this._onChannelAdded);
        this.addListener('channelUpdated', this._onChannelUpdated);
        this.addListener('channelDeleted', this._onChannelDeleted);
        this.addListener('userUpdated', this._onUserUpdated);
        this.addListener('userSubscribed', this._onUserSubscribed);
        this.addListener('userUnsubscribed', this._onUserUnsubscribed);
        this.addListener('newMessageNotification', this._onNewMessageNotification);
        this.addListener('addedToChannelNotification', this._onAddedToChannelNotification);
        this.addListener('invitedToChannelNotification', this._onInvitedToChannelNotification);
        this.addListener('removedFromChannelNotification', this._onRemovedFromChannelNotification);
        // EventEmitterHelper.addEventListener('notificationSubscribed', this._onNotificationSubscribed);
        this.addListener('connectionStateUpdated', this._onConnectionStateUpdated);
        this.addListener('channelSynchronizationStatusUpdated', this._onChannelSynchronizationStatusUpdated);

        this.addListener('messageAdded', this._onMessageAdded);
        this.addListener('messageUpdated', this._onMessageUpdated);
        this.addListener('messageDeleted', this._onMessageDeleted);
        this.addListener('memberAdded', this._onMemberAdded);
        this.addListener('memberUpdated', this._onMemberUpdated);
        this.addListener('memberDeleted', this._onMemberDeleted);
        this.addListener('typingStarted', this._onTypingStarted);
        this.addListener('typingEnded', this._onTypingEnded);
        this.addListener('error', this._onError);
    };

    _removeAllListeners = () => {
        this.removeAllListeners('synchronizationStatusUpdated');
        this.removeAllListeners('tokenAboutToExpire');
        this.removeAllListeners('tokenExpired');
        // EventEmitterHelper.removeEventListener('channelJoined', this._onChannelJoined);
        // EventEmitterHelper.removeEventListener('channelInvited', this._onChannelInvited);
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

    _onTokenAboutToExpire = async () => RNTwilioChatClient.updateClient(await this._tokenCallback().jwt);
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
    _onConnectionStateUpdated = (payload) => this._eventEmitter.emit('connectionStateUpdated', payload);
    _onChannelSynchronizationStatusUpdated = (payload) => this._eventEmitter.emit('channelSynchronizationStatusUpdated', payload);

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
