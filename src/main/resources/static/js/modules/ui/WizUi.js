import {default as FilesInput} from "../utils/FilesInput.js";
import {default as SelectUser} from "../utils/SelectUser.js";
import {Step} from "../../prototypes/Step.js";
import {ExternalUserInfos} from "../../prototypes/ExternalUserInfos.js";

export class WizUi {

    constructor(workflowId, div, workflowName, csrf) {
        this.workflowId = workflowId;
        this.signBookId = "";
        this.div = div;
        this.workflowName = workflowName;
        this.csrf = csrf;
        this.mode = "";
        this.input;
        this.fileInput;
        this.close = false;
        this.end = false;
        this.start = false;
        this.modal = $('#' + this.div.attr('id').replace("Frame", "Modal"));
        $('#addNew').hide();
        this.initListeners();
    }

    initListeners() {
        this.modal.on('hidden.bs.modal', e => this.checkOnModalClose());
    }

    checkOnModalClose() {
        let workflowId = $("#wizWorkflowId").val();
        if(this.signBookId || workflowId) {
            let self = this;
            bootbox.confirm("Attention si vous fermez cette fenêtre, les modifications seront perdues", function(result) {
                if(result) {
                    if (workflowId) {
                        $.ajax({
                            method: "DELETE",
                            url: "/user/workflows/silent-delete/" + workflowId + "?" + self.csrf.parameterName + "=" + self.csrf.token,
                            cache: false
                        });
                    } else {
                        $.ajax({
                            method: "DELETE",
                            url: "/user/signbooks/silent-delete/" + self.signBookId + "?" + self.csrf.parameterName + "=" + self.csrf.token,
                            cache: false
                        });
                    }
                    self.modal.modal('hide');
                    self.modal.unbind();
                } else {
                    self.modal.modal('show');
                }
            });
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
        $.ajax({
            type: "GET",
            url: '/user/wizard/wiz-init-steps/' + this.signBookId + '?workflowId=' + this.workflowId,
            dataType : 'html',
            cache: false,
            success : html => this.initWiz2(html)
        });
    }

    initWiz2(html) {
        let csrf = this.csrf;
        this.div.html(html);
        if($("#recipientsEmailsWiz").length) {
            new SelectUser("recipientsEmailsWiz", null, null, csrf);
        }
        $('[id^="recipientEmailsWizSelect_"]').each(function (){
            new SelectUser($(this).attr('id'), null, null, csrf);
        });
        $('[id^="targetEmailsSelect_"]').each(function (){
            new SelectUser($(this).attr('id'), null, null, csrf);
        });
        let self = this;
        $("#end").on('click', function (){
            self.end = true;
        });
        $("#endStart").on('click', function (){
            self.end = true;
            self.start = true;
        });
        $("#exitWiz").on('click', e => this.exit());
        $("#saveWorkflow").on('click', e => this.saveWorkflow(e));
        $("#wiz-step-form").on('submit', e => this.gotoAddStep(e));
    }

    gotoAddStep(e) {
        e.preventDefault();
        let csrf = this.csrf;
        let step = new Step();
        step.workflowId = $('#wizWorkflowId').val();
        step.recipientsEmails = $('#recipientsEmailsWiz').find(`[data-check='true']`).prevObject[0].slim.selected();
        step.allSignToComplete = $('#allSignToCompleteWiz').is(':checked');
        let userSignFirst = $('#_userSignFirstWiz').is(':checked');
        step.signType = $('#signTypeWiz').val();
        $("div[id^='externalUserInfos_']").each(function() {
            let externalUserInfos = new ExternalUserInfos();
            externalUserInfos.email = $(this).find("#emails").val();
            externalUserInfos.name = $(this).find("#names").val();
            externalUserInfos.firstname = $(this).find("#firstnames").val();
            externalUserInfos.phone = $(this).find("#phones").val();
            step.externalUsersInfos.push(externalUserInfos);
        });
        let signBookId = this.signBookId;
        console.log(signBookId);
        let self = this;
        $.ajax({
            url: "/user/wizard/wiz-add-step"+ this.mode +"/" + signBookId + "?end=" + self.end + "&userSignFirst=" + userSignFirst + "&start=" + self.start + "&close=" + self.close + "&" + csrf.parameterName + "=" + csrf.token,
            type: 'POST',
            contentType: "application/json",
            data: JSON.stringify(step),
            success: html => this.initWiz2(html),
            error: function(data){
                console.error(data.responseJSON.message);
                bootbox.alert("Une erreur s'est produite. Merci de vérifier votre saisie", function (){ });
            }
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
        let self = this;
        if(this.signBookId !== ""){
            $.ajax({
                url: "/user/wizard/wizend/" + self.signBookId + "?name=" + name + "&close=" + $('#close').val() + "&" + csrf.parameterName + "=" + csrf.token,
                type: 'POST',
                success: html => this.initWiz2(html)
            });
        } else {
            $.ajax({
                url: "/user/wizard/wiz-save-workflow/" + $('#wizWorkflowId').val(),
                type: 'GET',
                success: html => this.initWiz2(html)
            });
        }

    }

}