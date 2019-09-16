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

const TwilioChatClient = {

    create(token) {
        return RNTwilioChat
            .create(token)
            .then(() => new Promise((resolve, reject) => resolve(this)));
    },

    getChannel(channelSidOrUniqueName) {
        return RNTwilioChat.getChannel(channelSidOrUniqueName);
    }

}

export default TwilioChat;