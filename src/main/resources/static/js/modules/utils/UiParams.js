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

    getCsrfConfig() {
        const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        const headerName = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
        if (token && headerName) {
            return {
                [headerName]: token,
                'X-Requested-With': 'XMLHttpRequest'
            };
        }
        return {
            'X-Requested-With': 'XMLHttpRequest'
        };
    }

    getParamsFromServer() {
        $.get("/ws-secure/ui/ui-data", data => this.assignParams(data?.preferences || {}));
    }

    assignParams(data) {
        this.params = data;
        console.debug("debug - " + this.params);
        this.fireEvent("ready", ['ok']);
    }

    set(key, value) {
        const headers = this.getCsrfConfig();
        return new Promise(function(resolve, reject) {
            $.ajax({
                url: "/ws-secure/ui/ui-data/preferences/" + encodeURIComponent(key) + "/" + encodeURIComponent(value),
                type: 'PUT',
                headers,
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