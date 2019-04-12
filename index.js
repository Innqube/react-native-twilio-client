import {
    NativeModules,
    NativeEventEmitter,
    Platform,
} from 'react-native'

const ANDROID = 'android'
const IOS = 'ios'

const TwilioClient = NativeModules.RNTwilioClient
const EventEmitterHelper = NativeModules.EventEmitterHelper

const NativeAppEventEmitter = new NativeEventEmitter(EventEmitterHelper)

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

const Twilio = {
    // initialize TwilioVoice with access token
    async initWithToken(token) {
        if (typeof token !== 'string') {
            return {
                initialized: false,
                err: 'Invalid token, token must be a string'
            }
        };

        const result = await TwilioClient.initWithAccessToken(token)
        if (Platform.OS === IOS) {
            return {
                initialized: true,
            }
        }
        return result
    },
    connect(params = {}) {
        return TwilioClient.connect(params)
    },
    disconnect(uuid) {
        TwilioClient.disconnect(uuid)
    },
    accept() {
        if (Platform.OS === IOS) {
            return
        }
        TwilioClient.accept()
    },
    reject() {
        if (Platform.OS === IOS) {
            return
        }
        TwilioClient.reject()
    },
    ignore() {
        if (Platform.OS === IOS) {
            return
        }
        TwilioClient.ignore()
    },
    setMuted(isMuted) {
        TwilioClient.setMuted(isMuted)
    },
    setSpeakerPhone(value) {
        TwilioClient.setSpeakerPhone(value)
    },
    sendDigits(digits) {
        TwilioClient.sendDigits(digits)
    },
    requestPermissions(senderId) {
        if (Platform.OS === ANDROID) {
            TwilioClient.requestPermissions(senderId)
        }
    },
    unregister() {
        if (Platform.OS === IOS) {
            TwilioClient.unregister()
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
        TwilioClient.sendMessage(message)
    },
    deviceReadyForCalls() {
        TwilioClient.deviceReadyForCalls()
    },
    getDeviceToken() {
        return TwilioClient.getDeviceToken();
    }
}

export default Twilio
