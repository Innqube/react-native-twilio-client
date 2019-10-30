import {NativeEventEmitter, NativeModules, Platform,} from 'react-native'

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

    initialize(tokenCallback) {
        return new Promise(((resolve, reject) => {
            tokenCallback()
                .then(token => {
                    RNTwilioVoiceClient
                        .init(token)
                        .then(result => resolve(result))
                        .catch(error => reject(error));
                })
                .catch(error => reject(error));
        }));
    },
    connect(params = {}, tokenCallback) {
        tokenCallback()
            .then(token => {
                return RNTwilioVoiceClient.connect(params, token)
            })
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
    unregister(tokenCallback) {
        if (Platform.OS === IOS) {
            return new Promise(((resolve, reject) => {
                tokenCallback()
                    .then(token => {
                        RNTwilioVoiceClient
                            .unregister(token)
                            .then(result => resolve(result))
                            .catch(error => reject(error));
                    })
                    .catch(error => reject(error));
            }));
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
