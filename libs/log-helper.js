import {NativeModules,} from 'react-native'

const {RNLogHelper} = NativeModules;

const LogHelper = {
    log(message) {
        if (typeof message !== 'string') {
            throw new Error('Message is required and must be a string');
        }
        RNLogHelper.log(message);
    }
};

export default LogHelper
