export class Nexu {

    nexuUrl;
    nexuVersion;

    constructor(nexuUrl, nexuVersion) {
        this.nexuUrl = nexuUrl;
        this.nexuVersion = nexuVersion;
        if (!String.prototype.startsWith) {
            String.prototype.startsWith = function(searchString, position){
                return this.substr(position || 0, searchString.length) === searchString;
            };
        }

        this.init();
    }

    init() {
        console.log("Start checking NexU");
        $.ajax({
            type: "GET",
            url: this.nexuUrl + "/nexu-info",
            crossDomain: true,
            dataType: "jsonp",
            context : this,
            success: data => this.checkNexu(data)
        }).fail(function (error) {
            console.warn("NexU not detected or not started ! " + JSON.stringify(error));
            $("#warning-text").html("NexU not detected or not started ! ");
            $("#nexu_missing_alert").show();
        });

    }

    checkNexu(data) {
        if(data.version.startsWith(this.nexuVersion)) {
            console.log("Loading script...");
            this.loadScript();
            $("#nexu_ready_alert").show();
            $("#submit-button").prop('disabled', false);
        } else {
            // need update
            $("#nexu_version_alert").show();
            console.log("Bad NexU version " + data.version + " instead of " + this.nexuVersion);

        }
    }

    loadScript() {
        var xhrObj = new XMLHttpRequest();
        xhrObj.open('GET', this.nexuUrl + "/nexu.js");
        xhrObj.send(null);
        var se = document.createElement('script');
        se.type = "text/javascript";
        se.text = xhrObj.responseText;
        document.getElementsByTagName('head')[0].appendChild(se);
        console.log("NexU script loaded");
    }

}