import {NativeAppEventEmitter, NativeModules} from 'react-native';
import EventEmitter from './event-emitter';

/**
 * @author Enrique Viard.
 *         Copyright © 2019 InnQube. All rights reserved.
 */

const {
    RNNotificationsModule,
} = NativeModules;

class Notifications {

    _eventEmitter;

    constructor(props) {
        this._eventEmitter = new EventEmitter();
        this._initEventListeners();
    }

    _initEventListeners = () => {
        NativeAppEventEmitter.addListener('notificationReceived', this.onNotificationReceived);
    };

    onNotificationReceived = (payload) => this._eventEmitter.emit('notificationReceived', payload);

    // Events delegation
    addListener = (name, handler) => this._eventEmitter.addListener(name, handler);
    removeAllListeners = (name) => this._eventEmitter.removeAllListeners(name);

}

export default Notifications;
