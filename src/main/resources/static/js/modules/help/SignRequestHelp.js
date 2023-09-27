export class SignRequestHelp {

    constructor(doneTour, isOtp) {
        this.doneTour = doneTour;
        this.isOtp = isOtp;
        this.intro = introJs();
        this.intro.setOptions({nextLabel: 'Suivant', prevLabel: 'Précédent', doneLabel: 'Terminer', skipLabel: 'x', showStepNumbers: 'false', overlayOpacity: 0.3})
        this.initListeners();
        this.initStep();
    }

    initListeners() {
        this.intro.onbeforechange(e => this.scrollTop(e));
        this.intro.onafterchange(e => this.modButtons());
        let self = this;
        this.intro.onexit(function () {
            if(self.isOtp) {
                $.get("/otp/users/mark-intro-as-read/signRequestHelp");
            } else {
                $.get("/user/users/mark-intro-as-read/signRequestHelp");
            }
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
            intro: "La barre d'outils permet :" +
                "<ul>" +
                "<li>d'obtenir des details sur la demande</li>" +
                "<li>de naviguer dans le document (ajuster le zoom, changer de page)</li>" +
                "<li>d'ajouter des pièces jointes (document ou liens)</li>" +
                "<li>d'ajouter ou de modifier la/les signature(s)</li>",
            position: 'auto'
        });
        if($.trim($("#sign-tools").html()) !== '') {
            this.intro.addStep({
                element: '#sign-tools',
                intro: "Ici vous pouvez ajouter des signatures ainsi que les mentions \"signé par\", \"signé le\"",
                position: 'bottom'
            });
        }
        this.intro.addStep({
            element: '#sidebar',
            intro: "La barre latérale vous permet de basculer sur le mode \"commentaires\" et vous informe sur l'avancée du circuit",
            position: 'right'
        });
        if($.trim($("#cross").html()) !== '') {
            this.intro.addStep({
                element: '#cross',
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