import {NativeModules,} from 'react-native'

const {RNLogHelper} = NativeModules;

const LogHelper = {
    log(message) {
        RNLogHelper.sendMessage(message)
    }
}

export default LogHelper
