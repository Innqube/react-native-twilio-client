import {NativeAppEventEmitter, NativeModules} from 'react-native';

/**
 * @author Enrique Viard.
 *         Copyright © 2021 No Good Software Inc. All rights reserved.
 */

const {
  RNAudioManagerModule,
} = NativeModules;

const AudioManager = {
  getAvailableAudioInputs() {
    return RNAudioManagerModule.getAvailableAudioInputs();
  },
  switchAudioInput(input) {
    return RNAudioManagerModule.switchAudioInput(input);
  }
};

export default AudioManager;



