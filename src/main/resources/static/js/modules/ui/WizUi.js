import {default as FilesInput} from "../utils/FilesInput.js?version=@version@";
import {default as SelectUser} from "../utils/SelectUser.js?version=@version@";
import {Step} from "../../prototypes/Step.js?version=@version@";
import {ExternalUserInfos} from "../../prototypes/ExternalUserInfos.js?version=@version@";

export class WizUi {

    constructor(workflowId, div, csrf, maxSize) {
        this.workflowId = workflowId;
        this.newSignBookId = "";
        this.newWorkflowId = "";
        this.div = div;
        this.csrf = csrf;
        this.maxSize = maxSize;
        this.pending=false;
        this.input;
        this.fileInput;
        this.recipientCCSelect;
        this.form;
        this.close = false;
        this.end = false;
        this.start = false;
        this.modal = $('#' + this.div.attr('id').replace("div", "modal"));
        $('#save-step').hide();
        this.initListeners();
    }

    initListeners() {
        this.modal.on('hidden.bs.modal', e => this.checkOnModalClose());
    }

    selfSignStart() {
        console.info("start self signbook");
        $.ajax({
            type: "GET",
            url: '/user/wizard/wiz-start-sign/self',
            dataType : 'html',
            cache: false,
            success : html => this.selfSignDisplayForm(html),
        });
    }

    selfSignDisplayForm(html) {
        let self = this;
        this.div.html(html);
        this.input = $("#multipartFiles");
        this.fileInput = new FilesInput(this.input, this.maxSize, this.csrf, null, false, null);
        this.input.on("filebatchuploadsuccess", function(event, data, previewId, index, fileId) {
            $.ajax({
                url: "/user/wizard/start-self-sign/" + self.newSignBookId + "?" + self.csrf.parameterName + "=" + self.csrf.token,
                type: 'POST',
                success: e => self.redirectToSignBook()
            });
        });
        $("#fast-sign-button").on('click', e => self.wizCreateSign("self"));
    }

    wizCreateSign(type) {
        console.info("create signbook");
        let self = this;
        $.ajax({
            url: "/user/wizard/wiz-create-sign/" + type + "?"+ self.csrf.parameterName + "=" + self.csrf.token,
            type: 'POST',
            success: function(signBookId) {
                self.newSignBookId = signBookId
                self.fileInput.signBookId = self.newSignBookId;
                self.input.fileinput("upload")
            }
        });
    }

    fastStartSign() {
        console.info("start fast signbook");
        $.ajax({
            type: "GET",
            url: '/user/wizard/wiz-start-sign/fast',
            dataType : 'html',
            cache: false,
            success : html => this.fastSignDisplayForm(html),
        });
    }

    fastSignDisplayForm(html) {
        this.div.html(html);
        this.input = $("#multipartFiles");
        let self = this;
        this.fileInput = new FilesInput(this.input, this.maxSize, this.csrf, null, false, null);
        if($("#recipientsEmails").length) {
            new SelectUser("recipientsEmails", null, null, this.csrf);
        }
        if($("#recipientsCCEmails").length) {
            this.recipientCCSelect = new SelectUser("recipientsCCEmails", null, null, this.csrf);
        }
        this.input.on("filebatchuploadsuccess", e => this.fastSignSubmitDatas());
        $("#send-draft-button").on('click', function() {
            self.wizCreateSign("fast");
        });
        $("#send-pending-button").on('click', function() {
            self.pending = true;
            self.wizCreateSign("fast");
        });
    }

