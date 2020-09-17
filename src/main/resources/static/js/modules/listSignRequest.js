export default class ListSignRequest {

    constructor() {
        this.initListeners();
    }

    initListeners() {
        window.addEventListener("DOMMouseScroll", e => this.computeWhellEvent(e));
        window.addEventListener("wheel", e => this.computeWhellEvent(e));
    }

}