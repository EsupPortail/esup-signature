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
        this.turnAllButtonsOff();
        this.globalFilterButton.removeClass('btn-light');
        this.globalFilterButton.addClass('btn-secondary');
        $('.workflowButton').addClass('d-none');
        $('.formButton').addClass('d-none');
        $('.globalButton').removeClass('d-none');
        $('.noForm').addClass('d-none');
        $('.noWorkflow').addClass('d-none');
    }

    filterWorkflows(e) {
        this.turnAllButtonsOff();
        this.workflowFilterButton.removeClass('btn-light');
        this.workflowFilterButton.addClass('btn-secondary');
        $('.noWorkflow').removeClass('d-none');
        $('.noForm').addClass('d-none');
        $('.workflowButton').removeClass('d-none');
        $('.formButton').addClass('d-none');
        $('.globalButton').addClass('d-none');
    }

    filterForms(e) {
        this.turnAllButtonsOff();
        this.formFilterButton.removeClass('btn-light');
        this.formFilterButton.addClass('btn-secondary');
        $('.noForm').removeClass('d-none');
        $('.noWorkflow').addClass('d-none');
        $('.workflowButton').addClass('d-none');
        $('.formButton').removeClass('d-none');
        $('.globalButton').addClass('d-none');
    }

    filterNothing(e) {
        this.turnAllButtonsOff();
        this.noFilterButton.removeClass('btn-light');
        this.noFilterButton.addClass('btn-secondary');
        $('.workflowButton').removeClass('d-none');
        $('.formButton').removeClass('d-none');
        $('.globalButton').removeClass('d-none');
        $('.noWorkflow').addClass('d-none');
        $('.noForm').addClass('d-none');
    }

    turnAllButtonsOff() {
        this.workflowFilterButton.removeClass('btn-secondary');
        this.formFilterButton.removeClass('btn-secondary');
        this.globalFilterButton.removeClass('btn-secondary');
        this.noFilterButton.removeClass('btn-secondary');
        this.workflowFilterButton.addClass('btn-light');
        this.formFilterButton.addClass('btn-light');
        this.globalFilterButton.addClass('btn-light');
        this.noFilterButton.addClass('btn-light');
    }

    activeHorizontalScrolling(e){
        if(!this.menuToggled) {
            let delta = Math.max(-1, Math.min(1, (e.originalEvent.wheelDelta || -e.originalEvent.detail)));
            $(e.currentTarget).scrollLeft($(e.currentTarget).scrollLeft() - ( delta * 40 ) );
            e.preventDefault();
        }
    }

}