    fastSignSubmitDatas() {
        let self = this;
        let step = new Step();
        step.title = $('#title').val();
        let recipientsEmails = $('#recipientsEmails').find(`[data-es-check-cert='true']`).prevObject[0].slim.getSelected();
        recipientsEmails.forEach(function(email) {
            let externalUserInfos = new ExternalUserInfos();
            externalUserInfos.email = email;
            let extInfos = $("div[id='externalUserInfos_" + email + "']");
            externalUserInfos.name = extInfos.find("#names").val();
            externalUserInfos.firstName = extInfos.find("#firstnames").val();
            externalUserInfos.phone = extInfos.find("#phones").val();
            externalUserInfos.forceSms = extInfos.find("#forcesmses").val() === "1";
            step.recipients.push(externalUserInfos);
        });
        step.allSignToComplete = $('#allSignToComplete').is(':checked');
        step.userSignFirst = $('#userSignFirst').is(':checked');
        step.comment = $('#comment').val();
        step.recipientsCCEmails = document.querySelector('#recipientsCCEmails').slim.getSelected();
        step.changeable = false;
        step.autoSign = false;
        step.signType = $('#signTypeNew').val();
        $.post({
            url: '/user/wizard/update-fast-sign/' + this.newSignBookId + '?' + this.csrf.parameterName + '=' + this.csrf.token + '&pending=' + self.pending,
            contentType: "application/json",
            data: JSON.stringify(step),
            success: e => this.redirectToSignBook(),
            error: function() {
                $("#update-fast-sign-submit").click();
            }
        });
    }

    workflowSignStart() {
        console.info("start workflow signbook");
        $.ajax({
            type: "GET",
            url: '/user/wizard/wiz-start-sign/workflow?workflowId=' + this.workflowId,
            dataType : 'html',
            cache: false,
            success : html => this.workflowSignDisplayForm(html)
        });
    }

    workflowSignDisplayForm(html) {
        let self = this;
        this.div.html(html);
        this.input = $("#multipartFiles_" + this.workflowId);
        if(!this.workflowId) this.input = $("#multipartFiles_0");
        this.newSignBookId = this.input.attr("data-es-signbook-id");
        this.fileInput = new FilesInput(this.input, this.maxSize, this.csrf, null, false, this.newSignBookId);
        if($("#recipientsCCEmails").length) {
            this.recipientCCSelect = new SelectUser("recipientsCCEmails", null, null, this.csrf);
        }
        this.input.on("filebatchuploadsuccess", function() {
            self.workflowSignNextStep();
        });
        $("#wiz-start-button").on('click', function (){
            $.ajax({
                type: "POST",
                url: '/user/wizard/wiz-create-workflow-sign?workflowId=' + self.workflowId + "&" + self.csrf.parameterName + "=" + self.csrf.token,
                success: function(signBookId) {
                    self.newSignBookId = signBookId
                    self.fileInput.signBookId = self.newSignBookId;
                    self.input.fileinput("upload")
                }
            });
        });
    }

    workflowSignSubmitDatas() {
        let form = $("#start-workflow-form");
        console.log("submit workflow sign");
        $.post({
            url: '/user/wizard/wiz-init-workflow/' + this.newSignBookId + '?' + this.csrf.parameterName + '=' + this.csrf.token,
            data: form.serialize(),
            success: e => this.workflowSignNextStep(),
            error: function () {
                $("#workflow-form-submit").click();
            }
        });
    }

    wizardWorkflowStart() {
        console.info("start wizard workflow");
        $.ajax({
            type: "GET",
            url: '/user/wizard/wiz-start-workflow',
            dataType : 'html',
            cache: false,
            success : html => this.workflowSignNextStepDisplay(html)
        });
    }

    workflowSignNextStep() {
        console.info("Next workflow step");
        let comment = $("#comment");
        let title = $("#title-wiz");
        let id = this.workflowId;
        if(id === "") {
            id = 0;
        }
        let recipientsCCEmails=[];
        $('select[name="recipientsCCEmails' + id + '"] option:selected').each(function() {
            recipientsCCEmails.push($(this).val());
        });
        let forceAllSign = $('input[name="forceAllSign"]').is(":checked");
        this.div.html("");
        let self = this;
        $.ajax({
            type: "GET",
            url: '/user/wizard/wiz-new-step/' + self.newSignBookId + '?workflowId=' + id + "&recipientsCCEmails=" + recipientsCCEmails + "&forceAllSign=" + forceAllSign + "&comment=" + encodeURIComponent(comment.val()) + "&title=" + title.val(),
            dataType : 'html',
            cache: false,
            success : html => this.workflowSignNextStepDisplay(html)
        });
    }

