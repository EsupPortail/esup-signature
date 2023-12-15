import {UiParams} from "../utils/UiParams.js?version=@version@";

export class HomeUi {

    constructor(startFormId) {
        console.info("Starting home UI");
        this.noFilterButton = $("#noFilterButton");
        this.workflowFilterButton = $("#workflowFilterButton");
        this.formFilterButton = $("#formFilterButton");
        this.globalFilterButton = $("#globalFilterButton");
        this.workflowFilterStatus = true;
        this.formFilterStatus = true;
        this.globalFilterStatus = true;
        this.menuToggled = false;
        this.uiParams = new UiParams();
        this.initListeners();
        if(localStorage.getItem('menuToggled') === "true") {
            this.toggleNewMenu();
        }
        $(document).ready(function () {
            let oldSignRequests = $("#oldSignRequests");
            if(oldSignRequests.length) {
                oldSignRequests.modal('show');
                $("#warningReaded").on('click', function () {
                    $.get("/ws-secure/global/warning-readed");
                });

            }
            let recipientNotPresentSignRequests = $("#recipientNotPresentSignRequests");
            if(recipientNotPresentSignRequests.length) {
                recipientNotPresentSignRequests.modal('show');
            }
            if(startFormId != null) {
                $("#sendModal_" + startFormId).modal('show');
            }
        });
    }

    initListeners() {
        $('#toggle-new-grid').on('click', e => this.toggleNewMenu());
        $('#new-scroll').on('wheel', e => this.activeHorizontalScrolling(e));
        this.noFilterButton.on('click', e => this.showAll(e));
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
    }

    toggleNewMenu() {
        console.info("toggle new menu");
        $('#new-scroll').toggleClass('text-nowrap').toggleClass('new-min-h');
        // $('#to-sign-list').toggleClass('d-flex d-none');
        let newDiv = $('#new-div');
        newDiv.toggleClass('position-fixed');
        newDiv.toggleClass('new-width');
        newDiv.toggleClass('new-height');
        $('#toggle-new-grid').children().toggleClass('fa-th fa-chevron-up');
        $('#listSignRequestTable').toggleClass('d-none');
        $('.newHr').toggleClass('d-none');
        $('#newContainer').toggleClass('d-inline').toggleClass("text-left");
        $('.newToggled').toggleClass('d-none');
        $('.noForm').toggleClass('d-none');
        $('.noWorkflow').toggleClass('d-none');
        this.menuToggled = !this.menuToggled;
        localStorage.setItem('menuToggled', this.menuToggled);
    }

    hideAll() {
        $('.globalButton').addClass('d-none');
        $('.workflow-button').addClass('d-none');
        $('.form-button').addClass('d-none');
        this.noFilterButton.removeClass('btn-secondary');
        this.noFilterButton.addClass('btn-light');
        this.globalFilterButton.removeClass('btn-secondary');
        this.workflowFilterButton.removeClass('btn-secondary');
        this.formFilterButton.removeClass('btn-secondary');
        this.globalFilterButton.addClass('btn-light');
        this.workflowFilterButton.addClass('btn-light');
        this.formFilterButton.addClass('btn-light');
    }

    showAll() {
        $('.globalButton').removeClass('d-none');
        $('.workflow-button').removeClass('d-none');
        $('.form-button').removeClass('d-none');
        this.noFilterButton.addClass('btn-secondary');
        this.noFilterButton.removeClass('btn-light');
        this.globalFilterButton.removeClass('btn-secondary');
        this.workflowFilterButton.removeClass('btn-secondary');
        this.formFilterButton.removeClass('btn-secondary');
        this.globalFilterButton.addClass('btn-light');
        this.workflowFilterButton.addClass('btn-light');
        this.formFilterButton.addClass('btn-light');
    }



    filterGlobal(e) {
        this.hideAll();
        this.globalFilterButton.removeClass('btn-light');
        this.globalFilterButton.addClass('btn-secondary');
        $('.globalButton').removeClass('d-none');
        this.globalFilterStatus = !this.globalFilterStatus;
        return this.uiParams.set("globalFilterStatus", this.globalFilterStatus);
    }

    filterWorkflows(e) {
        this.hideAll();
        this.workflowFilterButton.removeClass('btn-light');
        this.workflowFilterButton.addClass('btn-secondary');
        $('.workflow-button').removeClass('d-none');
        this.workflowFilterStatus = !this.workflowFilterStatus;
        return this.uiParams.set("workflowFilterStatus", this.workflowFilterStatus);
    }

    filterForms(e) {
        this.hideAll();
        this.formFilterButton.removeClass('btn-light');
        this.formFilterButton.addClass('btn-secondary');
        $('.form-button').removeClass('d-none');
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