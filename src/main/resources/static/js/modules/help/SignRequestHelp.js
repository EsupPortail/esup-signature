export class SignRequestHelp {

    constructor(doneTour, isOtp) {
        this.doneTour = doneTour;
        this.isOtp = isOtp;
        this.intro = introJs();
        this.intro.setOptions({nextLabel: 'Suivant', prevLabel: 'Précédent', doneLabel: 'Terminer', skipLabel: '<i class="fi fi-rr-cross"></i>', showStepNumbers: 'false', overlayOpacity: 0.3})
        this.initListeners();
        this.initStep();
    }

    initListeners() {
        this.intro.onbeforechange(() => this.scrollTop());
        this.intro.onafterchange(() => this.modButtons());
        let self = this;
        this.intro.onexit(function () {
            if(self.isOtp) {
                $.get("/otp/users/mark-intro-as-read/signRequestHelp");
            } else {
                $.get("/user/users/mark-intro-as-read/signRequestHelp");
            }
        });
        $("#helpStartButton").on('click', () => this.start());
    }

    initStep() {

        this.intro.addStep({
            intro: "Cette interface vous permet de :" +
                "<ul>" +
                "<li>Consulter le document que vous devez signer</li>" +
                "<li>Suivre l’état de la demande et les étapes du circuit</li>" +
                "<li>Préparer puis valider votre signature ou votre visa selon votre rôle</li>" +
                "<li>Ajouter des annotations, post-it ou pièces jointes selon vos droits</li>" +
                "</ul>"
        });

        this.addStepIfElementExists('#tools', {
            intro: "Cette zone regroupe les actions principales sur la demande :" +
                "<ul>" +
                "<li>choisir le mode de signature</li>" +
                "<li>insérer votre signature dans le document PDF</li>" +
                "<li>valider, signer ou refuser la demande</li>" +
                "<li>télécharger le document signé en fin de circuit</li>",
            position: 'auto'
        });
        this.addStepIfElementExists(['#sign-tools', '[id^="crossTools_"]'], {
            intro: "Quand une signature est sélectionnée, ces options permettent de personnaliser son rendu :" +
                "<ul>" +
                "<li>afficher ou masquer les textes complémentaires</li>" +
                "<li>choisir la position des mentions autour de la signature</li>" +
                "<li>afficher le type de signature, le nom, la date ou un texte libre</li>" +
                "<li>activer selon le cas un filigrane</li>" +
                "</ul>",
            position: 'bottom'
        });
        this.addStepIfElementExists('#sidebar', {
            intro: "La barre latérale vous donne une vue d’ensemble de la demande : statut, créateur, suivi, commentaires éventuels et étapes du circuit.",
            position: 'right'
        });
        this.addStepIfElementExists(['#cross', '[id^="cross_"]'], {
            intro: "Cette zone correspond à votre signature ou à votre visa dans le document. Vous pouvez la déplacer, la redimensionner et ajuster son contenu avant validation.",
            position: 'bottom'
        });
        this.addStepIfElementExists('#signButtons', {
            intro: "Cette colonne regroupe des actions complémentaires. Selon vos droits et l’état de la demande, vous y trouverez par exemple les informations, pièces jointes, post-it, réglages, téléchargements, impression ou suppression.",
            position: 'left'
        });
    }

    addStepIfElementExists(selectors, stepOptions) {
        const element = this.findFirstExistingElement(selectors);
        if (element == null) {
            return;
        }

        this.intro.addStep({
            ...stepOptions,
            element: element
        });
    }

    findFirstExistingElement(selectors) {
        const selectorList = Array.isArray(selectors) ? selectors : [selectors];

        for (const selector of selectorList) {
            const candidates = Array.from(document.querySelectorAll(selector));
            const matchingElement = candidates.find(element => this.isHelpTargetVisible(element));
            if (matchingElement != null) {
                return matchingElement;
            }
        }

        return null;
    }

    isHelpTargetVisible(element) {
        if (element == null) {
            return false;
        }

        if ($(element).hasClass('d-none')) {
            return false;
        }

        return $.trim($(element).html()) !== '';
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

    scrollTop() {
        window.scrollTo(0, 0);
    }

    modButtons() {
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