    workflowSignNextStepDisplay(html) {
        console.info("Last workflow step");
        let self = this;
        this.div.html(html);
        if($("#recipientsEmails").length) {
            new SelectUser("recipientsEmails", null, null, this.csrf);
        }
        $('[id^="recipientEmailsWizSelect_"]').each(function (){
            let maxRecipient = $(this).attr('data-es-max-recipient');
            new SelectUser($(this).attr('id'), maxRecipient, null, self.csrf);
        });
        if($("#targetEmailsSelect").length) {
            new SelectUser("targetEmailsSelect", null, null, this.csrf);
        }
        if($("#recipientsCCEmails").length) {
            this.recipientCCSelect = new SelectUser("recipientsCCEmails", null, null, this.csrf);
        }
        if($("#workflowId").length) {
            this.newWorkflowId = $("#workflowId").val();
        }
        $("#end-workflow-sign").on('click', function (){
            self.end = true;
            self.start = true;
            self.workflowSignSubmitStepData();
        });
        $("#wiz-exit").on('click', e => this.exit());
        $("#save-workflow").on('click', e => this.saveWorkflow());
        $("#save-step").on('click', e => this.workflowSignSubmitStepData());
        $("#send-draft-button").on('click', e => this.workflowSignSubmitLastStepData(false));
        $("#send-pending-button").on('click', e => this.workflowSignSubmitLastStepData(true));
    }


    workflowSignSubmitStepData() {
        console.info("Submit step");
        let csrf = this.csrf;
        let step = new Step();
        step.workflowId = this.workflowId
        let recipientsEmailsSelect = $('#recipientsEmails');
        let recipientsEmails = recipientsEmailsSelect.find(`[data-es-check-cert='true']`).prevObject[0].slim.getSelected();
        $(recipientsEmails).each(function(e, email) {
            let externalUserInfos = new ExternalUserInfos();
            externalUserInfos.email = email;
            step.recipients.push(externalUserInfos);
        });
        $("div[id^='externalUserInfos_']").each(function() {
            let externalUserInfos = new ExternalUserInfos();
            externalUserInfos.email = $(this).find("#emails").val();
            externalUserInfos.name = $(this).find("#names").val();
            externalUserInfos.firstName = $(this).find("#firstnames").val();
            externalUserInfos.phone = $(this).find("#phones").val();
            externalUserInfos.forceSms = $(this).find("#forcesmses").val();
            step.recipients.push(externalUserInfos);
        });
        step.allSignToComplete = $('#allSignToComplete').is(':checked');
        step.changeable = $('#changeable').is(':checked');
        step.autoSign = $('#autoSign').is(':checked');
        step.signType = $('#signType').val();
        let userSignFirst = $('#userSignFirst').is(':checked');
        let self = this;
        let mode = "workflow";
        let elementId = -1;
        if (this.newSignBookId !== "") {
            mode = "signbook";
            elementId = this.newSignBookId;
        } else if (this.newWorkflowId !== "") {
            elementId = this.workflowId;
        }
        $.ajax({
            url: '/user/wizard/wiz-add-step-' + mode +  '/' + elementId + '?end=' + self.end + '&userSignFirst=' + userSignFirst + '&start=' + self.start + '&close=' + self.close + '&' + csrf.parameterName + '=' + csrf.token,
            type: 'POST',
            contentType: "application/json",
            data: JSON.stringify(step),
            success: html => this.workflowSignNextStepDisplay(html),
            error: function(data){
                console.error(data.responseJSON.message);
                bootbox.alert("Une erreur s'est produite. Merci de vérifier votre saisie", function (){ });
            }
        });
    }

