import {EventFactory} from "./EventFactory.js";

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
        $.get("/user/users/get-ui-params", data => this.assignParams(data));
    }

    assignParams(data) {
        this.params = data;
        console.debug(this.params);
        this.fireEvent('ready', ['ok']);
    }

    set(key, value) {
        return new Promise(function(resolve, reject) {
            $.get({
                url: "/user/users/set-ui-params/" + key + "/" + value,
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