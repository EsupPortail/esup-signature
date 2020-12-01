export class HomeHelp {

    constructor(splashMessage) {
        this.splashMessage = splashMessage;
        this.intro = introJs();
        this.intro.setOptions({nextLabel: 'Suivant', prevLabel: 'Précédent', doneLabel: 'Terminer', skipLabel: 'Passer', showStepNumbers: 'false', overlayOpacity: 1})
        this.initListeners();
        this.initStep();
        this.doneTour = false;
    }

    initListeners() {
        this.intro.onafterchange(e => this.modButtons());
        this.intro.oncomplete(function () {
            localStorage.setItem('MyTour', 'Completed');
            $.get("/user/users/mark-as-read/0");
        });

        this.intro.onexit(function () {
            localStorage.setItem('MyTour', 'Completed');
            $.get("/user/users/mark-as-read/0");
        });
        $("#helpStartButton").on('click', e => this.start());
    }

    initStep() {
        if(this.splashMessage != null) {
            this.intro.addStep({
                intro: this.splashMessage.text
            });
        }

        this.intro.addStep({
            element: '#navbar-buttons',
            intro: "Voici les principaux éléments de navigation : <ul><li>Dans les brouillons vous retrouvez les formulaires en cours d'édition</li><li>Dans le tableau de bord, la liste de demandes à signer, ou en cours de signature</li><li>Dans outils, vous pouvez créer vos propres circuits de signature ou vérifier des documents</li></ul>",
            highlightClass: 'intro-js-custom-highlight',
            position: 'auto'
        });

        this.intro.addStep({
            element: '#user-buttons',
            intro: "Vous pouvez modifier / compléter vos paramètres à tous moments en utilisant le bouton paramètres en haut à droite.",
            highlightClass: 'intro-js-custom-highlight',
            position: 'left'
        });

        this.intro.addStep({
            element: '#newfastSign',
            intro: "Ce bouton vous permet de signer signer un document présent sur votre poste de travail",
            position: 'right'
        });
        this.intro.addStep({
            element: '#newSignDemand',
            intro: "Utilisez la demande simple pour faire signer le document à quelqu'un",
            position: 'right'
        });
        this.intro.addStep({
            element: '#newWizard',
            intro: "Ici vous pouvez créer une demande pour laquelle vous définissez un circuit personnalisé",
            position: 'right'
        });
        this.intro.addStep({
            element: '#newWorkflow',
            intro: "Les boutons <i class='fas fa-project-diagram'></i> permettent de démarrer des circuits personnalisés ou pré-définis ",
            position: 'right'
        });
        this.intro.addStep({
            element: '#newForm',
            intro: "Les boutons <i class='fas fa-file-alt'></i> permettent de remplir un formulaire",
            position: 'right'
        });
        this.intro.addStep({
            element: '#toSignList',
            intro: "Losrque vous avez un document à signer, il apparait dans cette liste",
            position: 'right'
        });
    }

    autoStart() {
        this.doneTour = localStorage.getItem('MyTour') === 'Completed';
        if (!this.doneTour) {
            this.intro.start();

        }
    }

    start() {
        $('#helpModal').modal('hide');
        this.intro.start();
    }

    modButtons() {
        $('.introjs-button').each(function(){
            console.log($(this));
            if($(this).hasClass('introjs-disabled')) {
                $(this).removeClass('introjs-disabled');
                $(this).addClass('disabled');
            }
            $(this).removeClass('introjs-button');
            $(this).addClass('btn btn-sm btn-light ml-1 btn-outline-dark');

        });
    }

    workflow() {
    }
}