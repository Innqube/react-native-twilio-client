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
    // Events for TwilioVideo
    voipRemoteNotificationsRegistered: new Map()
}

const TwilioVoiceClient = {

    initialize(tokenCallback) {
        return new Promise(((resolve, reject) => {
            tokenCallback()
                .then(token => {
                    TwilioVoice
                        .initWithAccessToken(token)
                        .then(result => resolve(result))
                        .catch(error => reject(error));
                })
                .catch(error => reject(error));
        }));
    },
    connect(params = {}, tokenCallback) {
        tokenCallback()
            .then(token => {
                return TwilioVoice.connect(params, token)
            })
    },
    disconnect(uuid) {
        TwilioVoice.disconnect(uuid)
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
    setMuted(isMuted) {
        TwilioVoice.setMuted(isMuted)
    },
    setSpeakerPhone(value) {
        TwilioVoice.setSpeakerPhone(value)
    },
    sendDigits(digits) {
        TwilioVoice.sendDigits(digits)
    },
    requestPermissions(senderId) {
        if (Platform.OS === ANDROID) {
            TwilioVoice.requestPermissions(senderId)
        }
    },
    unregister(tokenCallback) {
        if (Platform.OS === IOS) {
            return new Promise(((resolve, reject) => {
                tokenCallback()
                    .then(token => {
                        TwilioVoice
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
        TwilioVoice.sendMessage(message)
    },
    deviceReadyForCalls() {
        TwilioVoice.deviceReadyForCalls()
    },
    getDeviceToken() {
        return TwilioVoice.getDeviceToken();
    }
}

export default TwilioVoiceClient
