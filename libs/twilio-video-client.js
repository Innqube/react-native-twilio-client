import {NativeEventEmitter, NativeModules, Platform} from 'react-native'

const ANDROID = 'android'
const IOS = 'ios'

const {RNTwilioVideo: TwilioVideo, RNEventEmitterHelper} = NativeModules;

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
    // Events for TwilioVideo
    voipRemoteNotificationsRegistered: new Map()
}

const TwilioVoiceClient = {

    // initialize(tokenCallback) {
    //     return new Promise(((resolve, reject) => {
    //         tokenCallback()
    //             .then(token => {
    //                 TwilioVideo
    //                     .initWithAccessToken(token)
    //                     .then(result => resolve(result))
    //                     .catch(error => reject(error));
    //             })
    //             .catch(error => reject(error));
    //     }));
    // },
    // connect(params = {}, tokenCallback) {
    //     tokenCallback()
    //         .then(token => {
    //             return TwilioVideo.connect(params, token)
    //         })
    // },
    // disconnect(uuid) {
    //     if (Platform.OS === IOS) {
    //         TwilioVideo.disconnect(uuid);
    //     } else {
    //         TwilioVideo.disconnect();
    //     }
    // },
    accept() {
        if (Platform.OS === IOS) {
            return
        }
        TwilioVideo.accept()
    },
    reject() {
        if (Platform.OS === IOS) {
            return
        }
        TwilioVideo.reject()
    },
    // ignore() {
    //     if (Platform.OS === IOS) {
    //         return
    //     }
    //     TwilioVideo.ignore()
    // },
    // setMuted: TwilioVideo.setMuted,
    // setSpeakerPhone: TwilioVideo.setSpeakerPhone,
    // sendDigits: TwilioVideo.sendDigits,
    // hold: TwilioVideo.hold,
    // requestPermissions(senderId) {
    //     if (Platform.OS === ANDROID) {
    //         TwilioVideo.requestPermissions(senderId)
    //     }
    // },
    // getActiveCall: TwilioVideo.getActiveCall,
    // getCallInvite: TwilioVideo.getCallInvite,
    // configureCallKit(params = {}) {
    //     if (Platform.OS === IOS) {
    //         TwilioVideo.configureCallKit(params)
    //     }
    // },
    // unregister(tokenCallback) {
    //     return new Promise(((resolve, reject) => {
    //         tokenCallback()
    //             .then(token => {
    //                 TwilioVideo
    //                     .unregister(token)
    //                     .then(result => resolve(result))
    //                     .catch(error => reject(error));
    //             })
    //             .catch(error => reject(error));
    //     }));
    // },
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
        if (!_eventHandlers[type].has(handler)) {
            return
        }
        _eventHandlers[type].get(handler).remove()
        _eventHandlers[type].delete(handler)
    },
    // getDeviceToken() {
    //     if (Platform.OS === IOS) {
    //         return TwilioVideo.getDeviceToken();
    //     }
    // }
}

export default TwilioVoiceClient
