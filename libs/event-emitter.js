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
        ];

        return {
            name,
            handler
        }
    };

    removeListener = listener => {
        if (this._events[listener.name]) {
            this._events[listener.name] = this._events[listener.name].filter(handler => handler !== listener.handler);
        }
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

export default EventEmitter;
