import {NativeEventEmitter, NativeModules,} from 'react-native'

const {RNEventEmitterHelper} = NativeModules;

const NativeAppEventEmitter = new NativeEventEmitter(RNEventEmitterHelper);

const _eventHandlers = {
    // Vri/Opi events
    deviceReady: new Map(),
    deviceNotReady: new Map(),
    deviceDidReceiveIncoming: new Map(),
    connectionDidConnect: new Map(),
    connectionDidDisconnect: new Map(),
    callDidReconnect: new Map(),
    isReconnectingWithError: new Map(),
    performAnswerVoiceCall: new Map(),
    performAnswerVideoCall: new Map(),
    performEndVideoCall: new Map(),
    requestTransactionError: new Map(),
    callRejected: new Map(),
    voipRemoteNotificationsRegistered: new Map(),

    // Video events
    videoConnectionDidDisconnect: new Map(),
    videoDeviceDidReceiveIncoming: new Map(),
    videoCallInviteCancelled: new Map(),

    // ChatClient events
    connectionStateUpdated: new Map(),
    tokenExpired: new Map(),
    tokenAboutToExpire: new Map(),
    synchronizationStatusUpdated: new Map(),
    channelAdded: new Map(),
    channelUpdated: new Map(),
    channelSynchronizationUpdated: new Map(),
    channelDeleted: new Map(),
    error: new Map(),
    newMessageNotification: new Map(),
    addedToChannelNotification: new Map(),
    invitedToChannelNotification: new Map(),
    removedFromChannelNotification: new Map(),
    userUpdated: new Map(),
    userSubscribed: new Map(),
    userUnsubscribed: new Map(),
    channelSynchronizationStatusUpdated: new Map(),
    memberAdded: new Map(),
    memberUpdated: new Map(),
    memberDeleted: new Map(),
    messageAdded: new Map(),
    messageUpdated: new Map(),
    messageDeleted: new Map(),
    typingStarted: new Map(),
    typingEnded: new Map()
};

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
