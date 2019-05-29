import {NativeModules,} from 'react-native'

const {RNLogHelper} = NativeModules;

const LogHelper = {
    log(message) {
        RNLogHelper.log(message)
    }
}

export default LogHelper
