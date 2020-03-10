export class Step {

    stepper;

    constructor(stepDiv) {
        this.stepper = new Stepper(stepDiv, {
            linear: false,
            animation: true
        });
    }

    stepTo(num) {
        this.stepper.to(num);
    }

}