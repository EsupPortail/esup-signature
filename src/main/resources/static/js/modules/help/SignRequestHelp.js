export class SignRequestHelp {

    constructor(splashMessage) {
        this.doneTour = false;
        this.splashMessage = splashMessage;
        this.intro = introJs();
        this.intro.setOptions({nextLabel: 'Suivant', prevLabel: 'Précédent', doneLabel: 'Terminer', skipLabel: 'Passer', showStepNumbers: 'false', overlayOpacity: 1})
        this.initListeners();
        this.initStep();
    }

    initListeners() {
        this.intro.onafterchange(e => this.modButtons());
        this.intro.oncomplete(function () {
            localStorage.setItem('signRequestIntro', 'Completed');
        });

        this.intro.onexit(function () {
            localStorage.setItem('signRequestIntro', 'Completed');
        });
        $("#helpStartButton").on('click', e => this.start());
    }

    initStep() {

        this.intro.addStep({
            intro: "Cette est l'interface va vous permettre de :" +
                "<ul>" +
                "<li>Consulter le document que vous devez signer</li>" +
                "<li>Signer le document (avec ou sans certificat)</li>" +
                "<li>Ajouter des commentaires à des endroits précis du document</li>" +
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
                "<li>d'ajouter ou de modifier la/les signature(s)</li>",
            position: 'auto'
        });
        if($.trim($("#signTools").html()) !== '') {
            this.intro.addStep({
                element: '#signTools',
                highlightClass: 'intro-js-custom-highlight',
                intro: "Ici vous pouvez ajouter des signatures ainsi que les mentions \"signé par\", \"signé le\"",
                position: 'bottom'
            });
        }
        this.intro.addStep({
            element: '#sidebar',
            intro: "La barre latérale vous permet de basculer sur le mode \"commentaires\" et vous informe sur l'avancé du circuit",
            position: 'right'
        });
        if($.trim($("#cross").html()) !== '') {
            this.intro.addStep({
                element: '#cross',
                highlightClass: 'intro-js-transparent-highlight',
                intro: "Déplacer votre signature en maintenant le bouton gauche de la souris enfoncée",
                position: 'bottom'
            });
        }
        if($.trim($("#signButtons").html()) !== '') {
            this.intro.addStep({
                element: '#signButtons',
                intro: "Utilisez les boutons suivants pour viser, signer ou refuser les documents",
                position: 'left'
            });
        }
    }

    autoStart() {
        this.doneTour = localStorage.getItem('signRequestIntro') === 'Completed';
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