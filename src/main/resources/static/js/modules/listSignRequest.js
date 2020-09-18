import {WheelDetector} from "./utils/WheelDetector.js";

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

    }

}