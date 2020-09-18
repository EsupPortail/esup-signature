import {WheelDetector} from "../../utils/WheelDetector.js";

export default class ListSignRequest {

    constructor(totalElementsToDisplay) {
        this.totalElementsToDisplay = totalElementsToDisplay;
        this.wheelDetector = new WheelDetector();
        this.signRequestTable = $("#signRequestTable")
        this.page = 1;
        this.initListeners();
    }

    initListeners() {
        this.wheelDetector.addEventListener("pagebottom", e => this.addToPage());
    }

    addToPage() {
        if(this.totalElementsToDisplay >= (this.page - 1) * 5 ) {
            console.info("Add to page");
            this.page++;
            $.get("/user/signrequests/list-ws?page=" + this.page, function (data) {
                $('#signRequestTable').append(data);
            });
        }
    }
}