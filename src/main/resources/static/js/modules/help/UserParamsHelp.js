export class UserParamsHelp {

    constructor(doneTour) {
        this.doneTour = doneTour;
        this.intro = introJs();
        this.intro.setOptions({nextLabel: 'Suivant', prevLabel: 'Précédent', doneLabel: 'Terminer', skipLabel: 'x', showStepNumbers: 'false', overlayOpacity: 0.8, disableInteraction: true})
        this.initListeners();
        this.initStep();
    }

    initListeners() {
        this.intro.onbeforechange(e => this.scrollTop(e));
        this.intro.onafterchange(e => this.modButtons());
        this.intro.onexit(function () {
            $.get("/user/users/mark-intro-as-read/userParamsHelp");
        });
        $("#helpStartButton").on('click', e => this.start());
    }

    initStep() {

        this.intro.addStep({
            intro: "Sur cette page vous pouvez modifier vos paramètres de signature :" +
                "<ul>" +
                "<li>Ajouter / supprimer des images de votre signature</li>" +
                "<li>Ajouter un certificat pour effectuer des signatures électroniques</li>" +
                "<li>Ajuster vos paramètres d'alerte</li>" +
                "</ul>"
        });

        this.intro.addStep({
            element: '#mySigns',
            intro: "La liste des signatures déjà enregistrées." +
                "<br>" +
                "Vous avez aussi la possibilité d'en supprimer.",
            position: 'auto'
        });
        this.intro.addStep({
            element: '#vanilla-upload',
            intro: "Pour ajouter une signature, vous pouvez parcours votre ordinateur pour choisir une image/scan de votre signature.",
            position: 'bottom'
        });
        this.intro.addStep({
            element: '#canvas',
            intro: "Vous pouvez aussi dessiner une signature directement à la souris.",
            position: 'bottom'
        });
        this.intro.addStep({
            element: '#keyForm',
            intro: "Vous avez la possibilité (si besoin) de stocker un magasin de certificat dans votre profil." +
                "<br>" +
                "Cela ne sera utile que si une signature électronique avec certificat est exigée." +
                "<br>" +
                "Dans la plupart cette étape peut être ignorée.",
            position: 'bottom'
        });
        this.intro.addStep({
            element: '#emailAlertFrequency',
            intro: "Enfin, vous pouvez ajouter la fréquence d'envoi de alerte mail.",
            position: 'top'
        });
        this.intro.addStep({
            intro: "Attention à bien enregistrer vos modifications avec le bouton en bas à droite.",
            position: 'top'
        });
    }

    autoStart() {
        if (!this.doneTour) {
            this.intro.start();
        }
    }

    start() {
        $('#helpModal').modal('hide');
        this.intro.start();
    }

    scrollTop(e) {
        window.scrollTo(0, 0);
    }

    modButtons(e) {
        $('.introjs-button').each(function(){
            if($(this).hasClass('introjs-disabled')) {
                $(this).removeClass('introjs-disabled');
                $(this).addClass('disabled');
            }
            $(this).removeClass('introjs-button');
            $(this).addClass('btn btn-sm btn-light ms-1 btn-outline-dark');

        });
    }

}