import {default as FilesInput} from "../utils/FilesInput.js";
import {default as SelectUser} from "../utils/SelectUser.js";
import {Step} from "../../prototypes/Step.js";

export class WizUi {

    constructor(workflowId, div, workflowName, csrf) {
        this.workflowId = workflowId;
        this.signBookId = "";
        this.div = div;
        this.workflowName = workflowName;
        this.csrf = csrf;
        this.mode = "";
        this.input;
        this.fileInput
        this.modal = $('#' + this.div.attr('id').replace("Frame", "Modal"));
        this.initListeners();
    }

    initListeners() {
        this.modal.on('hidden.bs.modal', e => this.checkOnModalClose());
    }

    checkOnModalClose() {
        let workflowId = $("#wizWorkflowId").val();
        if(this.signBookId || workflowId) {
            if (bootbox.confirm("Attention si vous fermez cette fenêtre, les modifications seront perdues")) {
                if(workflowId) {
                    $.ajax({
                        method: "DELETE",
                        url: "/user/workflows/silent-delete/" + workflowId + "?" + this.csrf.parameterName + "=" + this.csrf.token,
                        cache: false
                    });
                } else {
                    $.ajax({
                        method: "DELETE",
                        url: "/user/signbooks/silent-delete/" + this.signBookId + "?" + this.csrf.parameterName + "=" + this.csrf.token,
                        cache: false
                    });
                }
                this.modal.modal('hide');
                this.modal.unbind();
            } else {
                this.modal.modal('show');
            }
        }
    }

    startByDocs() {
        console.info("Start wizard signbook");
        $.ajax({
            type: "GET",
            url: '/user/wizard/wiz-start-by-docs?workflowId=' + this.workflowId,
            dataType : 'html',
            cache: false,
            success : html => this.initWiz1(html)
        });
    }

    startByRecipients() {
        console.info("Start wizard workflow");
        this.mode = "-workflow";
        $.ajax({
            type: "GET",
            url: '/user/wizard/wiz-init-steps-workflow',
            dataType : 'html',
            cache: false,
            success : html => this.initWiz2(html)
        });
    }

    initWiz1(html) {
        this.div.html(html);
        this.input = $("#multipartFiles_" + this.workflowId);
        if(!this.workflowId) this.input = $("#multipartFiles_0");
        this.fileInput = new FilesInput(this.input, this.workflowName, this.workflowName, null, false, this.csrf, null);
        this.fileInput.addEventListener("uploaded", e => this.gotoStep2(e));
    }

    gotoStep2(e) {
        this.div.html("");
        this.signBookId = e;
        console.log(this.signBookId);
        $.ajax({
            type: "GET",
            url: '/user/wizard/wiz-init-steps/' + this.signBookId + '?workflowId=' + this.workflowId,
            dataType : 'html',
            cache: false,
            success : html => this.initWiz2(html)
        });
    }

    initWiz2(html) {
        console.log(this.csrf);
        this.div.html(html);
        if($("#recipientsEmailsWiz").length) {
            new SelectUser("recipientsEmailsWiz");
        }
        $("#addNew").on('click', e => this.gotoAddStep(false, false));
        $("#end").on('click', e => this.gotoAddStep(true, false));
        $("#endStart").on('click', e => this.gotoAddStep(true, true));
        $("#exitWiz").on('click', e => this.exit());
        $("#saveWorkflow").on('click', e => this.saveWorkflow(e));

    }

    gotoAddStep(end, start) {
        let csrf = this.csrf;
        let step = new Step();
        step.workflowId = $('#wizWorkflowId').val();
        step.recipientsEmails = $('#recipientsEmailsWiz').find(`[data-check='true']`).prevObject[0].slim.selected();
        step.allSignToComplete = $('#allSignToCompleteWiz').is(':checked');
        step.signType = $('#signTypeWiz').val();
        let signBookId = this.signBookId;
        console.log(signBookId);
        $.ajax({
            url: "/user/wizard/wiz-add-step"+ this.mode +"/" + signBookId + "?end=" + end + "&start=" + start + "&" + csrf.parameterName + "=" + csrf.token,
            type: 'POST',
            contentType: "application/json",
            data: JSON.stringify(step),
            success: html => this.initWiz2(html)
        });
    }

    saveWorkflow(e) {
        let saveWorkflowForm = $("#saveWorkflowForm");
        if (saveWorkflowForm.find('.required').filter(function(){ return this.value === '' }).length > 0) {
            $("#saveWorkflowSubmit").click();
            return false;
        }

        let csrf = this.csrf;
        let name = $("#workflowName").val();
        let elementId = $("#elementId");
        $.ajax({
            url: "/user/wizard/wiz-save"+ this.mode +"/" + elementId.val() + "?name=" + name + "&" + csrf.parameterName + "=" + csrf.token,
            type: 'POST',
            success: html => this.initWiz2(html)
        });
    }

    exit() {
        let csrf = this.csrf;
        let signBookId = this.signBookId;
        $.ajax({
            url: "/user/wizard/wizend/" + signBookId + "?name=" + name + "&" + csrf.parameterName + "=" + csrf.token,
            type: 'POST',
            success: html => this.initWiz2(html)
        });
    }

}