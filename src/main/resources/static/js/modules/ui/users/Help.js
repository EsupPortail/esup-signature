export class Help {

    constructor() {
        this.initListeners();
        this.intro = introJs();
        this.initStep();
    }

    initStep() {
        this.intro.addStep({
            element: document.querySelectorAll('#newfastSign')[0],
            intro: "Ok, wasn't that fun?",
            position: 'right'
        });
        this.intro.addStep({
            element: document.querySelectorAll('#newSignDemand')[0],
            intro: "Ok, wasn't that fun2?",
            position: 'right'
        });
        this.intro.addStep({
            element: document.querySelectorAll('#newWizard')[0],
            intro: "Ok, wasn't that fun3?",
            position: 'right'
        });
        this.intro.addStep({
            element: document.querySelectorAll('#newWorkflow')[0],
            intro: "Ok, wasn't that fun3?",
            position: 'right'
        });
        this.intro.addStep({
            element: document.querySelectorAll('#newForm')[0],
            intro: "Ok, wasn't that fun3?",
            position: 'right'
        });
        this.intro.addStep({
            element: document.querySelectorAll('#toSignList')[0],
            intro: "Ok, wasn't that fun3?",
            position: 'right'
        });
        this.intro.addStep({
            element: document.querySelectorAll('#signType2')[0],
            intro: "Ok, wasn't that fun3?",
            position: 'right'
        });
    }

    initListeners() {
        $("#helpStartButton").on('click', e => this.start());
    }

    start() {
        this.intro.onbeforechange(function (targetElement){
            if (targetElement.id === 'signType2') {
                $('#sendSignRequestModal').modal('show');
                $('.introjs-overlay, .introjs-helperLayer, .introjs-tooltipReferenceLayer, .introjs-fixedTooltip').appendTo('#signType2');
            }
        });
        $('#helpModal').modal('hide');

        this.intro.start();
    }

    workflow() {
    }
}