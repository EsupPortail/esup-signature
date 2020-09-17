import EventFactory from "./EventFactory.js";

export default class WheelDetection extends EventFactory {


    constructor() {
        super();
        this.initListeners();
    }

    initListeners() {
        window.addEventListener("DOMMouseScroll", e => this.computeWhellEvent(e));
        window.addEventListener("wheel", e => this.computeWhellEvent(e));
    }

    computeWhellEvent(event) {
        console.debug("wheel event");
        if(event.ctrlKey === true) {
            if (this.detectMouseWheelDirection(event) === 'down'){
                console.debug("wheel down zoom out");
                this.fireEvent("zoomout");
            } else {
                console.debug("wheel up zoom in");
                this.fireEvent("zoomin");
            }
        } else {
            if (this.detectMouseWheelDirection(event) === 'down' && $(window).scrollTop() + $(window).height() === $(document).height()) {
                console.debug("wheel down next page");
                this.fireEvent("pagebottom");
            } else if (this.detectMouseWheelDirection(event) === 'up' && window.scrollY === 0) {
                console.debug("wheel up prev page");
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