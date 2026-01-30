export class WorkflowUi {

    constructor() {
        console.info("Starting workflow UI");
        this.initListeners();
    }

    initListeners() {
        $(document).ready(e => this.initDeleteListener());
        $(document).ready(e => this.initMultiSignListener());
        $("#delete-button").on("click", e => this.confirmDelete());
        $("#autoSign").on('change', function(){
           if($(this).is(":checked")) {
               let id = $(this).attr("data-es-step-id");
               bootbox.confirm('Attention tous les paramètres de cette étape seront perdus après la validation', function (result){
                   if(result) {
                       $("#cert_" + id).removeClass("d-none");
                       $("#conf_" + id).addClass("d-none");
                       $("#table_" + id).addClass("d-none");

                   } else {
                       $("#autoSign").click();
                   }
               });
           } else {
               let id = $(this).attr("data-es-step-id");
               $("#cert_" + id).addClass("d-none");
               $("#conf_" + id).removeClass("d-none");
               $("#table_" + id).removeClass("d-none");
           }
        });
        let self = this;
        $(document).ready(function () {

            $('select[id^="signType-"]').each(function () {
                const stepId = $(this).attr('id').split('signType-')[1];
                self.toggleVisibility(stepId, $(this).val());
            });

            $('select[id^="signType-"]').on('change', function () {
                const stepId = $(this).attr('id').split('signType-')[1];
                self.toggleVisibility(stepId, $(this).val());
            });
        });
        this.manageMinMaxLevels();
    }

    toggleVisibility(stepId, selectedVal) {
        const $signTypeDiv = $('#signTypeDiv-' + stepId);
        const $sealVisaDiv = $('#sealVisaDiv-' + stepId);

        if (selectedVal === 'signature') {
            $signTypeDiv.removeClass('d-none');
        } else {
            $signTypeDiv.addClass('d-none');
        }

        if (selectedVal === 'visa') {
            $sealVisaDiv.removeClass('d-none');
        } else {
            $sealVisaDiv.addClass('d-none');
        }
    }

    toggleAnnotationOption(element) {
        let annotationOption;
        let idSuffix = element.id.split('-')[1]; // Récupère le numéro (ex: '1', '2', etc.)
        if(idSuffix === undefined) {
            annotationOption = document.getElementById(`singleSignWithAnnotation`);
        } else {
            annotationOption = document.getElementById(`singleSignWithAnnotation-${idSuffix}`);
        }
        if (annotationOption) {
            annotationOption.disabled = element.checked;
        }
    }

    confirmDelete() {
        bootbox.confirm("Voulez-vous vraiment supprimer ce circuit ?", function (result){
            if(result) {
                $("#delete").submit();
            }
        })
    }

    initMultiSignListener() {
        let self = this;
        $(".multi-sign-btn").each(function(){
            $(this).on("click", e => self.toggleAnnotationOption(e.currentTarget));
        });
    }

    initDeleteListener() {
        let self = this;
        $(".del-step-btn").each(function(){
            $(this).on("click", e => self.launchDelete(e));
        });
    }

    launchDelete(e) {
        bootbox.confirm('Voulez-vous vraiment supprimer cette étape ?<br>Tous les liens avec les champs de formulaires seront supprimés', function(result){
            if(result) {
                $('#del_' + $(e.currentTarget).attr("id")).submit();
            }
        });
    }

    manageMinMaxLevels() {
        const signLevelMap = {
            'simple': 2,
            'advanced': 3,
            'qualified': 4
        };

        document.querySelectorAll('select[id^="minSignLevel-"]').forEach(minSelect => {
            const idSuffix = minSelect.id.replace('minSignLevel-', '');
            const maxSelect = document.getElementById('maxSignLevel-' + idSuffix);
            if (!maxSelect) return;

            const updateMaxOptions = () => {
                const minValueNum = signLevelMap[minSelect.value] || 0;
                Array.from(maxSelect.options).forEach(option => {
                    const optionValueNum = signLevelMap[option.value] || 0;
                    option.disabled = optionValueNum < minValueNum;
                });

                // Ajuste la valeur actuelle si elle est inférieure au min
                if ((signLevelMap[maxSelect.value] || 0) < minValueNum) {
                    maxSelect.value = minSelect.value;
                }
            };

            // Initial update au chargement
            updateMaxOptions();

            // Update au changement
            minSelect.removeEventListener('change', updateMaxOptions);
            minSelect.addEventListener('change', updateMaxOptions);
        });
    }

}