import {NativeModules} from "react-native";

const {
    RNTwilioChatClient,
} = NativeModules;

const TwilioMessage = {
}

const ChatChannel = {

    constructor(props) {
        this.uniqueName = props.uniqueName;
        this.friendlyName = props.friendlyName;
        this.sid = props.sid;
        this.lastMessageIndex = props.lastMessageIndex;
        this.attributes = props.attributes;
    },

    getMessages(index, count) {
        return RNTwilioChatClient.getMessages(this.uniqueName, index, count);
    },

    getUnconsumedMessagesCount() {
        return RNTwilioChatClient.getUnconsumedMessagesCount(this.uniqueName);
    }

}

const TwilioChat = {

    create(token) {
        return RNTwilioChatClient
            .create(token)
            .then(() => new Promise((resolve, reject) => resolve(this)));
    },

    getChannel(channelSidOrUniqueName) {
        return RNTwilioChatClient
            .getChannel(channelSidOrUniqueName)
            .then(() => new Promise((resolve, reject) => resolve(
                    new ChatChannel(RNTwilioChatClient.getChannel(channelSidOrUniqueName))
                    )
                )
            );
    }

}

export default TwilioChat;