import {default as FilesInput} from "../utils/FilesInput.js?version=@version@";
import {default as SelectUser} from "../utils/SelectUser.js?version=@version@";
import {Step} from "../../prototypes/Step.js?version=@version@";
import {Recipient} from "../../prototypes/Recipient.js?version=@version@";

export class WizUi {

    constructor(workflowId, div, csrf, maxSize) {
        this.workflowId = workflowId;
        this.newSignBookId = "";
        this.newWorkflowId = "";
        this.div = div;
        this.csrf = csrf;
        this.maxSize = maxSize;
        this.pending = false;
        this.input = null;
        this.fileInput = null;
        this.recipientCCSelect = null;
        this.form = null;
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

    listenHelpMarkAsReadButton(btn) {
        console.debug("debug - " + "listen to" + btn);
        $(btn).on('click', e => this.markHelpAsRead(e));
    }

    markHelpAsRead(e) {
        let id = e.target.id.split('_')[1];
        console.info("mark help as read message " + id);
        $.get("/user/users/mark-help-as-read/" + id);
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
        this.input.on("filebatchuploadsuccess", function() {
            $.ajax({
                url: "/user/wizard/start-self-sign/" + self.newSignBookId + "?" + self.csrf.parameterName + "=" + self.csrf.token,
                type: 'POST',
                success: function() {
                    location.href = "/user/signbooks/" + self.newSignBookId;
                }
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
        let recipientsEmails;
        if($("#recipientsEmails-1").length) {
            recipientsEmails = new SelectUser("recipientsEmails-1", null, null, this.csrf);
        }
        if($("#recipientsCCEmails").length) {
            this.recipientCCSelect = new SelectUser("recipientsCCEmails", null, null, this.csrf);
        }
        this.input.on("filebatchuploadsuccess", e => this.fastSignSubmitDatas());
        $("#send-draft-button").on('click', function() {
            self.wizCreateSign("fast");
        });
        $("#send-pending-button").on('click', function() {
            if(recipientsEmails.slimSelect.getSelected().length > 0) {
                self.pending = true;
                self.wizCreateSign("fast");
            } else {
                $("#update-fast-sign-submit").click();
            }
        });
    }

    fastSignSubmitDatas() {
        let self = this;
        let successCallback = function () {
            location.href = "/user/signbooks/" + self.newSignBookId;
        }
        let errorCallback = function(e) {

            $("#update-fast-sign-submit").click();

        }
        this.sendSteps('/user/wizard/update-fast-sign/' + this.newSignBookId + '?pending=' + self.pending, $("#update-fast-sign"), successCallback, errorCallback);

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
        $('button[id^="markHelpAsReadButton_"]').each((index, e) => this.listenHelpMarkAsReadButton(e));
    }

    wizardFormStart(formId) {
        console.info("start wizard form");
        $.ajax({
            type: "GET",
            url: '/user/wizard/wiz-start-form/'+ formId,
            dataType : 'html',
            cache: false,
            success : html => this.startFormDisplayForm(html)
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
        let id = this.workflowId;
        if(id === "") {
            id = 0;
        }
        let self = this;
        let successCallback = function (html) {
            self.workflowSignNextStepDisplay(html)
        }
        let errorCallback = function (e) {
            $("#workflow-form-submit").click();
        }
        this.sendSteps('/user/wizard/wiz-new-step/' + self.newSignBookId + '?workflowId=' + id, $("#start-workflow-form"), successCallback, errorCallback);
        this.div.html("");
    }

    workflowSignNextStepDisplay(html) {
        console.info("Last workflow step");
        let self = this;
        this.div.html(html);
        $('[id^="recipientsEmails-"]').each(function (){
            if($(this).is('select')) {
                let maxRecipient = $(this).attr('data-es-max-recipient');
                new SelectUser($(this).attr('id'), maxRecipient, null, self.csrf);
            }
        });
        if($("#targetEmailsSelect").length) {
            new SelectUser("targetEmailsSelect", null, null, this.csrf);
        }
        if($("#recipientsCCEmails").length) {
            this.recipientCCSelect = new SelectUser("recipientsCCEmails", null, null, this.csrf);
        }
        let workflowIdInput = $("#workflowId");
        if(workflowIdInput.length) {
            this.newWorkflowId = workflowIdInput.val();
        }
        $("#end-workflow-sign").on('click', function (){
            self.end = true;
            self.start = true;
            self.workflowSignSubmitStepData();
        });
        $("#wiz-end").on('click', e => this.wizardEnd());
        $("#wiz-exit").on('click', e => this.wizardExit());
        $("#save-workflow").on('click', e => this.saveWorkflow());
        $("#save-step").on('click', e => this.workflowSignSubmitStepData());
        $("#send-draft-button").on('click', e => this.workflowSignSubmitLastStepData(false));
        $("#send-pending-button").on('click', e => this.workflowSignSubmitLastStepData(true));
    }

    workflowSignSubmitStepData() {
        console.info("Submit step");
        let self = this;
        let mode = "workflow";
        let elementId = -1;
        if (this.newSignBookId !== "") {
            mode = "signbook";
            elementId = this.newSignBookId;
        } else if (this.newWorkflowId !== "") {
            elementId = this.newWorkflowId;
        }
        let successCallback = function(html) {
            self.workflowSignNextStepDisplay(html)
        };
        let errorCallback = function (data) {
            $("#wiz-step-form-submit").click();
        };
        let url = '/user/wizard/wiz-add-step-' + mode +  '/' + elementId + '?end=' + self.end + '&start=' + self.start + '&close=' + self.close;
        this.sendSteps(url, $("#wiz-step-form"), successCallback, errorCallback);

    }

    workflowSignSubmitLastStepData(pending) {
        let self = this;
        let successCallback = function() {
            location.href = "/user/signbooks/" + self.newSignBookId;
        }
        let errorCallback = function(e) {
            if(e.responseText.startsWith('400'))  {
                bootbox.alert("Une erreur s’est produite lors du démarrage du circuit<br>" + e.responseText, function() {
                    self.closeModal();
                });
            } else {
                $("#send-sign-submit").click();
            }
        }
        this.sendSteps('/user/wizard/wiz-init-workflow/' + this.newSignBookId + '?pending=' + pending, $("li[id^='step-wiz-']"), successCallback, errorCallback);
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
            },
            error: function() {
                $("#save-workflow-submit").click();
            }
        });
    }

    wizardExit() {
        location.href = "/user/signbooks/" + this.newSignBookId;
    }

    wizardEnd() {
        let self = this;
        $.ajax({
            url: "/user/wizard/wiz-save-workflow/" + self.newWorkflowId,
            type: 'GET',
            success: html => self.saveWorkflowDisplay(html)
        });
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

                } else {
                    self.modal.modal('show');
                }
            });
        } else {
            this.closeModal();
        }
   }

