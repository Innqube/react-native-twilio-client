import {
    NativeModules,
} from 'react-native';
import ChatChannel from './chat-channel';
import EventEmitterHelper from '../event-emitter-helper';

const {
    RNTwilioChatClient,
} = NativeModules;

const TwilioChatClient = function (props) {

    this.initialize = async function(initialToken) {
        if (typeof initialToken !== 'string') {
            return {
                initialized: false,
                err: 'Invalid token, token must be a string'
            }
        };
        return await RNTwilioChatClient.createClient(initialToken, null);
    };

    this.update = function (updatedToken) {
        if (typeof updatedToken !== 'string') {
            return {
                updated: false,
                err: 'Invalid token, token must be a string'
            }
        }
        return RNTwilioChatClient.updateClient(updatedToken);
    };

    this.sendMessage = function (message) {
        if (typeof message !== 'string') {
            throw new Error('Message is required and must be a string');
        }
        return RNTwilioChatClient.sendMessage(message);
        // .then(({sid, type, paginator}) => new Paginator(sid, type, paginator));
    };

    this.getChannel = function (channelSidOrUniqueName) {
        return RNTwilioChatClient
            .getChannel(channelSidOrUniqueName)
            .then(channel => {
                const chatChannel = new ChatChannel(channel);
                EventEmitterHelper.addEventListener('messageAdded', message => this._messageFilter(message, chatChannel));
                return new Promise((resolve, reject) => resolve(chatChannel));
            });
    };

    this.createChannel = function (uniqueName, friendlyName, type) {
        return RNTwilioChatClient.createChannel(uniqueName, friendlyName, type);
    };

    this.joinChannel = function (uniqueName, friendlyName, type) {
        return RNTwilioChatClient.joinChannel(uniqueName, friendlyName, type);
    };

    this.getPublicChannels = function () {
        return RNTwilioChatClient.getPublicChannels();
    };

    this.getUserChannels = function () {
        return RNTwilioChatClient.getUserChannels();
    };
};

export default TwilioChatClient;
