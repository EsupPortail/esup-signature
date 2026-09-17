import {EventFactory} from "./EventFactory.js?version=@version@";
export class WheelDetector extends EventFactory {
    constructor() {
        super();
        this.initListeners();
        this.lastScrollTop = 0;
    }
    initListeners() {
        let self = this;
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

}