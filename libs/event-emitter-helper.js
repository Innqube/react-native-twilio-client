import {
    NativeModules,
    NativeEventEmitter,
} from 'react-native'

const {RNEventEmitterHelper} = NativeModules;

const NativeAppEventEmitter = new NativeEventEmitter(RNEventEmitterHelper);

const _eventHandlers = {
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
    // Chat events
    synchronizationStatusUpdated: new Map(),
    messageAdded: new Map(),
    typingStartedOnChannel: new Map(),
    typingEndedOnChannel: new Map(),
    tokenAboutToExpire: new Map(),
    tokenExpired: new Map()
}

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
}

export default EventEmitterHelper
