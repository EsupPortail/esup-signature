export class HomeHelp {

    constructor(doneTour) {
        this.doneTour = doneTour;
        this.intro = introJs();
        this.intro.setOptions({nextLabel: 'Suivant', prevLabel: 'Précédent', doneLabel: 'Terminer', skipLabel: 'Passer', showStepNumbers: 'false', overlayOpacity: 1})
        this.initListeners();
        this.initStep();
    }

    initListeners() {
        this.intro.onbeforechange(e => this.scrollTop(e));
        this.intro.onafterchange(e => this.modButtons());
        this.intro.onexit(function () {
            $.get("/user/users/mark-intro-as-read/homeHelp");
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
            intro: "Voici les principaux éléments de navigation : " +
                "<ul>" +
                "<li>Dans les brouillons vous retrouvez les formulaires en cours d'édition.</li>" +
                "<li>Dans le tableau de bord, la liste des demandes à signer, ou en cours de signature.</li>" +
                "<li>Dans \"Outils\", vous pouvez créer vos propres circuits de signature ou vérifier des documents.</li>" +
                "</ul>",
            highlightClass: 'intro-js-custom-highlight',
            position: 'auto'
        });

        this.intro.addStep({
            element: '#user-buttons',
            intro: "Vous pouvez modifier / compléter vos paramètres à tout moment en utilisant le bouton 'paramètres' en haut à droite.",
            position: 'left'
        });
        if($.trim($("#newfastSign").html()) !== '') {
            this.intro.addStep({
                element: '#newfastSign',
                intro: "Ce bouton vous permet de signer un document présent sur votre poste de travail.",
                position: 'right'
            });
        }
        if($.trim($("#newSignDemand").html()) !== '') {
            this.intro.addStep({
                element: '#newSignDemand',
                intro: "Utilisez la demande simple pour faire signer le document à quelqu'un.",
                position: 'right'
            });
        }
        if($.trim($("#newWizard").html()) !== '') {
            this.intro.addStep({
                element: '#newWizard',
                intro: "Ici vous pouvez créer une demande pour laquelle vous définissez un circuit personnalisé.",
                position: 'right'
            });
        }
        if($.trim($("#newWorkflow").html()) !== '') {
            this.intro.addStep({
                element: '#newWorkflow',
                intro: "Les boutons <i class='fas fa-project-diagram fa-2x'></i> permettent de démarrer des circuits personnalisés ou pré-définis.",
                position: 'right'
            });
        }

        if($.trim($("#newForm").html()) !== '') {
            this.intro.addStep({
                element: '#newForm',
                intro: "Les boutons <i class='fas fa-file-alt fa-2x'></i> permettent de remplir un formulaire.",
                position: 'right'
            });
        }

        this.intro.addStep({
            element: '#toSignList',
            intro: "Lorsque vous avez un document à signer, il apparait dans cette liste.",
            position: 'right'
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

    modButtons() {
        $('.introjs-button').each(function(){
            if($(this).hasClass('introjs-disabled')) {
                $(this).removeClass('introjs-disabled');
                $(this).addClass('disabled');
            }
            $(this).removeClass('introjs-button');
            $(this).addClass('btn btn-sm btn-light ml-1 btn-outline-dark');

        });
    }

}
