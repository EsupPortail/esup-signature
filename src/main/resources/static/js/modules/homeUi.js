export class HomeUi {

    constructor() {
        console.info("Starting home UI");
        this.workflowFilterButton = $('#workflowFilterButton');
        this.formFilterButton = $('#formFilterButton');
        this.globalFilterButton = $('#globalFilterButton');
        this.noFilterButton = $('#noFilterButton');
        this.menuToggled = false;
        this.initListeners();
    }

    initListeners() {
        $('#toggleNewGrid').on('click', e => this.toggleNewMenu());
        $('#newScroll').on('mousewheel DOMMouseScroll', e => this.activeHorizontalScrolling(e));
        this.workflowFilterButton.on('click', e => this.filterWorkflows(e));
        this.globalFilterButton.on('click', e => this.globalWorkflows(e));
        this.formFilterButton.on('click', e => this.filterForms(e));
        this.noFilterButton.on('click', e => this.filterNothing(e));
    }

    toggleNewMenu() {
        console.info("toggle new menu");
        $('#newScroll').toggleClass('text-nowrap').toggleClass('new-min-h');
        $('#toSignList').toggleClass('d-flex d-none');
        $('#toggleNewGrid').children().toggleClass('fa-th fa-chevron-up');
        $('.newHr').toggleClass('d-none');
        $('#newContainer').toggleClass('d-inline').toggleClass("text-left");
        $('.newToggled').toggleClass('d-none');
        $('.noForm').toggleClass('d-none');
        $('.noWorkflow').toggleClass('d-none');
        this.menuToggled = !this.menuToggled;
    }

    globalWorkflows(e) {
        this.workflowFilterButton.addClass('disabled');
        this.formFilterButton.addClass('disabled');
        this.globalFilterButton.removeClass('disabled');
        this.noFilterButton.addClass('disabled');
        $('.workflowButton').addClass('d-none');
        $('.formButton').addClass('d-none');
        $('.globalButton').removeClass('d-none');
        $('.noForm').addClass('d-none');
        $('.noWorkflow').addClass('d-none');
    }

    filterWorkflows(e) {
        this.workflowFilterButton.removeClass('disabled');
        this.formFilterButton.addClass('disabled');
        this.globalFilterButton.addClass('disabled');
        this.noFilterButton.addClass('disabled');
        $('.noWorkflow').removeClass('d-none');
        $('.noForm').addClass('d-none');
        $('.workflowButton').removeClass('d-none');
        $('.formButton').addClass('d-none');
        $('.globalButton').addClass('d-none');

    }

    filterForms(e) {
        this.workflowFilterButton.addClass('disabled');
        this.formFilterButton.removeClass('disabled');
        this.globalFilterButton.addClass('disabled');
        this.noFilterButton.addClass('disabled');
        $('.noForm').removeClass('d-none');
        $('.noWorkflow').addClass('d-none');
        $('.workflowButton').addClass('d-none');
        $('.formButton').removeClass('d-none');
        $('.globalButton').addClass('d-none');
    }

    filterNothing(e) {
        this.workflowFilterButton.addClass('disabled');
        this.formFilterButton.addClass('disabled');
        this.noFilterButton.removeClass('disabled');
        $('.workflowButton').removeClass('d-none');
        $('.formButton').removeClass('d-none');
        $('.globalButton').removeClass('d-none');
        $('.noWorkflow').addClass('d-none');
        $('.noForm').addClass('d-none');

    }

    activeHorizontalScrolling(event){
        if(!this.menuToggled) {
            event.preventDefault();
            var delta = Math.max(-1, Math.min(1, (event.originalEvent.wheelDelta || -event.originalEvent.detail)));
            $(this).scrollLeft($(this).scrollLeft() - (delta * 100));
        }
    }

}