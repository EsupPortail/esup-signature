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
        this.checkAlertFrequency();
        console.log(signRequestParams);
        this.signRequestParams =  new SignRequestParams(signRequestParams, 0, 1, 1, userName, userName, false, true, false, true, false, null, true);
        this.initListeners();
    }

    initListeners() {
        $("#saveButton").on('click', e => this.save());

        this.userSignatureCrop.addEventListener("started", e => this.userSignaturePad.clear());
        if(this.emailAlertFrequencySelect != null) {
            this.emailAlertFrequencySelect.addEventListener("change", e => this.checkAlertFrequency());
        }
        $('[id^="deleteSign_"]').each(function() {
            $(this).on('click', function (e){
                e.preventDefault();
                let target = e.currentTarget;
                bootbox.confirm("Voulez-vous vraiment supprimer cette signature ?", function(result){
                    if(result) {
                        location.href = $(target).attr('href');
                    }
                })
            });
        });

    }

    save() {
        this.userSignaturePad.checkSignatureUpdate();
        // alert(this.signRequestParams.addExtra);
        $("#sign-request-params").val(JSON.stringify(this.signRequestParams));
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