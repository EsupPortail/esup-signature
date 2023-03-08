import {CsrfToken} from "../../prototypes/CsrfToken.js?version=@version@";

export class NewUi {

    constructor(csrf) {
        console.info("Starting New UI");
        this.csrf = new CsrfToken(csrf);
        this.initListeners();
    }

    initListeners() {
        $("#send-pending-button").on('click', e => this.checkUserCertificate(true));
        $("#send-draft-button").on('click', e => this.checkUserCertificate(false));
        $("#fast-sign-button").on("click", e => this.submitFastSignRequest(e));
    }

    checkUserCertificate(send) {
        if ($('#signType2').val() === 'certSign') {
            let self = this;
            $.ajax({
                url: "/user/users/check-users-certificate?" + self.csrf.parameterName + "=" + self.csrf.token,
                type: 'POST',
                contentType: "application/json",
                dataType: 'json',
                data: JSON.stringify($('#recipientsEmails').find(`[data-es-check-cert='true']`).prevObject[0].slim.getSelected()),
                success: response => this.checkSendPending(response, send)
            });
        } else {
            this.submitSendPending(send);
        }
    }


    checkSendPending(data, send) {
        if (data.length === 0) {
            this.submitSendPending(send);
            return;
        }
        let self = this;
        let stringChain = "Les utilisateurs suivants n’ont pas de certificats électroniques : <br><ul>";
        for (let i = 0; i < data.length ; i++) {
            stringChain += "<li>" + data[i].firstname + " " + data[i].name + "</li>";
        }
        stringChain += "</ul>Confirmez-vous l’envoie de la demande ? "
        bootbox.confirm(stringChain, function(result) {
            if(result) {
                self.submitSendPending(send);
            }
        });
    }

    submitSendPending(send) {
        let form = $("#send-sign-form");
        let fileInput = $("#send-sign-file-input");
        let nbFiles = fileInput.fileinput('getFilesCount')
        if(nbFiles === 0) {
            $("#send-sign-submit").click();
            return;
        }
        this.finishSignBook(form, fileInput, send)
    }

    submitFastSignRequest(e) {
        let form = $("#fast-sign-form");
        let fileInput = $("#fast-sign-file-input");
        let nbFiles = fileInput.fileinput('getFilesCount')
        if(nbFiles === 0) {
            $("#fast-form-submit").click();
            return;
        }
        $("#fast-sign-button").addClass("d-none");
        $("#fast-form-cancel").removeClass("d-none");
        $("#fast-form-close").addClass("d-none");
        this.finishSignBook(form, fileInput, true)
    }

    finishSignBook(form, fileInput, send) {
        let self = this;
        $.post({
            url: form.attr("action"),
            data: form.serialize(),
            success: function(signBookId) {
                fileInput.on('filebatchuploadsuccess', function() {
                    $.post({
                        url : "/ws-secure/signrequests/finish-signbook/?" + self.csrf.parameterName + "=" + self.csrf.token,
                        success: function() {
                            if(send) {
                                location.href = "/user/signbooks/pending/" + signBookId;
                            } else {
                                location.href = "/user/signbooks/" + signBookId;
                            }
                        }
                    });
                });
                fileInput.fileinput("upload");
            },
            error: function() {
                $("#send-sign-submit").click();
            }
        });
    }

}