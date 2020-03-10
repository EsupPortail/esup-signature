export class Home {

    constructor() {
        this.init();
    }

    init() {
        $('#toggleNewGrid').on('click', e => this.toggleNewMenu());
        $('#newScroll').on('mousewheel DOMMouseScroll', e => this.activeHorizontalScrolling(e));
    }

    toggleNewMenu () {
        console.info("toggle new menu");
        $('#newScroll').toggleClass('text-nowrap').toggleClass('new-min-h');
        $('#toSignList').toggleClass('d-flex d-none');
        $('#toggleNewGrid').children().toggleClass('fa-th fa-chevron-up');
    }

    activeHorizontalScrolling(event){
        event.preventDefault();
        var delta = Math.max(-1, Math.min(1, (event.originalEvent.wheelDelta || -event.originalEvent.detail)));
        $(this).scrollLeft( $(this).scrollLeft() - ( delta * 100 ) );
    }

}