export class EventFactory {

    constructor() {
        this.events = {};
    }

    addEventListener(name, handler) {
        if (this.events.hasOwnProperty(name))
            this.events[name].push(handler);
        else
            this.events[name] = [handler];
    };

    removeEventListener(name, handler) {
        if (!this.events.hasOwnProperty(name))
            return;

        let index = this.events[name].indexOf(handler);
        if (index !== -1)
            this.events[name].splice(index, 1);
    };

    fireEvent(name, args) {
        if (!this.events.hasOwnProperty(name))
            return;

        if (!args || !args.length)
            args = [];

        let evs = this.events[name], l = evs.length;
        for (let i = 0; i < l; i++) {
            evs[i].apply(null, args);
        }
    };

}   