    workflowSignSubmitLastStepData(pending) {
        let steps = [];
        let i = 0;
        $("div[id^='step-wiz-']").each(function() {
            i++;
            let step = new Step();
            step.title = $('#title').val();
            let recipientsSelect = $('#recipientEmailsWizSelect_' + i).find(`[data-es-check-cert='true']`).prevObject[0];
            if(!recipientsSelect) return;
            let recipientsEmails = recipientsSelect.slim.getSelected();
            recipientsEmails.forEach(function (email) {
                let externalUserInfos = new ExternalUserInfos();
                externalUserInfos.email = email;
                let extInfos = $("div[id='externalUserInfos_" + email + "']");
                externalUserInfos.name = extInfos.find("#names").val();
                externalUserInfos.firstName = extInfos.find("#firstnames").val();
                externalUserInfos.phone = extInfos.find("#phones").val();
                externalUserInfos.forceSms = extInfos.find("#forcesmses").val() === "1";
                step.recipients.push(externalUserInfos);
            });
            step.allSignToComplete = $('#allSignToComplete').is(':checked');
            step.changeable = false;
            step.autoSign = false;
            step.signType = $('#signTypeNew').val();
            steps.push(step);
        });
        $.post({
            url: '/user/wizard/wiz-init-workflow/' + this.newSignBookId + '?' + this.csrf.parameterName + '=' + this.csrf.token + '&pending=' + pending,
            contentType: "application/json",
            data: JSON.stringify(steps),
            success: e => this.redirectToSignBook(),
            error: function() {
                $("#send-sign-submit").click();
            }
        });
    }

    saveWorkflow() {
        let csrf = this.csrf;
        let name = $("#workflowName").val();
        let self = this;
        let mode = "workflow";
        let elementId = -1;
        if (this.newSignBookId !== "") {
            mode = "signbook";
            elementId = this.newSignBookId;
        } else if (this.newWorkflowId !== "") {
            elementId = this.newWorkflowId;
        }
        $.ajax({
            url: "/user/wizard/wiz-save-"+ mode +"/" + elementId + "?name=" + name + "&viewers=" + self.recipientCCSelect.slimSelect.getSelected() + "&" + csrf.parameterName + "=" + csrf.token,
            type: 'POST',
            success: function() {
                if(self.newSignBookId !== "") {
                    location.href = "/user/signbooks/" + self.newSignBookId;
                } else {
                    location.href = "/user";
                }
            }
        });
    }

    exit() {
        let csrf = this.csrf;
        let self = this;
        if(this.newSignBookId !== ""){
            $.ajax({
                url: "/user/wizard/wiz-end/" + this.newSignBookId + "?name=" + name + "&close=" + $('#close').val() + "&" + csrf.parameterName + "=" + csrf.token,
                type: 'POST',
                success: function() {
                    location.href = "/user/signbooks/" + self.newSignBookId;
                }
            });
        } else {
            $.ajax({
                url: "/user/wizard/wiz-save-workflow/" + self.newWorkflowId,
                type: 'GET',
                success: html => this.saveWorkflowDisplay(html)
            });
        }
    }

    saveWorkflowDisplay(html) {
        this.div.html(html);
        if($("#recipientsCCEmails").length) {
            this.recipientCCSelect = new SelectUser("recipientsCCEmails", null, null, this.csrf);
        }
        $("#save-workflow").on('click', e => this.saveWorkflow());

    }

    checkOnModalClose() {
        let self = this;
        if(self.newSignBookId || self.newWorkflowId) {
            bootbox.confirm("Attention si vous fermez cette fenêtre, les modifications seront perdues", function(result) {
                if(result) {
                    if (self.newWorkflowId !== "") {
                        $.ajax({
                            method: "DELETE",
                            url: "/ws-secure/global/silent-delete-workflow/" + self.newWorkflowId + "?" + self.csrf.parameterName + "=" + self.csrf.token,
                            cache: false
                        });
                    } else if (self.newSignBookId !== "") {
                        $.ajax({
                            method: "DELETE",
                            url: "/ws-secure/global/silent-delete-signbook/" + self.newSignBookId + "?" + self.csrf.parameterName + "=" + self.csrf.token,
                            cache: false
                        });
                    }
                    self.modal.modal('hide');
                    self.modal.unbind();
                    self.div.html("");
                } else {
                    self.modal.modal('show');
                }
            });
        } else {
            this.modal.modal('hide');
            this.modal.unbind();
            this.div.html("");
        }
   }

    redirectToSignBook() {
        location.href = "/user/signbooks/" + this.newSignBookId;
    }

}