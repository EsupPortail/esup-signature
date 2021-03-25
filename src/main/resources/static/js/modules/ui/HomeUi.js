import {UiParams} from "../utils/UiParams.js";

export class HomeUi {

    constructor() {
        console.info("Starting home UI");
        this.workflowFilterButton = $('#workflowFilterButton');
        this.formFilterButton = $('#formFilterButton');
        this.globalFilterButton = $('#globalFilterButton');
        this.workflowFilterStatus = true;
        this.formFilterStatus = true;
        this.globalFilterStatus = true;
        this.menuToggled = false;
        this.uiParams = new UiParams();
        this.initListeners();
    }

    initListeners() {
        $('#toggleNewGrid').on('click', e => this.toggleNewMenu());
        $('#newScroll').on('mousewheel DOMMouseScroll', e => this.activeHorizontalScrolling(e));
        this.workflowFilterButton.on('click', e => this.filterWorkflows(e));
        this.globalFilterButton.on('click', e => this.filterGlobal(e));
        this.formFilterButton.on('click', e => this.filterForms(e));
        $('[id^="deleteWorkflow_"]').each(function (){
            $(this).on('submit', function (e){
                e.preventDefault();
                let target = e.currentTarget;
                bootbox.confirm("Voulez-vous vraiment supprimer ce circuit ?", function (result) {
                    if(result) {
                        target.submit();
                    }
                });
            });
        });
        this.uiParams.addEventListener("ready", e => this.initUiParams());
    }

    initUiParams() {
        this.initWorkflowFilter();
    }

    initWorkflowFilter() {
        this.workflowFilterStatus = this.uiParams.get("workflowFilterStatus");
        if(this.workflowFilterStatus == null) {
            this.workflowFilterStatus = true;
            this.uiParams.set("workflowFilterStatus", true).then(e => this.initFormFilter());
        } else {
            if(this.workflowFilterStatus === "false") {
                this.filterWorkflows().then(e => this.initFormFilter());
            } else {
                this.initFormFilter()
            }
        }
    }

    initFormFilter() {
        this.formFilterStatus = this.uiParams.get("formFilterStatus")
        if(this.formFilterStatus == null) {
            this.formFilterStatus = true;
            this.uiParams.set("formFilterStatus", true).then(e => this.initGlobalFilter());
        } else {
            if(this.formFilterStatus === "false") {
                this.filterForms().then(e => this.initGlobalFilter());
            } else {
                this.initGlobalFilter();
            }
        }
    }

    initGlobalFilter() {
        this.globalFilterStatus = this.uiParams.get("globalFilterStatus");
        if(this.globalFilterStatus == null) {
            this.globalFilterStatus = true;
            this.uiParams.set("globalFilterStatus", true);
        } else {
            if(this.globalFilterStatus === "false") {
                this.filterGlobal();
            }
        }
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

    filterGlobal(e) {
        this.globalFilterButton.toggleClass('btn-secondary btn-light');
        $('.globalButton').toggleClass('d-none');
        this.globalFilterStatus = !this.globalFilterStatus;
        return this.uiParams.set("globalFilterStatus", this.globalFilterStatus);
    }

    filterWorkflows(e) {
        this.workflowFilterButton.toggleClass('btn-secondary btn-light');
        $('.workflowButton').toggleClass('d-none');
        this.workflowFilterStatus = !this.workflowFilterStatus;
        return this.uiParams.set("workflowFilterStatus", this.workflowFilterStatus);
    }

    filterForms(e) {
        this.formFilterButton.toggleClass('btn-secondary btn-light');
        $('.formButton').toggleClass('d-none');
        this.formFilterStatus = !this.formFilterStatus;
        return this.uiParams.set("formFilterStatus", this.formFilterStatus);
    }

    activeHorizontalScrolling(e){
        if(!this.menuToggled) {
            let delta = Math.max(-1, Math.min(1, (e.originalEvent.wheelDelta || -e.originalEvent.detail)));
            $(e.currentTarget).scrollLeft($(e.currentTarget).scrollLeft() - ( delta * 40 ) );
            e.preventDefault();
        }
    }

}