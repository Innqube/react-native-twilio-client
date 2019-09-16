import {NativeModules} from "react-native";
import ChatChannel from './chat/chat-channel';
import EventEmitterHelper from './event-emitter-helper';

const {
    RNTwilioChatClient,
} = NativeModules;

const TwilioMessage = {}

const TwilioChat = {

    create(token) {
        return RNTwilioChatClient
            .create(token)
            .then(() => new Promise((resolve, reject) => resolve(this)));
    },

    getChannel(channelSidOrUniqueName) {
        return RNTwilioChatClient
            .getChannel(channelSidOrUniqueName)
            .then(channel => {
                const chatChannel = new ChatChannel(channel);
                EventEmitterHelper.addEventListener('messageAdded', message => this._messageFilter(message, chatChannel));
                return new Promise((resolve, reject) => resolve(chatChannel));
            });
    },

    _messageFilter(message, channel) {
        if (message.channel.sid === channel.sid) {
            if (channel.onNewMessage) {
                channel.onNewMessage(message);
            }
        }
    }

}

export default TwilioChat;
