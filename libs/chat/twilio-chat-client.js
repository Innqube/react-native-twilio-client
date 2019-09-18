import {NativeModules,} from 'react-native';
import EventEmitterHelper from '../event-emitter-helper';
import SynchronizationStatus from '../domain/synchronization-status';

const {
    RNTwilioChatClient,
    RNTwilioChatChannels
} = NativeModules;

class TwilioChatClient {

    constructor(props) {
        this.tokenCallback = props.tokenCallback;
    }

    create = () => new Promise(((resolve, reject) => {
        this.tokenCallback()
            .then(token => {
                RNTwilioChatClient
                    .createClient(token)
                    .then(client => EventEmitterHelper.addEventListener(
                        'synchronizationStatusUpdated',
                        status => this._synchronizationListener(status, resolve, reject)
                        )
                    )
                    .catch(error => reject(error))
            })
            .catch(error => reject(error));
    }));

    shutdown = () => {
        this._removeAllListeners();
        RNTwilioChatClient.shutdown();
    };

    register = (token) => RNTwilioChatClient.register(token);

    unRegister = (token) => RNTwilioChatClient.unRegister(token);

    createChannel = (uniqueName, friendlyName, type = 0, attributes = {}) => RNTwilioChatChannels.create(uniqueName, friendlyName, type, attributes);

    getPublicChannels = () => RNTwilioChatClient.getPublicChannels();

    getUserChannels = () => RNTwilioChatClient.getUserChannels();

    getChannel = (channelSidOrUniqueName) => RNTwilioChatChannels.getChannel(channelSidOrUniqueName);

    _synchronizationListener = (status, resolve, reject) => {
        switch (status) {
            case SynchronizationStatus.COMPLETED:
                this._initEventListeners();
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

    _messageFilter(message, channel) {
        if (message.channel.sid === channel.sid) {
            if (channel.onNewMessage) {
                channel.onNewMessage(message);
            }
        }
    }

    _initEventListeners = () => {
        EventEmitterHelper.addEventListener('tokenAboutToExpire', this._onTokenAboutToExpire);
        EventEmitterHelper.addEventListener('tokenExpired', this._onTokenAboutToExpire);
        EventEmitterHelper.addEventListener('channelJoined', this._onChannelJoined);
        EventEmitterHelper.addEventListener('channelInvited', this._onChannelInvited);
        EventEmitterHelper.addEventListener('channelAdded', this._onChannelAdded);
        EventEmitterHelper.addEventListener('channelUpdate', this._onChannelUpdate);
        EventEmitterHelper.addEventListener('channelDeleted', this._onChannelDeleted);
        EventEmitterHelper.addEventListener('userUpdated', this._onUserUpdated);
        EventEmitterHelper.addEventListener('userSubscribed', this._onUserSubscribed);
        EventEmitterHelper.addEventListener('userUnsubscribed', this._onUserUnsubscribed);
        EventEmitterHelper.addEventListener('addedToChannelNotification', this._onAddedToChannelNotification);
        EventEmitterHelper.addEventListener('invitedToChannelNotification', this._onInvitedToChannelNotification);
        EventEmitterHelper.addEventListener('removedFromChannelNotification', this._onRemovedFromChannelNotification);
        EventEmitterHelper.addEventListener('notificationSubscribed', this._onNotificationSubscribed);
        EventEmitterHelper.addEventListener('connectionStateChanged', this._onConnectionStateChanged);
    };

    _removeAllListeners = () => {
        EventEmitterHelper.removeEventListener('synchronizationStatusUpdated', this._synchronizationListener);
        EventEmitterHelper.removeEventListener('tokenAboutToExpire', this._onTokenAboutToExpire);
        EventEmitterHelper.removeEventListener('tokenExpired', this._onTokenAboutToExpire);
        EventEmitterHelper.removeEventListener('channelJoined', this._onChannelJoined);
        EventEmitterHelper.removeEventListener('channelInvited', this._onChannelInvited);
        EventEmitterHelper.removeEventListener('channelAdded', this._onChannelAdded);
        EventEmitterHelper.removeEventListener('channelUpdate', this._onChannelUpdate);
        EventEmitterHelper.removeEventListener('channelDeleted', this._onChannelDeleted);
        EventEmitterHelper.removeEventListener('userUpdated', this._onUserUpdated);
        EventEmitterHelper.removeEventListener('userSubscribed', this._onUserSubscribed);
        EventEmitterHelper.removeEventListener('userUnsubscribed', this._onUserUnsubscribed);
        EventEmitterHelper.removeEventListener('addedToChannelNotification', this._onAddedToChannelNotification);
        EventEmitterHelper.removeEventListener('invitedToChannelNotification', this._onInvitedToChannelNotification);
        EventEmitterHelper.removeEventListener('removedFromChannelNotification', this._onRemovedFromChannelNotification);
        EventEmitterHelper.removeEventListener('notificationSubscribed', this._onNotificationSubscribed);
        EventEmitterHelper.removeEventListener('connectionStateChanged', this._onConnectionStateChanged);
    };

    _onTokenAboutToExpire = async () => RNTwilioChatClient.updateClient(await this.tokenCallback());

    _onChannelJoined = (payload) => dispatchEvent(new CustomEvent('channelJoined', {detail: payload}));

    _onChannelInvited = (payload) => dispatchEvent(new CustomEvent('channelInvited', {detail: payload}));

    _onChannelAdded = (payload) => dispatchEvent(new CustomEvent('channelAdded', {detail: payload}));

    _onChannelUpdate = (payload) => dispatchEvent(new CustomEvent('channelUpdate', {detail: payload}));

    _onChannelDeleted = (payload) => dispatchEvent(new CustomEvent('channelDeleted', {detail: payload}));

    _onUserUpdated = (payload) => dispatchEvent(new CustomEvent('userUpdated', {detail: payload}));

    _onUserSubscribed = (payload) => dispatchEvent(new CustomEvent('userSubscribed', {detail: payload}));

    _onUserUnsubscribed = (payload) => dispatchEvent(new CustomEvent('userUnsubscribed', {detail: payload}));

    _onAddedToChannelNotification = (payload) => dispatchEvent(new CustomEvent('addedToChannelNotification', {detail: payload}));

    _onInvitedToChannelNotification = (payload) => dispatchEvent(new CustomEvent('invitedToChannelNotification', {detail: payload}));

    _onRemovedFromChannelNotification = (payload) => dispatchEvent(new CustomEvent('removedFromChannelNotification', {detail: payload}));

    _onNotificationSubscribed = (payload) => dispatchEvent(new CustomEvent('notificationSubscribed', {detail: payload}));

    _onConnectionStateChanged = (payload) => dispatchEvent(new CustomEvent('connectionStateChanged', {detail: payload}));
}

export default TwilioChatClient;
