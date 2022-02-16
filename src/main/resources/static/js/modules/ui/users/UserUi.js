import {UserSignaturePad} from "./UserSignaturePad.js?version=@version@";
import {UserSignatureCrop} from "./UserSignatureCrop.js?version=@version@";

export default class UserUi {

    constructor() {
        console.log('Starting user UI');
        this.emailAlertFrequencySelect = document.getElementById("emailAlertFrequency_id");
        this.emailAlertDay = document.getElementById("emailAlertDay");
        this.emailAlertHour = document.getElementById("emailAlertHour");
        this.userSignaturePad = new UserSignaturePad();
        this.userSignatureCrop = new UserSignatureCrop();
        this.checkAlertFrequency();
        this.initListeners();
    }

    initListeners() {
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