/**
 * @author Enrique Viard.
 *         Copyright © 2021 No Good Software Inc. All rights reserved.
 */
import {NativeModules} from 'react-native';

const {RNLocalizedStrings} = NativeModules;

const LocalizedStrings = {
  configure(language, translations) {
    RNLocalizedStrings.configure(language, translations);
  },
};

export default LocalizedStrings;
