import {EventFactory} from "./EventFactory.js?version=@version@";

export class UiParams extends EventFactory {

    constructor() {
        super();
        this.params;
        this.getParamsFromServer();
        this.initListeners();
    }

    initListeners() {
    }

    getParamsFromServer() {
        $.get("/ws-secure/ui/preferences", data => this.assignParams(data));
    }

    assignParams(data) {
        this.params = data;
        console.debug("debug - " + this.params);
        this.fireEvent("ready", ['ok']);
    }

    set(key, value) {
        return new Promise(function(resolve, reject) {
            $.get({
                url: "/ws-secure/ui/preferences/" + key + "/" + value,
                success: function(data) {
                    resolve(data)
                },
                error: function(err) {
                    reject(err)
                }
            });
        });
    }

    get(name) {
        return this.params[name];
    }

}