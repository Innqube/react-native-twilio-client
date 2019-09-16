const {
    RNTwilioChatClient,
} = NativeModules;

const TwilioMessage = {
}

const ChatChannel = {

    getMessages(index, count) {
        return RNTwilioChatClient.getMessages(this.uniqueName, index, count);
    }

}

const TwilioChat = {

    create(token) {
        return RNTwilioChatClient
            .create(token)
            .then(() => new Promise((resolve, reject) => resolve(this)));
    },

    getChannel(channelSidOrUniqueName) {
        return RNTwilioChatClient.getChannel(channelSidOrUniqueName);
    }

}

export default TwilioChat;