import {UiParams} from "../utils/UiParams.js?version=@version@";

export class HomeUi {

    constructor(startFormId, startWorkflowId) {
        console.info("Starting home UI");
        this.noFilterButton = $("#noFilterButton");
        this.workflowFilterButton = $("#workflowFilterButton");
        this.formFilterButton = $("#formFilterButton");
        this.globalFilterButton = $("#globalFilterButton");
        this.workflowFilterStatus = true;
        this.formFilterStatus = true;
        this.globalFilterStatus = true;
        this.uiParams = new UiParams();
        this.initListeners();
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
                let formButton = $('#form-button-' + startFormId);
                if(formButton.length) {
                    formButton[0].click();
                } else {
                    bootbox.alert("Ce formulaire n'a pas été trouvé. Vérifier si vous avez bien les droits pour accéder à ce formulaire")
                }
            }
            if(startWorkflowId != null) {
                let workflowButton = $('#workflow-button-' + startWorkflowId);
                if(workflowButton.length) {
                    workflowButton.click();
                } else {
                    bootbox.alert("Ce circuit n'a pas été trouvé. Vérifier si vous avez bien les droits pour accéder à ce circuit")
                }
            }
        });
    }

    initListeners() {
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

    hideAll() {
        $('.global-button').addClass('d-none');
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
        $('.global-button').removeClass('d-none');
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
        $('.global-button').removeClass('d-none');
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

}