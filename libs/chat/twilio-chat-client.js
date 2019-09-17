import {
    NativeModules,
} from 'react-native';
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
                    .then(client => {
                        EventEmitterHelper.addEventListener('synchronizationStatusUpdated', (status) => {
                            switch (status) {
                                case SynchronizationStatus.ClientSynchronizationStatusCompleted:
                                    EventEmitterHelper.addEventListener('tokenAboutToExpire', async () => {
                                        RNTwilioChatClient.updateClient(await this.tokenCallback());
                                    });
                                    dispatchEvent(new CustomEvent('synchronizationStatusUpdated', SynchronizationStatus.ClientSynchronizationStatusCompleted));
                                    resolve(this);
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
                        });
                    })
            });
    }));

    update = () => ;
    shutdown = () => RNTwilioChatClient.shutdown();
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
                EventEmitterHelper.addEventListener('messageAdded', message => _messageFilter(message, chatChannel));
                return Promise.resolve(chatChannel);
            });
    };

};

export default TwilioChatClient;