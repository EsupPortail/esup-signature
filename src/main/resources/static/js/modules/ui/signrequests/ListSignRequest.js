import {WheelDetector} from "../../utils/WheelDetector.js";

export default class ListSignRequest {

    constructor() {
        this.wheelDetector = new WheelDetector();
        this.signRequestTable = $("#signRequestTable")
        this.initListeners();
    }

    initListeners() {
        this.wheelDetector.addEventListener("pagebottom", e => this.addToPage());
    }

    addToPage() {
        console.info("Add to page");
        $.get("/user/signrequests/list-ws?page=2", function( data ) {
            console.log(data);
            $('#signRequestTable tr:last').after('' +
                '<tr>data.</tr><tr>...</tr>' +
                '');

        });
    }

}