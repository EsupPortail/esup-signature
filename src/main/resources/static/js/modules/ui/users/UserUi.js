import {UserSignaturePad} from "./UserSignaturePad.js?version=@version@";
import {UserSignatureCrop} from "./UserSignatureCrop.js?version=@version@";
import {SignRequestParams} from '../../../prototypes/SignRequestParams.js?version=@version@';

export class UserUi {

    constructor(userName, signRequestParams) {
        console.log('Starting user UI');
        this.emailAlertFrequencySelect = document.getElementById("emailAlertFrequency_id");
        this.emailAlertDay = document.getElementById("emailAlertDay");
        this.emailAlertHour = document.getElementById("emailAlertHour");
        this.userSignaturePad = new UserSignaturePad();
        this.userSignatureCrop = new UserSignatureCrop();
        this.saveSignRequestParams = false;
        this.checkAlertFrequency();
        if(signRequestParams != null) {
            this.saveSignRequestParams = true;
        }
        this.toggleSaveSignRequest();
        if($("#signRequestParamsFormDiv").length) {
            this.signRequestParams = new SignRequestParams(signRequestParams, 0, 1, 1, userName, userName, false, true, false, true, false, null, true, 0);
        }
        this.initListeners();
    }

    initListeners() {
        $("#saveButton").on('click', e => this.save());

        this.userSignatureCrop.addEventListener("started", e => this.userSignaturePad.clear());
        if(this.emailAlertFrequencySelect != null) {
            this.emailAlertFrequencySelect.addEventListener("change", e => this.checkAlertFrequency(e));
        }
        $('[id^="deleteSign_"]').each(function() {
            $(this).on('click', function (e){
                e.preventDefault();
                let target = e.currentTarget;
                bootbox.confirm("Voulez-vous vraiment supprimer cette signature ?", function(result){
                    if(result) {
                        location.href = $(target).attr('href');
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
            $("#sign-request-params").val(JSON.stringify(this.signRequestParams));
        }
        $("#userParamsForm").submit();
    }

    checkAlertFrequency() {
        if(this.emailAlertFrequencySelect != null) {
            let selectedValue = this.emailAlertFrequencySelect.options[this.emailAlertFrequencySelect.selectedIndex].value;
            if(selectedValue === 'daily') {
                this.emailAlertDay.style.display = "none";
                this.emailAlertHour.style.display = "flex";
            } else if(selectedValue === 'weekly') {
                this.emailAlertDay.style.display = "flex";
                this.emailAlertHour.style.display = "none";
            } else {
                this.emailAlertDay.style.display = "none";
                this.emailAlertHour.style.display = "none";
            }
        }
    }
}