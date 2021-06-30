import {EventFactory} from "./EventFactory.js";

export class WheelDetector extends EventFactory {

    constructor() {
        super();
        this.initListeners();
    }

    initListeners() {
        window.addEventListener("DOMMouseScroll", e => this.computeWhellEvent(e));
        window.addEventListener("wheel", e => this.computeWhellEvent(e));
    }

    computeWhellEvent(event) {
        if(event.ctrlKey === true) {
            if (this.detectMouseWheelDirection(event) === 'down'){
                console.debug("wheel down zoom out");
                this.fireEvent("zoomout");
            } else {
                console.debug("wheel up zoom in");
                this.fireEvent("zoomin");
            }
        } else {
            if (this.detectMouseWheelDirection(event) === 'down' && parseInt($(window).scrollTop() + $(window).height()) >= parseInt($(document).height())) {
                console.debug("wheel down");
                this.fireEvent("pagebottom");
            } else if (this.detectMouseWheelDirection(event) === 'up' && window.scrollY === 0) {
                console.debug("wheel up");
                this.fireEvent("pagetop");
            }
        }
    }

    detectMouseWheelDirection(e) {
        let delta = null,
            direction = false;
        let e_delta = (e.deltaY || -e.wheelDelta || e.detail);
        if ( e_delta ) {
            delta = e_delta  / 60;
        } else if ( e.detail ) {
            delta = -e.detail / 2;
        }
        if ( delta !== null ) {
            direction = delta > 0 ? 'down' : 'up';
        }
        return direction;
    }

}