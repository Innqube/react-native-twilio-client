import {NativeEventEmitter, NativeModules, Platform} from 'react-native';

/**
 * @author Enrique Viard.
 *         Copyright © 2021 No Good Software Inc. All rights reserved.
 */

const {
  RNAudioManager,
  RNEventEmitterHelper,
} = NativeModules;

const NativeAppEventEmitter = new NativeEventEmitter(RNEventEmitterHelper);

const _eventHandlers = {
  audioRouteChanged: new Map(),
};

const AudioManager = {
  getAvailableAudioInputs() {
    return RNAudioManager.getAvailableAudioInputs();
  },
  switchAudioInput(input) {
    return RNAudioManager.switchAudioInput(input);
  },
  configure(mode) {
    Platform.OS === 'ios' ? RNAudioManager.configure(mode) : null;
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
    if (!_eventHandlers[type].has(handler)) {
      return
    }
    _eventHandlers[type].get(handler).remove()
    _eventHandlers[type].delete(handler)
  },
};

export default AudioManager;



