export default class Step {

    stepper;

    constructor(stepDiv) {
        console.info("Start stepper");
        this.stepper = new Stepper(stepDiv, {
            linear: false,
            animation: true
        });
    }

    stepTo(num) {
        this.stepper.to(num);
    }

}