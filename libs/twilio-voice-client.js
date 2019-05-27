import {
    NativeModules,
    NativeEventEmitter,
    Platform,
} from 'react-native'

const ANDROID = 'android'
const IOS = 'ios'

const {RNTwilioClient, RNEventEmitterHelper} = NativeModules;

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

        const result = await RNTwilioClient.initWithAccessToken(token)
        if (Platform.OS === IOS) {
            return {
                initialized: true,
            }
        }
        return result
    },
    connect(params = {}) {
        return RNTwilioClient.connect(params)
    },
    disconnect(uuid) {
        RNTwilioClient.disconnect(uuid)
    },
    accept() {
        if (Platform.OS === IOS) {
            return
        }
        RNTwilioClient.accept()
    },
    reject() {
        if (Platform.OS === IOS) {
            return
        }
        RNTwilioClient.reject()
    },
    ignore() {
        if (Platform.OS === IOS) {
            return
        }
        RNTwilioClient.ignore()
    },
    setMuted(isMuted) {
        RNTwilioClient.setMuted(isMuted)
    },
    setSpeakerPhone(value) {
        RNTwilioClient.setSpeakerPhone(value)
    },
    sendDigits(digits) {
        RNTwilioClient.sendDigits(digits)
    },
    requestPermissions(senderId) {
        if (Platform.OS === ANDROID) {
            RNTwilioClient.requestPermissions(senderId)
        }
    },
    unregister() {
        if (Platform.OS === IOS) {
            RNTwilioClient.unregister()
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
        RNTwilioClient.sendMessage(message)
    },
    deviceReadyForCalls() {
        RNTwilioClient.deviceReadyForCalls()
    },
    getDeviceToken() {
        return RNTwilioClient.getDeviceToken();
    }
}

export default TwilioVoiceClient
