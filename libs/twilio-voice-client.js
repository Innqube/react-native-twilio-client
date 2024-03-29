import {NativeEventEmitter, NativeModules, Platform} from 'react-native'

const ANDROID = 'android'
const IOS = 'ios'

const {RNTwilioVoice: TwilioVoice, RNEventEmitterHelper} = NativeModules;

const NativeAppEventEmitter = new NativeEventEmitter(RNEventEmitterHelper);

// Supported events
const _eventHandlers = {
    deviceReady: new Map(),
    deviceNotReady: new Map(),
    deviceDidReceiveIncoming: new Map(),
    connectionDidConnect: new Map(),
    connectionIsReconnecting: new Map(),
    connectionDidReconnect: new Map(),
    connectionDidDisconnect: new Map(),
    callStateRinging: new Map(),
    callInviteCancelled: new Map(),
    callRejected: new Map(),
    voiceCallAccepted: new Map(),
    voiceCallRejected: new Map(),
    voiceGoOffline: new Map(),
    // Events for TwilioVideo
    voipRemoteNotificationsRegistered: new Map()
}

const TwilioVoiceClient = {

    connect(params = {}, iceServers = [], tokenCallback) {
        tokenCallback()
          .then(token => {
              return TwilioVoice.connect(params, iceServers, token)
          })
    },
    disconnect(uuid) {
        if (Platform.OS === IOS) {
            TwilioVoice.disconnect(uuid);
        } else {
            TwilioVoice.disconnect();
        }
    },
    accept() {
        if (Platform.OS === IOS) {
            return
        }
        TwilioVoice.accept()
    },
    reject() {
        if (Platform.OS === IOS) {
            return
        }
        TwilioVoice.reject()
    },
    ignore() {
        if (Platform.OS === IOS) {
            return
        }
        TwilioVoice.ignore()
    },
    setMuted: TwilioVoice.setMuted,
    setSpeakerPhone: TwilioVoice.setSpeakerPhone,
    sendDigits: TwilioVoice.sendDigits,
    hold: TwilioVoice.hold,
    requestPermissions(senderId) {
        if (Platform.OS === ANDROID) {
            TwilioVoice.requestPermissions(senderId)
        }
    },
    getActiveCall: Platform.OS === ANDROID ? TwilioVoice.getActiveCall : null,
    getCallInvite: TwilioVoice.getCallInvite,
    configureCallKit(params = {}) {
        if (Platform.OS === IOS) {
            TwilioVoice.configureCallKit(params)
        }
    },
    addEventListener(type, handler) {
        if (!_eventHandlers.hasOwnProperty(type)) {
            throw new Error('Event handler not found: ' + type)
        }
        if (_eventHandlers[type])
            if (_eventHandlers[type].has(handler)) {
                return
            }
        _eventHandlers[type].set(handler, NativeAppEventEmitter.addListener(type, rtn => { handler(rtn) }))
    },
    removeEventListener(type, handler) {
        NativeAppEventEmitter.removeAllListeners(type)
        if (!_eventHandlers[type].has(handler)) {
            return
        }
        _eventHandlers[type].get(handler).remove()
        _eventHandlers[type].delete(handler)
    },
    getDeviceToken() {
        if (Platform.OS === IOS) {
            return TwilioVoice.getDeviceToken();
        }
    },
    setEdge(edge) {
        TwilioVoice.setEdge(edge)
    }
}

export default TwilioVoiceClient
