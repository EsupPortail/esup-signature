import {UserSignaturePad} from "./UserSignaturePad.js?version=@version@";
import {UserSignatureCrop} from "./UserSignatureCrop.js?version=@version@";
import {SignRequestParams} from '../../../prototypes/SignRequestParams.js?version=@version@';

export class UserUi {

    constructor(userName, signRequestParams) {
        console.log('Starting user UI');
        this.emailAlertFrequencySelect = $("#emailAlertFrequency_id");
        this.emailAlertDay = $("#emailAlertDayDiv");
        this.emailAlertHour = $("#emailAlertHourDiv");
        this.userSignaturePad = new UserSignaturePad("canvas", 1, 4);
        this.userSignatureCrop = new UserSignatureCrop();
        this.saveSignRequestParams = false;
        this.checkAlertFrequency();
        if(signRequestParams != null) {
            this.saveSignRequestParams = true;
        }
        this.toggleSaveSignRequest();
        if($("#signRequestParamsFormDiv").length) {
            this.signRequestParams = new SignRequestParams(signRequestParams, 0, 1, 1, userName, userName, false, true, false, false, null, true, 0);
        }
        this.initListeners();
    }

    initListeners() {
        $("#saveButton").on('click', e => this.save());

        this.userSignatureCrop.addEventListener("started", e => this.userSignaturePad.clear());
        if(this.emailAlertFrequencySelect != null) {
            this.emailAlertFrequencySelect.on("change", e => this.checkAlertFrequency(e));
        }
        $('[id^="deleteSign_"]').each(function() {
            $(this).on('click', function (e){
                e.preventDefault();
                let target = e.currentTarget;
                bootbox.confirm("Voulez-vous vraiment supprimer cette signature ?", function(result){
                    if(result) {
                        $('#deleteForm-' + $(target).attr("data-es-id")).submit();
                    }
                });
            });
        });
        $("#saveSignRequestParams").on("click", e => this.toggleSaveSignRequest(e));
        $("#signRequestParamsClean").on("click", e => this.clearLocalStorage())
    }

    clearLocalStorage() {
        bootbox.confirm("Vous allez remettre à zéro les paramètres par défaut de votre signature", function(result){
            if(result) {
                localStorage.clear();
                bootbox.alert("Vous paramètres ont été réinitialisés", null);

            }
        });
    }

    toggleSaveSignRequest() {
        if(this.saveSignRequestParams) {
            localStorage.clear();
            this.saveSignRequestParams = false;
            $("#signRequestParamsFormDiv").removeClass("d-none");
            $("#signRequestParamsCleanDiv").addClass("d-none");
        } else {
            this.saveSignRequestParams = true;
            $("#signRequestParamsFormDiv").addClass("d-none");
            $("#signRequestParamsCleanDiv").removeClass("d-none");
        }

    }

    save() {
        this.userSignaturePad.checkSignatureUpdate();
        if(!this.saveSignRequestParams) {
            this.signRequestParams.xPos = 0;
            this.signRequestParams.yPos = 0;
            let signRequestParams = JSON.stringify(this.signRequestParams);
            $("#sign-request-params").val(signRequestParams);
        }
        if($("#name").val() === "" || $("#firstname").val() === "") {
            $("#submitUserParamsForm").click();
            return;
        }
        if(this.userSignaturePad.signaturePad.isEmpty() && this.userSignatureCrop.signImageBase64 === null) {
            $("#signImageBase64").val("");
        }
        $("#userParamsForm").submit();
    }

    checkAlertFrequency() {
        let selectedValue = this.emailAlertFrequencySelect.val();
        console.info("selected check alert frequency : " + selectedValue);
        if(selectedValue === 'daily') {
            this.emailAlertDay.addClass("d-none");
            this.emailAlertHour.removeClass("d-none");
        } else if(selectedValue === 'weekly') {
            this.emailAlertDay.removeClass("d-none");
            this.emailAlertHour.addClass("d-none");
        } else {
            this.emailAlertDay.addClass("d-none");
            this.emailAlertHour.addClass("d-none");
        }
    }
}