   closeModal() {
       this.modal.modal('hide');
       this.modal.unbind();
       this.div.html("");
   }

    startFormDisplayForm(html) {
        let self = this;
        this.div.html(html);
        $('[id^="recipientsEmails-"]').each(function (){
            let maxRecipient = $(this).attr('data-es-max-recipient');
            new SelectUser($(this).attr('id'), maxRecipient, null, self.csrf);
        });
        $("#send-form-button").on("click", e => this.sendForm(e));
        $('button[id^="markHelpAsReadButton_"]').each((index, e) => this.listenHelpMarkAsReadButton(e));
    }

    sendForm(e) {
        let formId = $(e.target).attr('data-es-form-id');
        let spinner = $("#send-form-spinner");
        spinner.removeClass("d-none");
        let successCallback = function(id) {
            location.href = "/user/signbooks/" + id;
        };
        let errorCallback = function(e) {
            $("#send-form-submit").click();
            spinner.addClass("d-none");
        };
        this.sendSteps('/user/datas/send-form/' + formId + '?pending=' + false, $("li[id^='step-form-']"), successCallback, errorCallback);
    }

    sendSteps(url, stepsSources, successCallback, errorCallback) {
        let self = this;
        let errorMsg = new Object();
        errorMsg.responseText = "500";
        let steps = [];
        let i = 0;
        stepsSources.each(function() {
            i++;
            let step = new Step();
            step.stepNumber=i;
            step.title = $('#title').val();
            step.comment = $("#comment").val();
            let recipientsSelect = $('#recipientsEmails-' + i).find(`[data-es-check-cert='true']`).prevObject[0];
            if(recipientsSelect) {
                let recipientsEmails = recipientsSelect.slim.getSelected();
                recipientsEmails.forEach(function (email) {
                    let recipient = new Recipient();
                    recipient.email = email;
                    let extInfos = $("div[id='recipient_" + email + "']");
                    recipient.name = extInfos.find("#names").val();
                    recipient.firstName = extInfos.find("#firstnames").val();
                    recipient.phone = extInfos.find("#phones").val();
                    recipient.forceSms = extInfos.find("#forcesmses").prop("checked");
                    step.recipients.push(recipient);
                });
            }
            $('div[id^="extRecipient-"]').each(function() {
                if($(this).attr("id").includes("-" + i + "-")) {
                    let recipient = new Recipient();
                    recipient.email = $(this).attr("data-es-email");
                    let extInfos = $(this);
                    recipient.name = extInfos.find("#names").val();
                    if(recipient.name == null || recipient.name === '') {
                        errorCallback(errorMsg);
                        return;
                    }
                    recipient.firstName = extInfos.find("#firstnames").val();
                    if(recipient.firstName == null || recipient.firstName === '') {
                        errorCallback(errorMsg);
                        return;
                    }
                    recipient.phone = extInfos.find("#phones").val();
                    recipient.forceSms = extInfos.find("#forcesmses").prop("checked");
                    step.recipients.push(recipient);
                }
            })
            $('select[name="recipientsCCEmails"] option:selected').each(function() {
                step.recipientsCCEmails.push($(this).val());
            });
            step.userSignFirst = $('#userSignFirst').is(':checked');
            step.allSignToComplete = $('#all-sign-to-complete-' + i).is(':checked');
            step.changeable = $('#changeable-' + i).is(':checked');
            step.autoSign = $('#autoSign-' + i).is(':checked');
            step.signType = $('#signType-' + i).val();
            step.forceAllSign = $('input[name="forceAllSign"]').is(":checked");
            steps.push(step);
        });
        $.post({
            url: url + '&' + self.csrf.parameterName + '=' + self.csrf.token,
            contentType: "application/json",
            data: JSON.stringify(steps),
            success: e => successCallback(e),
            error: e => errorCallback(e)
        });
    }
}