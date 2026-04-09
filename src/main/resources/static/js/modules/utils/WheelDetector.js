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

        const workspace = document.getElementById("workspace");
        if(workspace) {
            workspace.addEventListener("scroll", function() {
                let st = workspace.scrollTop;
                if (st > self.lastScrollTop){
                    self.fireEvent("down", [st]);
                } else {
                    self.fireEvent("up", [st]);
                }
                self.lastScrollTop = st <= 0 ? 0 : st;
            }, false);
        }
    }

    computeWhellEvent(event) {
        const workspace = document.getElementById("pdf");
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