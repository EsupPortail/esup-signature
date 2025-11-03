export class Home2Help {

    constructor(doneTour) {
        this.doneTour = doneTour;
        this.intro = introJs();
        this.intro.setOptions({nextLabel: 'Suivant', prevLabel: 'Précédent', doneLabel: 'Terminer', skipLabel: 'x', showStepNumbers: 'false', overlayOpacity: 0.3})
        this.initListeners();
        this.initStep();
    }

    initListeners() {
        this.intro.onbeforechange(e => this.scrollTop(e));
        this.intro.onafterchange(e => this.modButtons());
        this.intro.onexit(function () {
            $.get("/user/users/mark-intro-as-read/home2Help");
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
            element: 'main',
            intro: "<h5>Bienvenue sur le nouvel accueil d'Esup-signature</h5>" +
                "<p>Pour votre première visite, ce tutoriel va vous présenter la nouvelle page d'accueil. Vous pourrez le relancer à l'aide du bouton d'aide de la barre de navigation</p>" +
                "<a href='https://www.esup-portail.org/wiki/spaces/SIGN/overview' target='_blank'>Cliquez ici pour plus d'information sur l'outil</a><br><br>",
            highlightClass: 'intro-js-custom-highlight',
            position: 'auto'
        });
        if($.trim($("#navbar-buttons").html()) !== '') {
            this.intro.addStep({
                element: '#navbar-buttons',
                intro:
                    "<h5>Voici les principaux éléments de navigation</h5>" +
                    "<ul>" +
                    "<li>\"Accueil\" : pour revenir sur cette page à tout moment.</li>" +
                    "<li>\"Tableau de bord\" : la liste des demandes à signer, ou en cours de signature.</li>" +
                    "<li>\"Autorisations\" : rediriger vos demandes ou mettre en place une délégation de signature.</li>" +
                    "</ul>",
                highlightClass: 'intro-js-custom-highlight',
                position: 'auto'
            });
        }
        if($.trim($("#user-buttons").html()) !== '') {

            this.intro.addStep({
                element: '#user-buttons',
                intro: "<h5>Bouton aide et profil</h5>Vous pouvez accéder à votre profil pour insérer vos images de signature et modifier vos paramètres en cliquant sur votre nom en haut à droite, puis sur 'Modifier mes paramètres'.",
                position: 'left'
            });
        }
        if($.trim($("#new-self-sign").html()) !== '') {
            this.intro.addStep({
                element: '#new-self-sign',
                intro: "Cette tuile vous permet de démarrer la signature d'un document présent sur votre poste de travail.",
                position: 'right'
            });
        }
        if($.trim($("#new-fast-sign").html()) !== '') {
            this.intro.addStep({
                element: '#new-fast-sign',
                intro: "<p>Utilisez la demande de signature pour faire signer le document à quelqu'un.</p>" +
                    "<p>Vous pourrez choisir des destinataires internes ou externes et choisir le niveau de signature (signature simple par défaut)</p>",
                position: 'right'
            });
        }
        if($.trim($("#start-wizard-custom-button").html()) !== '') {
            this.intro.addStep({
                element: '#start-wizard-custom-button',
                intro: "Les demandes personnalisées permettent de créer des circuits à plusieurs étapes.",
                position: 'right'
            });
        }
        if($.trim($("#all-modal-button").html()) !== '') {
            this.intro.addStep({
                element: '#all-modal-button',
                intro: "Ici, afficher la liste des procédures (circuits et formulaires) disponibles pour vous. Vous pourrez ajouter vos préférés en favoris.",
                position: 'right'
            });
        }
        if($.trim($("#newSignDemand").html()) !== '') {
            this.intro.addStep({
                element: '#newSignDemand',
                intro: "La demande personnalisée permet de créer un circuit à plusieurs étapes. Ce circuit pourra être sauvegardé pour être réutiliser a posteriori",
                position: 'right'
            });
        }
        if($.trim($("#shortcuts").html()) !== '') {
            this.intro.addStep({
                element: '#shortcuts',
                intro: "Retrouvez les principaux raccourcis : " +
                    "<ul>" +
                    "<li>Vos demandes (signées, à signer et envoyées)</li>" +
                    "<li>L’outil de contrôle des signatures</li>" +
                    "<li>L’assistant de création de circuits réutilisables</li>" +
                    "</ul>",
                position: 'right'
            });
        }
        if($.trim($("#to-sign-list").html()) !== '') {
            this.intro.addStep({
                element: '#to-sign-list',
                intro: "Retrouvez d'un coup d’œil vos demandes à signer, ici.",
                position: 'right'
            });
        }
        if($.trim($("#myFavorites").html()) !== '') {
            this.intro.addStep({
                element: '#myFavorites',
                intro: "Vous pouvez ajouter les circuits et formulaires les plus souvent utilisés ici. Pour cela, parcourez toutes les procédures et cliquez sur l'étoile ! <i style=\"font-family: 'Font Awesome 5 Free'\" class=\"fa-solid fa-star text-warning\"></i>",
                position: 'right'
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
