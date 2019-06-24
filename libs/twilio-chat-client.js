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
        if (typeof message !== 'string') {
            throw new Error('Message is required and must be a string');
        }
        return RNTwilioChatClient.sendMessage(message);
            // .then(({sid, type, paginator}) => new Paginator(sid, type, paginator));
    },
    joinChannel(uniqueName, friendlyName, type) {
        return RNTwilioChatClient.joinChannel(uniqueName, friendlyName, type);
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
    },
    getMessagesBefore(index, count): Promise<Message[]> {
        if (typeof count !== 'number') {
            throw new Error('Count is required and must be a number');
        }
        if (typeof index !== 'number') {
            throw new Error('Index is required and must be a number');
        }
        return RNTwilioChatClient.getMessagesBefore(index, count);
    },
    typing() {
        RNTwilioChatClient.typing();
    }
}

export default TwilioChatClient;
