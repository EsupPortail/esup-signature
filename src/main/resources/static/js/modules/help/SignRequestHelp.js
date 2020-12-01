export class SignRequestHelp {

    constructor(splashMessage) {
        this.splashMessage = splashMessage;
        this.intro = introJs();
        this.intro.setOptions({nextLabel: 'Suivant', prevLabel: 'Précédent', doneLabel: 'Terminer', skipLabel: 'Passer', showStepNumbers: 'false', overlayOpacity: 1})
        this.initListeners();
        this.initStep();
    }

    initListeners() {
        this.intro.onafterchange(e => this.modButtons());
        this.intro.oncomplete(function(){
            $.get("/user/users/mark-as-read/0");
        });
        $("#helpStartButton").on('click', e => this.start());
    }

    initStep() {

        this.intro.addStep({
            intro: "Cette est l'interface va vous permettre de :" +
                "<ul>" +
                "<li>Consulter de le document que vous devez signer</li>" +
                "<li>Signer le document (avec ou sans certificat)</li>" +
                "<li>Ajouter des commentaire à des endroits précis du document</li>" +
                "</ul>"
        });

        this.intro.addStep({
            element: '#tools',
            highlightClass: 'intro-js-transparent-highlight',
            intro: "La barre d'outils permet :" +
                "<ul>" +
                "<li>d'obtenir des details sur la demande</li>" +
                "<li>de naviguer dans le document (ajuster le zoom, changer de page)</li>" +
                "<li>d'ajouter des pièces jointes (document ou liens)</li>" +
                "<li>dajouter ou de modifier la/les signature</li>",
            position: 'auto'
        });

        this.intro.addStep({
            element: '#signTools',
            highlightClass: 'intro-js-custom-highlight',
            intro: "Ici vous pouvez ajouter des signature et les mentions \"signé par\", \"signé le\"",
            position: 'bottom'
        });
        this.intro.addStep({
            element: '#sidebar',
            intro: "La barre latérale vous permet de basculer sur le mode \"commentaires\" et vous informe sur l'avancée du circuit",
            position: 'right'
        });
        this.intro.addStep({
            element: '#cross',
            highlightClass: 'intro-js-transparent-highlight',
            intro: "Deplacer votre signature en maintenant le bouton gauche de la sourie enfoncée",
            position: 'bottom'
        });
        this.intro.addStep({
            element: '#signButtons',
            intro: "Utilisez les boutons suivants pour viser, signer ou refuser les documents",
            position: 'left'
        });
    }

    start() {
        // this.intro.onbeforechange(function (targetElement){
        //     if (targetElement.id === 'signType2') {
        //         $('#sendSignRequestModal').modal('show');
        //         $('.introjs-overlay, .introjs-helperLayer, .introjs-tooltipReferenceLayer, .introjs-fixedTooltip').appendTo('#signType2');
        //     }
        // });
        $('#helpModal').modal('hide');
        this.intro.start();
        // this.modButtons();
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