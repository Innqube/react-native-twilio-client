import {
    NativeModules,
} from 'react-native';
import Channel from './domain/channel';
import Message from './domain/message';

const {
    RNTwilioChatClient,
} = NativeModules;


const TwilioChatClient = {

    async initialize(initialToken) {
        if (typeof initialToken !== 'string') {
            return {
                initialized: false,
                err: 'Invalid token, token must be a string'
            }
        };
        return await RNTwilioChatClient.createClient(initialToken, null);
    },

    sendMessage(message) {
        return RNTwilioChatClient.sendMessage(message);
            // .then(({sid, type, paginator}) => new Paginator(sid, type, paginator));
    },

    joinChannel(channelSid) {
        return RNTwilioChatClient.joinChannel(channelSid);
    },
    getPublicChannels(): Promise<Channel[]> {
        return RNTwilioChatClient.getPublicChannels();
    },
    getUserChannels(): Promise<Channel[]> {
        return RNTwilioChatClient.getUserChannels();
    },
    createChannel(uniqueName, friendlyName, type): Promise<Channel> {
        return RNTwilioChatClient.createChannel(uniqueName, friendlyName, type);
    },
    getLastMessages(count): Promise<Message[]> {
        if (typeof count !== 'number') {
            throw new Error('Count is required and must be a number');
        }
        return RNTwilioChatClient.getLastMessages(count);
    }

}

export default TwilioChatClient;
