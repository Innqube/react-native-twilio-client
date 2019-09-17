import {NativeModules,} from 'react-native';
import TwilioChatChannel from './twilio-chat-channel';
import EventEmitterHelper from '../event-emitter-helper';
import SynchronizationStatus from '../domain/synchronization-status';

const {
    RNTwilioChatClient,
} = NativeModules;

class TwilioChatClient {

    constructor(props) {
        this.tokenCallback = props.tokenCallback;
    }

    create = () => new Promise(((resolve, reject) => {
        this.tokenCallback()
            .then(token => {
                RNTwilioChatClient
                    .createClient(token, null)
                    .then(client => EventEmitterHelper.addEventListener(
                        'synchronizationStatusUpdated',
                        status => this._synchronizationListener(status, resolve, reject)
                        )
                    )
            });
    }));

    shutdown = () => {
        this._removeAllListeners();
        RNTwilioChatClient.shutdown();
    };

    register = (token) => RNTwilioChatClient.register(token);

    unRegister = (token) => RNTwilioChatClient.unRegister(token);

    createChannel = (uniqueName, friendlyName, type) =>  RNTwilioChatClient.createChannel(uniqueName, friendlyName, type);

    getPublicChannels = () => RNTwilioChatClient.getPublicChannels();

    getUserChannels = () => RNTwilioChatClient.getUserChannels();

    getChannel = (channelSidOrUniqueName) => {
        return RNTwilioChatClient
            .getChannel(channelSidOrUniqueName)
            .then(channel => {
                const chatChannel = new TwilioChatChannel(channel);
                EventEmitterHelper.addEventListener('messageAdded', message => this._messageFilter(message, chatChannel));
                return Promise.resolve(chatChannel);
            });
    };

    _synchronizationListener = (status, resolve, reject) => {
        switch (status) {
            case SynchronizationStatus.ClientSynchronizationStatusCompleted:
                this._initEventListeners();
                dispatchEvent(new CustomEvent('synchronizationStatusUpdated', SynchronizationStatus.ClientSynchronizationStatusCompleted));
                resolve(this);
                break;
            case SynchronizationStatus.ClientSynchronizationStatusStarted:
                dispatchEvent(new CustomEvent('synchronizationStatusUpdated', SynchronizationStatus.ClientSynchronizationStatusStarted));
                break;
            case SynchronizationStatus.ClientSynchronizationStatusChannelsListCompleted:
                dispatchEvent(new CustomEvent('synchronizationStatusUpdated', SynchronizationStatus.ClientSynchronizationStatusChannelsListCompleted));
                break;
            case SynchronizationStatus.ClientSynchronizationStatusFailed:
                dispatchEvent(new CustomEvent('synchronizationStatusUpdated', SynchronizationStatus.ClientSynchronizationStatusFailed));
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
        EventEmitterHelper.addEventListener('connectionStateChange', this._onConnectionStateChange);
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
        EventEmitterHelper.removeEventListener('connectionStateChange', this._onConnectionStateChange);
    };

    _onTokenAboutToExpire = async () => RNTwilioChatClient.updateClient(await this.tokenCallback());

    _onChannelJoined = (evt) => dispatchEvent(new CustomEvent('channelJoined', evt));

    _onChannelInvited = (evt) => dispatchEvent(new CustomEvent('channelInvited', evt));

    _onChannelAdded = (evt) => dispatchEvent(new CustomEvent('channelAdded', evt));

    _onChannelUpdate = (evt) => dispatchEvent(new CustomEvent('channelUpdate', evt));

    _onChannelDeleted = (evt) => dispatchEvent(new CustomEvent('channelDeleted', evt));

    _onUserUpdated = (evt) => dispatchEvent(new CustomEvent('userUpdated', evt));

    _onUserSubscribed = (evt) => dispatchEvent(new CustomEvent('userSubscribed', evt));

    _onUserUnsubscribed = (evt) => dispatchEvent(new CustomEvent('userUnsubscribed', evt));

    _onAddedToChannelNotification = (evt) => dispatchEvent(new CustomEvent('addedToChannelNotification', evt));

    _onInvitedToChannelNotification = (evt) => dispatchEvent(new CustomEvent('invitedToChannelNotification', evt));

    _onRemovedFromChannelNotification = (evt) => dispatchEvent(new CustomEvent('removedFromChannelNotification', evt));

    _onNotificationSubscribed = (evt) => dispatchEvent(new CustomEvent('notificationSubscribed', evt));

    _onConnectionStateChange = (evt) => dispatchEvent(new CustomEvent('connectionStateChange', evt));
};

export default TwilioChatClient;
