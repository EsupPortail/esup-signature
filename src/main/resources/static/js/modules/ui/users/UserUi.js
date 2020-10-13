import {UserSignaturePad} from "./UserSignaturePad.js";
import {UserSignatureCrop} from "./UserSignatureCrop.js";

export default class UserUi {

    constructor(lastSign, signWidth, signHeight) {
        console.log('Starting user UI');
        this.emailAlertFrequencySelect = document.getElementById("emailAlertFrequency_id");
        this.emailAlertDay = document.getElementById("emailAlertDay");
        this.emailAlertHour = document.getElementById("emailAlertHour");
        this.userSignaturePad = new UserSignaturePad(lastSign, signWidth, signHeight);
        this.userSignatureCrop = new UserSignatureCrop();
        this.checkAlertFrequency();
        this.initListeners();
    }

    initListeners() {
        this.userSignatureCrop.addEventListener("started", e => this.userSignaturePad.clear());
        this.emailAlertFrequencySelect.addEventListener("change", e => this.checkAlertFrequency());
    }

    checkAlertFrequency() {
        let selectedValue = this.emailAlertFrequencySelect.options[this.emailAlertFrequencySelect.selectedIndex].value;
        if (selectedValue === 'daily') {
            this.emailAlertDay.style.display = "none";
            this.emailAlertHour.style.display = "flex";
        } else if (selectedValue === 'weekly') {
            this.emailAlertDay.style.display = "flex";
            this.emailAlertHour.style.display = "none";
        } else {
            this.emailAlertDay.style.display = "none";
            this.emailAlertHour.style.display = "none";
        }
    }
}