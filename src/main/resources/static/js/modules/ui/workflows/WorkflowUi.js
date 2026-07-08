import {attachDirtyIndicator} from "../DirtyIndicator.js?version=@version@";

export class WorkflowUi {

    constructor() {
        console.info('Starting workflow UI');
        this.form = null;
        this.isInitialized = false;
        window.workflowUi = this;

        if (document.documentElement.dataset.globalUiReady === 'true') {
            queueMicrotask(() => this.initListeners());
            return;
        }

        document.addEventListener('globalUiReady', () => this.initListeners(), { once: true });
    }

    initListeners() {
        this.initDirtyIndicator();
        $(document).ready(() => this.initDeleteListener());
        $(document).ready(() => this.initMultiSignListener());
        $('#delete-button').on('click', () => this.confirmDelete());
        $('#autoSign').on('change', function () {
            if ($(this).is(':checked')) {
                const id = $(this).attr('data-es-step-id');
                bootbox.confirm('Attention tous les paramètres de cette étape seront perdus après la validation', function (result) {
                    if (result) {
                        $('#cert_' + id).removeClass('d-none');
                        $('#conf_' + id).addClass('d-none');
                        $('#table_' + id).addClass('d-none');
                    } else {
                        $('#autoSign').click();
                    }
                });
            } else {
                const id = $(this).attr('data-es-step-id');
                $('#cert_' + id).addClass('d-none');
                $('#conf_' + id).removeClass('d-none');
                $('#table_' + id).removeClass('d-none');
            }
        });

        const self = this;
        $(document).ready(function () {
            const $signTypeSelects = $('select[id^="signType-"]');

            $signTypeSelects.each(function () {
                const stepId = $(this).attr('id').split('signType-')[1];
                self.toggleVisibility(stepId, $(this).val());
            });

            $signTypeSelects.on('change', function () {
                const stepId = $(this).attr('id').split('signType-')[1];
                self.toggleVisibility(stepId, $(this).val());
            });
        });

        this.manageMinMaxLevels();
    }

    initDirtyIndicator() {
        if (this.isInitialized) {
            return;
        }

        this.form = document.getElementById('updateSignBook')
            || document.getElementById('formUpdate')
            || document.getElementById('params');
        const saveButton = document.getElementById('saveButton');

        if (!this.form || !saveButton) {
            return;
        }

        this.isInitialized = true;

        attachDirtyIndicator({
            form: this.form,
            saveButton
        });
    }

    submitForm() {
        if (!this.form) {
            return true;
        }

        if (this.form.requestSubmit) {
            this.form.requestSubmit();
        } else {
            this.form.submit();
        }

        return false;
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
        const idSuffix = element.id.split('-')[1];
        if (idSuffix === undefined) {
            annotationOption = document.getElementById('singleSignWithAnnotation');
        } else {
            annotationOption = document.getElementById(`singleSignWithAnnotation-${idSuffix}`);
        }
        if (annotationOption) {
            annotationOption.disabled = element.checked;
        }
    }

    confirmDelete() {
        bootbox.confirm('Voulez-vous vraiment supprimer ce circuit ?', function (result) {
            if (result) {
                $('#delete').submit();
            }
        });
    }

    initMultiSignListener() {
        const self = this;
        $('.multi-sign-btn').each(function () {
            $(this).on('click', e => self.toggleAnnotationOption(e.currentTarget));
        });
    }

    initDeleteListener() {
        const self = this;
        $('.del-step-btn').each(function () {
            $(this).on('click', e => self.launchDelete(e));
        });
    }

    launchDelete(e) {
        bootbox.confirm('Voulez-vous vraiment supprimer cette étape ?<br>Tous les liens avec les champs de formulaires seront supprimés', function (result) {
            if (result) {
                $('#del_' + $(e.currentTarget).attr('id')).submit();
            }
        });
    }

    manageMinMaxLevels() {
        const signLevelMap = {
            simple: 2,
            advanced: 3,
            qualified: 4
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

                if ((signLevelMap[maxSelect.value] || 0) < minValueNum) {
                    maxSelect.value = minSelect.value;
                }
            };

            updateMaxOptions();
            minSelect.removeEventListener('change', updateMaxOptions);
            minSelect.addEventListener('change', updateMaxOptions);
        });
    }

}