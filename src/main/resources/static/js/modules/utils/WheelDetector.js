import {EventFactory} from "./EventFactory.js?version=@version@";

export class WheelDetector extends EventFactory {

    constructor() {
        super();
        this.initListeners();
        this.lastScrollTop = 0;

    }

    initListeners() {
        let self = this;
        window.addEventListener("DOMMouseScroll", e => this.computeWhellEvent(e));
        window.addEventListener("wheel", e => this.computeWhellEvent(e), {passive: false});
        window.addEventListener("scroll", function(){ // or window.addEventListener("scroll"....
            let st = window.scrollY || document.documentElement.scrollTop;
            if (st > this.lastScrollTop){
                self.fireEvent("down", [window.scrollY]);
            } else {
                self.fireEvent("up", [window.scrollY]);
            }
            this.lastScrollTop = st <= 0 ? 0 : st; // For Mobile or negative scrolling
        }, false);

        window.addEventListener("resize", (e) => {
            if (window.devicePixelRatio === 1) {
                this.fireEvent("zoominit", [e]);
            }
        });
    }

    computeWhellEvent(event) {
        const workspace = document.getElementById("pdf");
        if (event.ctrlKey) {
            if(workspace && workspace.contains(event.target))  {
                event.preventDefault();
                if (this.detectMouseWheelDirection(event) === 'down'){
                    console.debug("debug - " + "wheel down zoom out");
                    this.fireEvent("zoomin", [event]);
                } else {
                    console.debug("debug - " + "wheel up zoom in");
                    this.fireEvent("zoomout", [event]);
                }
            } else {
                if (this.detectMouseWheelDirection(event) === 'down') {
                    console.debug("debug - " + "wheel down zoom out");
                    this.fireEvent("zoomout", [event]);
                } else {
                    console.debug("debug - " + "wheel up zoom in");
                    this.fireEvent("zoomin", [event]);
                }
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