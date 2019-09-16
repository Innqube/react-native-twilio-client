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
    update(updatedToken) {
        if (typeof updatedToken !== 'string') {
            return {
                updated: false,
                err: 'Invalid token, token must be a string'
            }
        };
        return RNTwilioChatClient.updateClient(updatedToken);
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
    getChannelMembers(): Promise<any[]> {
        return RNTwilioChatClient.getChannelMembers();
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
    getMessagesAfter(index, count): Promise<Message[]> {
        if (typeof count !== 'number') {
            throw new Error('Count is required and must be a number');
        }
        if (typeof index !== 'number') {
            throw new Error('Index is required and must be a number');
        }
        return RNTwilioChatClient.getMessagesAfter(index, count);
    },
    typing() {
        RNTwilioChatClient.typing();
    },
    getUnreadMessagesCount() {
        return RNTwilioChatClient.getUnreadMessagesCount();
    },
    getLastConsumedMessageIndex() {
        return RNTwilioChatClient.getLastConsumedMessageIndex();
    },
    getMessagesCount() {
        return RNTwilioChatClient.getMessagesCount();
    },
    setNoMessagesConsumed() {
        return RNTwilioChatClient.setNoMessagesConsumed();
    },
    setAllMessagesConsumed() {
        return RNTwilioChatClient.setAllMessagesConsumed();
    },
    setLastConsumedMessage(index) {
        return RNTwilioChatClient.setLastConsumedMessage(index);
    },
    advanceLastConsumedMessage(index) {
        return RNTwilioChatClient.advanceLastConsumedMessage(index);
    }
}

export default TwilioChatClient;
