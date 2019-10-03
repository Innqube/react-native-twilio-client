import {
    NativeModules,
    NativeEventEmitter,
    Platform,
} from 'react-native'

const ANDROID = 'android'
const IOS = 'ios'

const {RNTwilioVoiceClient, RNEventEmitterHelper} = NativeModules;

const NativeAppEventEmitter = new NativeEventEmitter(RNEventEmitterHelper);

// Supported events
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
    // Events for TwilioVideo
    voipRemoteNotificationsRegistered: new Map()
}

const TwilioVoiceClient = {
    // initialize TwilioVoice with access token
    async initWithToken(token) {
        if (typeof token !== 'string') {
            return {
                initialized: false,
                err: 'Invalid token, token must be a string'
            }
        };

        const result = await RNTwilioVoiceClient.initWithAccessToken(token)
        if (Platform.OS === IOS) {
            return {
                initialized: true,
            }
        }
        return result
    },
    connect(params = {}) {
        return RNTwilioVoiceClient.connect(params)
    },
    disconnect(uuid) {
        RNTwilioVoiceClient.disconnect(uuid)
    },
    accept() {
        if (Platform.OS === IOS) {
            return
        }
        RNTwilioVoiceClient.accept()
    },
    reject() {
        if (Platform.OS === IOS) {
            return
        }
        RNTwilioVoiceClient.reject()
    },
    ignore() {
        if (Platform.OS === IOS) {
            return
        }
        RNTwilioVoiceClient.ignore()
    },
    setMuted(isMuted) {
        RNTwilioVoiceClient.setMuted(isMuted)
    },
    setSpeakerPhone(value) {
        RNTwilioVoiceClient.setSpeakerPhone(value)
    },
    sendDigits(digits) {
        RNTwilioVoiceClient.sendDigits(digits)
    },
    requestPermissions(senderId) {
        if (Platform.OS === ANDROID) {
            RNTwilioVoiceClient.requestPermissions(senderId)
        }
    },
    unregister() {
        if (Platform.OS === IOS) {
            RNTwilioVoiceClient.unregister()
        }
    },
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
    },
    log(message) {
        RNTwilioVoiceClient.sendMessage(message)
    },
    deviceReadyForCalls() {
        RNTwilioVoiceClient.deviceReadyForCalls()
    },
    getDeviceToken() {
        return RNTwilioVoiceClient.getDeviceToken();
    }
}

export default TwilioVoiceClient
