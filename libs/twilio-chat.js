import {NativeModules} from "react-native";

const {
    RNTwilioChatClient,
} = NativeModules;

const TwilioMessage = {
}

const ChatChannel = function (props) {

    this.uniqueName = props.uniqueName;
    this.friendlyName = props.friendlyName;
    this.sid = props.sid;
    this.lastMessageIndex = props.lastMessageIndex;
    this.attributes = props.attributes;

    this.getMessages = function(index, count) {
        return RNTwilioChatClient.getMessages(this.uniqueName, index, count);
    },

    this.getUnconsumedMessagesCount = function() {
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
            .then(channel => Promise.resolve(new ChatChannel(channel)));
    }

}

export default TwilioChat;
