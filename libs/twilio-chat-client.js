import {
    NativeModules,
} from 'react-native';

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
        const response = await RNTwilioChatClient.createClient(initialToken, null);

        console.log('RESPONSE: ' + response);
        return response;
    },

    sendMessage(message) {
        return RNTwilioChatClient.sendMessage(message, null)
            .then(({sid, type, paginator}) => new Paginator(sid, type, paginator));
    },

    joinChannel(channelSid) {
        return RNTwilioChatClient.joinChannel(channelSid);
    },
    getPublicChannels() {
        return RNTwilioChatClient.getPublicChannels();
    },
    getUserChannels() {
        return RNTwilioChatClient.getUserChannels();
    },
    createChannel(uniqueName, friendlyName, type) {
        return RNTwilioChatClient.createChannel(uniqueName, friendlyName, type);
    }

    // getPublicChannels() {
    //     return TwilioChatChannels.getPublicChannels()
    //         .then(({sid, type, paginator}) => new Paginator(sid, type, paginator));
    // }
}

export default TwilioChatClient;
