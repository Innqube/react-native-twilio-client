import {
    NativeModules,
    NativeEventEmitter,
} from 'react-native'

const {RNEventEmitterHelper} = NativeModules;

const NativeAppEventEmitter = new NativeEventEmitter(RNEventEmitterHelper);

const _eventHandlers = {
    // Vri/Opi events
    deviceReady: new Map(),
    deviceNotReady: new Map(),
    deviceDidReceiveIncoming: new Map(),
    connectionDidConnect: new Map(),
    connectionDidDisconnect: new Map(),
    performAnswerVoiceCall: new Map(),
    performAnswerVideoCall: new Map(),
    performEndVideoCall: new Map(),
    requestTransactionError: new Map(),
    callRejected: new Map(),
    voipRemoteNotificationsRegistered: new Map(),

    // ChatClient events
    synchronizationStatusUpdated: new Map(),
    connectionStateChanged: new Map(),
    messageAdded: new Map(),

    tokenAboutToExpire: new Map(),
    tokenExpired: new Map(),
    channelJoined: new Map(),
    channelInvited: new Map(),
    channelAdded: new Map(),
    channelUpdate: new Map(),
    channelDeleted: new Map(),
    userUpdated: new Map(),
    userSubscribed: new Map(),
    userUnsubscribed: new Map(),
    addedToChannelNotification: new Map(),
    invitedToChannelNotification: new Map(),
    removedFromChannelNotification: new Map(),
    notificationSubscribed: new Map(),

    // ChatChannel events
    typingStartedOnChannel: new Map(),
    typingEndedOnChannel: new Map(),
};

//             sendEvent(getReactApplicationContext() ,"channelSynchronizationChange", channel);
//             sendEvent(getReactApplicationContext() ,"error", {code: string, message: string, status: string}});
//             sendEvent(getReactApplicationContext() ,"synchronizationStatus", string);
//             sendEvent(getReactApplicationContext() ,"error", {code: string, message: string, status: string});

const EventEmitterHelper = {
    addEventListener(type, handler) {
        if (_eventHandlers[type].has(handler)) {
            return
        }
        _eventHandlers[type].set(handler, NativeAppEventEmitter.addListener(type, rtn => {
            handler(rtn)
        }))
    },
    removeEventListener(type, handler) {
        if (!_eventHandlers[type].has(handler)) {
            return
        }
        _eventHandlers[type].get(handler).remove()
        _eventHandlers[type].delete(handler)
    }
};

export default EventEmitterHelper
