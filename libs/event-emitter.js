class EventEmitter {

    _events = {};

    constructor() {
    }

    addListener = (name, handler) => {

        // No handlers registered yet
        if (!this._events[name]) {
            this._events[name] = []
        }

        // Expand the array with this handler
        this._events[name] = [
            ...this._events[name],
            handler
        ]

    };

    removeAllListeners = (name) => {
        delete this._events[name];
    };

    emit = (name, payload) => {
        if (this._events[name]) {
            this._events[name].forEach(handler => handler(payload));
        }
    }

}
