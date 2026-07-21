export class PdfFormManager {

    constructor(viewer) {
        this.viewer = viewer;
        // Keep the historical PdfViewer `this` inside extracted form methods.
        return new Proxy(this, {
            get(target, prop) {
                const value = target[prop];
                if (typeof value === 'function' && prop !== 'constructor') {
                    return value.bind(viewer);
                }
                return value;
            }
        });
    }

    listenToSearchCompletion() {
        let controller = new AbortController();
        let signal = controller.signal;
        console.info("listen to search autocompletion");
        $(".search-completion").each(function () {
            const $input = $(this);
            if ($input.data("esAutocompleteBound") === true) {
                return;
            }
            let serviceName = $input.attr("search-completion-service-name");
            let searchType = $input.attr("search-completion-type");
            let searchReturn = $input.attr("search-completion-return");
            $input.autocomplete({
                delay: 500,
                source: function( request, response ) {
                    if(request.term.length > 2) {
                        controller.abort();
                        controller = new AbortController()
                        signal = controller.signal;
                        $.ajax({
                            url: "/user/users/search-extvalue?searchType=" + searchType + "&searchString=" + request.term + "&serviceName=" + serviceName + "&searchReturn=" + searchReturn,
                            dataType: "json",
                            signal: signal,
                            data: {
                                q: request.term
                            },
                            success: function (data) {
                                console.debug("debug - " + "search user " + request.term);
                                response($.map(data, function (item) {
                                    return {
                                        label: item.text,
                                        value: item.value
                                    };
                                }));
                            }
                        });
                    }
                }
            });
            $input.data("esAutocompleteBound", true);
        });
    }

    annotationLinkTargetBlank() {
        $('.linkAnnotation').each(function (){
            const $linkAnnotation = $(this);
            $linkAnnotation.children().attr('target', '_blank');
            if ($linkAnnotation.data('esDroppableBound') === true) {
                return;
            }
            $linkAnnotation.droppable({
                tolerance: "touch",
                drop: function( event, ui ) {
                    if($(ui.draggable).attr("id") != null && ($(ui.draggable).attr("id").includes("cross_") || $($(ui.draggable).attr("id").includes("border_")))) {
                        $("#border_" + $(ui.draggable).attr("id").split("_")[1]).addClass("cross-warning");
                    }
                },
                over: function( event, ui ) {
                    if($(ui.draggable).attr("id") != null && ($(ui.draggable).attr("id").includes("cross_") || $($(ui.draggable).attr("id").includes("border_")))) {
                        $("#border_" + $(ui.draggable).attr("id").split("_")[1]).addClass("cross-warning");
                    }
                },
                out: function( event, ui ) {
                    if($(ui.draggable).attr("id") != null && ($(ui.draggable).attr("id").includes("cross_") || $($(ui.draggable).attr("id").includes("border_")))) {
                        $("#border_" + $(ui.draggable).attr("id").split("_")[1]).removeClass("cross-warning");
                    }
                }
            });
            $linkAnnotation.data('esDroppableBound', true);
        });
    }

    annotationLinkRemove() {
        $('.linkAnnotation').each(function (){
            $(this).css("opacity", 0);
            $(this).click(function(e) {
                e.preventDefault();
            });
        });
    }

    promiseRenderForm(isField, page) {
        return page.getAnnotations().then(items => {
            this.renderPdfFormWithFields(items);
            return "Réussite";
        });
    }

    async promiseSaveValues() {
        console.log("save");
        console.info("launch save values");
        const tasks = [];
        for (let i = 1; i < this.pdfDoc.numPages + 1; i++) {
            tasks.push(this.pdfDoc.getPage(i).then(page => page.getAnnotations().then(items => this.saveValues(items))));
        }
        await Promise.all(tasks);
    }

    saveValues(items) {
        console.log("saving " + items.length + " fields");
        if(this.dataFields.length > 0) {
            for (let i = 0; i < this.dataFields.length; i++) {
                let dataField = this.dataFields[i];
                let item = items.filter(function (e) {
                    return e.fieldName != null && e.fieldName === dataField.name
                })[0];
                if (item != null && item.fieldName != null) {
                    this.saveValue(item);
                } else {
                    if(this.savedFields.get(dataField.name) == null) {
                        this.savedFields.set(dataField.name, dataField.defaultValue);
                    }
                }
            }
        } else {
            for (let i = 0; i < items.length; i++) {
                this.saveValue(items[i]);
            }
        }
    }

    saveValue(item) {
        if(item != null && item.fieldName != null) {
            let inputName = item.fieldName;
            let inputField = $("[name='" + $.escapeSelector(inputName) + "']");
            if (inputField.length > 0) {
                if (inputField.val() != null) {
                    if (inputField.is(':checkbox')) {
                        if (!inputField[0].checked) {
                            this.savedFields.set(item.fieldName, 'off');
                        } else {
                            this.savedFields.set(item.fieldName, 'on');
                        }
                        return;
                    }
                    if (inputField.is(':radio')) {
                        let radio = $('input[name=\'' + inputField.attr("name") + '\']');
                        let self = this;
                        radio.each(function() {
                            if ($(this).prop("checked")) {
                                self.savedFields.set(item.fieldName, $(this).val());
                            }
                        });
                        return;
                    }
                    if (inputField.is('select')) {
                        let value = inputField.val();
                        this.savedFields.set(item.fieldName, value);
                        return;
                    }
                    let value = inputField.val();
                    this.savedFields.set(item.fieldName, value);
                }
            }
        }
    }

    async promiseRestoreValue() {
        if(this.savedFields.size === 0) {
            await this.promiseSaveValues();
        }
        const tasks = [];
        for(let i = 1; i < this.pdfDoc.numPages + 1; i++) {
            tasks.push(this.pdfDoc.getPage(i).then(page => page.getAnnotations().then(items => this.restoreValues(items))));
        }
        await Promise.all(tasks);
        this.fireEvent("render", ['end']);
    }

    restoreValues(items) {
        console.log("set fields " + items.length);
        for (let i = 0; i < items.length; i++) {
            if(items[i].fieldName != null) {
                let inputName = items[i].fieldName.split(/\$|#|!/)[0];
                let savedValue = this.savedFields.get(items[i].fieldName);
                let inputField = $('[name="' + inputName + '"]');
                if (inputField.val() != null) {
                    if(savedValue != null) {
                        if (inputField.is(':checkbox')) {
                            if (savedValue === 'on') {
                                inputField.prop("checked", true);
                            } else {
                                inputField.prop("checked", false);
                            }
                            continue;
                        }
                        if (inputField.is(':radio')) {
                            let radio = $('input[name=\'' + inputName + '\'][value=\'' + items[i].buttonValue + '\']');
                            if (savedValue === radio.val()) {
                                radio.prop("checked", true);
                            }
                            continue;
                        }
                        inputField.val(savedValue);
                        continue;
                    }
                }
                let textareaField = $('textarea[name=\'' + inputName + '\']');
                if (textareaField.val() != null) {
                    if (savedValue != null) {
                        textareaField.val(savedValue);
                        continue;
                    }
                }
                if (inputField.is('select')) {
                    $("#" + inputName + " option[value='" + savedValue + "']").prop('selected', true);
                    inputField.val(savedValue);
                    continue;
                }
                let selectField = $('select[name=\'' + inputName + '\']');
                if (selectField.val() != null) {
                    let savedFields = this.savedFields;
                    $('#' + inputName + ' option').each(function() {
                        let fieldName = items[i].fieldName;
                        let value = $(this).val();
                        if(savedFields.get(fieldName) === value) {
                            $(this).prop("selected", true);
                        }
                    });
                }
            }
        }
    }

    renderPdfFormWithFields(items) {
        let self = this;
        let datePickerIndex = 40;
        console.debug("debug - " + "rending pdfForm items");
        let signFieldNumber = 0;
        for (let i = 0; i < items.length; i++) {
            if(items[i].fieldType === undefined) {
                if(items[i].title && items[i].title.toLowerCase().includes('sign')) {
                    signFieldNumber = signFieldNumber + 1;
                    $('.popupWrapper').remove();
                }
                continue;
            }
            let inputName = items[i].fieldName.split(/\$|#|!/)[0];
            let dataField;
            if(this.dataFields != null && items[i].fieldName != null) {
                dataField = this.dataFields.filter(obj => {
                    return obj.name === inputName
                })[0];
            }
            let canvasField = $('section[data-annotation-id=' + items[i].id + '] > canvas');
            if (canvasField.length) {
                canvasField.remove();
            }
            let inputField = $('section[data-annotation-id=' + items[i].id + '] > input');
            if (inputField.length) {
                inputField.addClass("field-type-text");
                inputField.on('input', function (e) {
                    clearTimeout(self.timer);
                    self.timer = setTimeout(e => self.fireEvent("change", ['checked']), 500);
                });
                inputField.removeAttr("hidden");
                if (dataField == null) continue;
                this.disableInput(inputField, dataField, items[i].readOnly);
                if (this.disableAllFields) continue;
                let section = $('section[data-annotation-id=' + items[i].id + ']');
                inputField.attr('name', inputName);
                inputField.attr('placeholder', " ");
                inputField.removeAttr("maxlength");
                inputField.attr('id', inputName);
                inputField.attr('title', dataField.description);
                if (dataField.favorisable && !$("#div_" + inputField.attr('id')).length) {
                    let sendField = inputField;
                    $.ajax({
                        type: "GET",
                        url: '/ws-secure/ui/favorites/fields/' + dataField.id,
                        success: response => this.autocomplete(response, sendField)
                    });
                }
                if (dataField.editable) {
                    inputField.val(items[i].fieldValue);
                    if (dataField.defaultValue != null) {
                        inputField.val(dataField.defaultValue);
                    }
                    this.enableInputField(inputField, dataField)
                } else {
                    inputField.val(items[i].fieldValue);
                }

                if (dataField.searchServiceName) {
                    inputField.addClass("search-completion");
                    inputField.attr("search-completion-service-name", dataField.searchServiceName);
                    inputField.attr("search-completion-return", dataField.searchReturn);
                    inputField.attr("search-completion-type", dataField.searchType);
                }

                if (dataField.type === "number") {
                    inputField.get(0).type = "number";
                }

                if (dataField.type === "link") {
                    this.ensureLinkFieldStyles();
                    inputField.addClass("field-type-link pdf-link-input");

                    const currentValue = (items[i].fieldValue && items[i].fieldValue.length)
                        ? items[i].fieldValue
                        : (dataField.defaultValue || '');

                    const testerId = inputName + "_test_btn";
                    const disabledLinkId = inputName + "_link_btn";
                    section.css('overflow', 'visible');
                    section.find('#' + testerId + ', #' + disabledLinkId).remove();

                    if (this.isFieldEnable(dataField)) {
                        inputField.attr('type', 'url');
                        inputField.attr('inputmode', 'url');
                        inputField.attr('placeholder', 'https://exemple.org');
                        inputField.show();
                        if (currentValue) {
                            inputField.val(currentValue);
                        }

                        const $tester = $('<button type="button" id="' + testerId + '" class="pdf-link-test-btn" disabled>Tester</button>');
                        section.append($tester);

                        const getLiveInputField = () => {
                            const $liveInput = $('section[data-annotation-id=' + items[i].id + '] > input[name="' + inputName + '"]');
                            if ($liveInput.length) {
                                return $liveInput;
                            }
                            return $('section[data-annotation-id=' + items[i].id + '] > input').first();
                        };

                        const updateLinkState = () => {
                            const $liveInput = getLiveInputField();
                            const value = ($liveInput.val() || '').trim();
                            const element = $liveInput.get(0);
                            const normalizedValue = this.normalizeLinkValue(value);
                            const currentState = this.linkValidationStates.get(inputName);

                            if (!value) {
                                this.clearLinkReachabilityCheck(inputName);
                                this.linkValidationStates.set(inputName, {
                                    status: 'empty',
                                    value: '',
                                    normalizedValue: ''
                                });
                                $tester.prop('disabled', true);
                                $tester.data('href', '');
                                $liveInput.removeClass('pdf-link-invalid pdf-link-valid');
                                if (element && typeof element.setCustomValidity === 'function') {
                                    element.setCustomValidity('');
                                }
                                return;
                            }

                            if (!this.isValidLinkValue(value)) {
                                this.clearLinkReachabilityCheck(inputName);
                                this.linkValidationStates.set(inputName, {
                                    status: 'format-invalid',
                                    value: value,
                                    normalizedValue: ''
                                });
                                $liveInput.addClass('pdf-link-invalid');
                                $liveInput.removeClass('pdf-link-valid');
                                $tester.prop('disabled', true);
                                $tester.data('href', '');
                                if (element && typeof element.setCustomValidity === 'function') {
                                    element.setCustomValidity('URL invalide');
                                }
                                return;
                            }

                            if (currentState && currentState.normalizedValue === normalizedValue) {
                                if (currentState.status === 'reachable') {
                                    $tester.prop('disabled', false);
                                    $tester.data('href', currentState.normalizedValue || '');
                                    $liveInput.removeClass('pdf-link-invalid').addClass('pdf-link-valid');
                                    if (element && typeof element.setCustomValidity === 'function') {
                                        element.setCustomValidity('');
                                    }
                                    return;
                                }
                                if (currentState.status === 'checking') {
                                    $tester.prop('disabled', true);
                                    $tester.data('href', '');
                                    $liveInput.removeClass('pdf-link-invalid pdf-link-valid');
                                    if (element && typeof element.setCustomValidity === 'function') {
                                        element.setCustomValidity('Vérification du lien en cours');
                                    }
                                    return;
                                }
                                if (currentState.status === 'unreachable') {
                                    $tester.prop('disabled', true);
                                    $tester.data('href', '');
                                    $liveInput.addClass('pdf-link-invalid').removeClass('pdf-link-valid');
                                    if (element && typeof element.setCustomValidity === 'function') {
                                        element.setCustomValidity('Lien inaccessible');
                                    }
                                    return;
                                }
                            }

                            this.scheduleLinkReachabilityCheck(inputName, value, (state) => {
                                const $currentInput = getLiveInputField();
                                const currentElement = $currentInput.get(0);

                                if (state.status === 'checking') {
                                    $tester.prop('disabled', true);
                                    $tester.data('href', '');
                                    $currentInput.removeClass('pdf-link-invalid pdf-link-valid');
                                    if (currentElement && typeof currentElement.setCustomValidity === 'function') {
                                        currentElement.setCustomValidity('Vérification du lien en cours');
                                    }
                                    return;
                                }

                                if (state.status === 'reachable') {
                                    $tester.prop('disabled', false);
                                    $tester.data('href', state.normalizedValue || '');
                                    $currentInput.removeClass('pdf-link-invalid').addClass('pdf-link-valid');
                                    if (currentElement && typeof currentElement.setCustomValidity === 'function') {
                                        currentElement.setCustomValidity('');
                                    }
                                    return;
                                }

                                $tester.prop('disabled', true);
                                $tester.data('href', '');
                                $currentInput.addClass('pdf-link-invalid').removeClass('pdf-link-valid');
                                if (currentElement && typeof currentElement.setCustomValidity === 'function') {
                                    currentElement.setCustomValidity('Lien inaccessible');
                                }
                            });
                        };

                        section.off('.pdf_link_' + items[i].id);
                        section.on('input.pdf_link_' + items[i].id + ' keyup.pdf_link_' + items[i].id + ' change.pdf_link_' + items[i].id + ' blur.pdf_link_' + items[i].id, 'input[name="' + inputName + '"]', () => {
                            updateLinkState();
                        });

                        $tester.off('click.pdf_link');
                        $tester.off('mousedown.pdf_link');
                        $tester.on('mousedown.pdf_link', (e) => {
                            e.preventDefault();
                        });
                        $tester.on('click.pdf_link', (e) => {
                            e.preventDefault();
                            const href = $tester.data('href');
                            if (!href) {
                                return;
                            }
                            window.open(href, '_blank', 'noopener,noreferrer');
                        });

                        updateLinkState();
                    } else {
                        inputField.attr('type', 'url');
                        inputField.attr('inputmode', 'url');
                        inputField.show();
                        inputField.prop('readonly', true);
                        inputField.prop('disabled', false);
                        inputField.removeClass('disabled-field disable-selection');
                        inputField.parent().removeClass('disable-div-selection');
                        inputField.addClass('pdf-link-disabled-input');
                        const rawValue = (currentValue || '').trim();
                        const valid = this.isValidLinkValue(rawValue);
                        const href = valid ? this.normalizeLinkValue(rawValue) : '#';
                        const label = valid ? 'Ouvrir le lien' : 'Lien indisponible';
                        const $linkButton = $('<a id="' + disabledLinkId + '" class="pdf-link-display-btn" target="_blank" rel="noopener noreferrer"></a>');

                        $linkButton.attr('href', href);
                        $linkButton.text(label);

                        if (!valid) {
                            $linkButton.addClass('is-disabled');
                            $linkButton.on('click', (e) => e.preventDefault());
                        }

                        inputField.off('click.pdf_link_disabled keydown.pdf_link_disabled');
                        if (valid) {
                            inputField.attr('title', rawValue);
                            inputField.on('click.pdf_link_disabled', (e) => {
                                e.preventDefault();
                                window.open(href, '_blank', 'noopener,noreferrer');
                            });
                            inputField.on('keydown.pdf_link_disabled', (e) => {
                                if (e.key === 'Enter' || e.key === ' ') {
                                    e.preventDefault();
                                    window.open(href, '_blank', 'noopener,noreferrer');
                                }
                            });
                        }

                        section.append($linkButton);
                    }

                    if (this.isFieldEnable(dataField)) {
                        if (dataField.required) {
                            inputField.prop('required', true);
                            inputField.addClass('required-field');
                        } else {
                            inputField.prop('required', false);
                            inputField.removeClass('required-field');
                        }
                    } else {
                        inputField.prop('required', false);
                    }
                }

                if (dataField.type === "radio") {
                    inputField.addClass("field-type-radio");
                    if (this.isFieldEnable(dataField)) {
                        if (dataField.required) {
                            inputField.parent().addClass('required-field');
                        }
                    }
                    inputField.val(items[i].buttonValue);
                    inputField.attr("id", dataField.name + items[i].buttonValue);
                    if (dataField.defaultValue === items[i].buttonValue) {
                        inputField.attr("checked", "checked");
                        inputField.prop("checked", true);
                    }
                    inputField.unbind();
                    inputField.on('click', e => this.fireEvent("change", ['checked']));
                }
                if (dataField.type === 'checkbox') {
                    inputField.addClass("field-type-checkbox");
                    inputField.val('on');
                    if (dataField.defaultValue === 'on') {
                        inputField.attr("checked", "checked");
                        inputField.prop("checked", true);
                    }
                    inputField.unbind();
                    inputField.on('click', e => this.fireEvent("change", ['checked']));
                }

                if (dataField.type === "date") {
                    datePickerIndex--;
                    const inputElement = inputField[0];

                    const picker = new tempusDominus.TempusDominus(inputElement, {
                        localization: {
                            today: 'Aller à aujourd\'hui',
                            clear: 'Effacer la sélection',
                            close: 'Fermer le sélecteur',
                            selectMonth: 'Sélectionner le mois',
                            previousMonth: 'Mois précédent',
                            nextMonth: 'Mois suivant',
                            selectYear: 'Sélectionner l\'année',
                            previousYear: 'Année précédente',
                            nextYear: 'Année suivante',
                            selectDecade: 'Sélectionner la décennie',
                            previousDecade: 'Décennie précédente',
                            nextDecade: 'Décennie suivante',
                            previousCentury: 'Siècle précédent',
                            nextCentury: 'Siècle suivant',
                            pickHour: 'Choisir l\'heure',
                            incrementHour: 'Augmenter l\'heure',
                            decrementHour: 'Diminuer l\'heure',
                            pickMinute: 'Choisir les minutes',
                            incrementMinute: 'Augmenter les minutes',
                            decrementMinute: 'Diminuer les minutes',
                            pickSecond: 'Choisir les secondes',
                            incrementSecond: 'Augmenter les secondes',
                            decrementSecond: 'Diminuer les secondes',
                            toggleMeridiem: 'Basculer AM/PM',
                            selectTime: 'Sélectionner l\'heure',
                            selectDate: 'Sélectionner la date',
                            locale: 'fr',
                            startOfTheWeek: 1,
                            format: 'dd/MM/yyyy',
                            toggleAriaLabel: 'Modifier la date',
                        },
                        display: {
                            icons: {
                                time: 'fi fi-rr-clock',
                                date: 'fi fi-rr-calendar-day',
                                up: 'fi fi-rr-angle-small-up',
                                down: 'fi fi-rr-angle-small-down',
                                previous: 'fi fi-rr-angle-small-left',
                                next: 'fi fi-rr-angle-small-right',
                                today: 'fi fi-rr-calendar-check',
                                clear: 'fi fi-rr-empty-set',
                                close: 'fi fi-rr-check'
                            },
                            components: {
                                calendar: true,
                                date: true,
                                month: true,
                                year: true,
                                decades: false,
                                clock: false,
                                hours: false,
                                minutes: false,
                                seconds: false
                            },
                            toolbarPlacement: 'bottom',
                            buttons: {
                                today: true,
                                clear: true,
                                close: true
                            }
                        }
                    });

                    inputField.on("focus", function () {
                        section.css("z-index", datePickerIndex + 2000);
                    });
                    inputField.on("focusout", function () {
                        section.css("z-index", 4);
                    });

                    inputElement.addEventListener('change', (e) => {
                        this.fireEvent("change", ['date']);
                    });
                }

                if (dataField.type === "time") {
                    datePickerIndex--;
                    const inputElement = inputField[0];

                    const picker = new tempusDominus.TempusDominus(inputElement, {
                        localization: {
                            locale: 'fr',
                            format: 'HH:mm',
                        },
                        stepping: 5,
                        display: {
                            viewMode: 'clock',
                            icons: {
                                time: 'fi fi-rr-clock',
                                date: 'fi fi-rr-calendar-day',
                                up: 'fi fi-rr-angle-small-up',
                                down: 'fi fi-rr-angle-small-down',
                                previous: 'fi fi-rr-angle-small-left',
                                next: 'fi fi-rr-angle-small-right',
                                today: 'fi fi-rr-calendar-check',
                                clear: 'fi fi-rr-trash',
                                close: 'fi fi-rr-check'
                            },
                            components: {
                                calendar: false,
                                date: false,
                                month: false,
                                year: false,
                                decades: false,
                                clock: true,
                                hours: true,
                                minutes: true,
                                seconds: false
                            },
                            toolbarPlacement: 'bottom',
                            buttons: {
                                today: true,
                                clear: true,
                                close: true
                            }
                        }
                    });

                    inputField.on("focus", function () {
                        section.css("z-index", datePickerIndex + 2000);
                    });
                    inputField.on("focusout", function () {
                        section.css("z-index", datePickerIndex);
                    });

                    inputElement.addEventListener('change', (e) => {
                        this.fireEvent("change", ['time']);
                    });
                }
            }

            inputField = $('section[data-annotation-id=' + items[i].id + '] > textarea');
            if (inputField.length) {
                inputField.addClass("field-type-textarea");
                inputField.on('input', function(e) {
                    clearTimeout(self.timer);
                    self.timer = setTimeout(e => self.fireEvent("change", ['checked']), 500);
                });
                inputField.removeAttr("hidden");
                if(dataField == null) continue;
                this.disableInput(inputField, dataField, items[i].readOnly);
                if(this.disableAllFields) continue;
                let sendField = inputField;
                if (dataField.favorisable) {
                    $.ajax({
                        type: "GET",
                        url: '/ws-secure/ui/favorites/fields/' + dataField.id,
                        success: response => this.autocomplete(response, sendField)
                    });
                }
                inputField.attr('name', inputName);
                inputField.attr('placeholder', " ");
                inputField.removeAttr("maxlength");
                inputField.attr('id', inputName);
                if (this.isFieldEnable(dataField)) {
                    if(dataField.defaultValue != null) {
                        inputField.val(dataField.defaultValue);
                    }
                    this.enableInputField(inputField, dataField)
                }
            }

            inputField = $('section[data-annotation-id=' + items[i].id + '] > select');
            if (inputField.length) {
                inputField.addClass("field-type-select");
                inputField.on('change', e => this.fireEvent("change", ['checked']));
                if(dataField == null) continue;
                this.disableInput(inputField, dataField, items[i].readOnly);
                inputField.removeAttr("hidden");
                if(this.disableAllFields) continue;
                inputField.removeAttr('size');
                inputField.attr('name', inputName);
                inputField.attr('id', inputName);
                if (dataField.editable) {
                    inputField.val(dataField.defaultValue);
                    this.enableInputField(inputField, dataField)
                }
            }
        }
        console.debug("debug - " + ">>End compute field");
        $(".annotationLayer").each(function() {
            $(this).removeClass("d-none");
        });
        this.listenToSearchCompletion();
    }

    isFieldEnable(dataField) {
        return dataField.editable && !dataField.readOnly;
    }

    enableInputField(inputField, dataField) {
        if (!dataField.required) {
            inputField.prop('required', false);
            inputField.removeClass('required-field');
        } else {
            inputField.prop('required', true);
            inputField.addClass('required-field');
        }
        if (!dataField.readOnly) {
            inputField.prop('disabled', false);
            inputField.removeClass('disabled-field disable-selection');
        }
        inputField.attr('title', dataField.description);
    }

    disableInput(inputField, dataField, readOnly) {
        if (readOnly || dataField == null || dataField.readOnly || this.disableAllFields || !this.isFieldEnable(dataField)) {
            inputField.addClass('disabled-field disable-selection');
            inputField.prop('disabled', true);
            inputField.prop('required', false);
            inputField.parent().addClass('disable-div-selection');
        }
    }

    autocomplete(response, inputField) {
        let id = inputField.attr('id');
        let div = "<div class='custom-autocompletion' id='div_" + id +"'></div>";
        $(div).insertAfter(inputField);
        inputField.autocomplete({
            delay: 500,
            source: response,
            appendTo: "#div_" + id,
            minLength:0
        }).bind('focus', function(){ $(this).autocomplete("search"); } );
    }

    checkForm() {
        return new Promise((resolve, reject) => {
            let formData = new Map();
            console.info("check data name");
            let self = this;
            let resolveOk = "ok";
            let warningFields = [];
            $(self.dataFields).each(function (e, item) {
                let savedField = self.savedFields.get(item.name)
                formData[item.name] = savedField;
                item.validationError = null;
                const isMissingRequiredValue = item.required && self.isFieldEnable(item) &&
                    (!savedField || (savedField === "off" && item.type === "checkbox"));
                const isInvalidLinkValue = item.type === "link" && self.isFieldEnable(item)
                    && !!savedField && !self.isValidLinkValue(savedField);
                const linkValidationState = item.type === "link"
                    ? self.linkValidationStates.get(item.name)
                    : null;
                const isUnverifiedOrUnreachableLink = item.type === "link" && self.isFieldEnable(item)
                    && !!savedField
                    && self.isValidLinkValue(savedField)
                    && (!linkValidationState || !['reachable'].includes(linkValidationState.status));

                if (isMissingRequiredValue || isInvalidLinkValue || isUnverifiedOrUnreachableLink) {
                    if (isInvalidLinkValue) {
                        item.validationError = 'invalid_link';
                    } else if (isUnverifiedOrUnreachableLink) {
                        item.validationError = linkValidationState?.status === 'checking'
                            ? 'checking_link'
                            : 'unreachable_link';
                    } else {
                        item.validationError = 'required';
                    }
                    let addWarning = true;
                    for(let i = 0; i < warningFields.length; i++) {
                        if(warningFields[i].name === item.name) {
                            addWarning = false;
                        }
                    }
                    if(addWarning) {
                        warningFields.push($(this)[0]);
                    }
                }
            });
            if (warningFields.length > 0) {
                warningFields.sort((a, b) => a.compareByPage(b))
                let text = "Certain champs requis n'ont pas été remplis dans ce formulaire<ul>";
                if (warningFields.length < 2 && warningFields[0].name != null) {
                    const fieldLabel = warningFields[0].description != null && warningFields[0].description !== ""
                        ? warningFields[0].description
                        : warningFields[0].name;
                    if (warningFields[0].validationError === 'invalid_link') {
                        text = "Le champ " + fieldLabel + " contient une URL invalide";
                    } else if (warningFields[0].validationError === 'checking_link') {
                        text = "Le champ " + fieldLabel + " est encore en cours de vérification";
                    } else if (warningFields[0].validationError === 'unreachable_link') {
                        text = "Le champ " + fieldLabel + " contient un lien inaccessible";
                    } else {
                        text = "Le champ " + fieldLabel + " n'est pas rempli";
                    }
                    if (warningFields[0].page != null) {
                        text += " en page " + warningFields[0].page;
                    }
                } else {
                    warningFields.forEach(function (field) {
                        let suffix = '';
                        if (field.validationError === 'invalid_link') {
                            suffix = ' : URL invalide';
                        } else if (field.validationError === 'checking_link') {
                            suffix = ' : vérification en cours';
                        } else if (field.validationError === 'unreachable_link') {
                            suffix = ' : lien inaccessible';
                        }
                        if (field.description != null && field.description !== "") {
                            text += "<li>" + field.description + suffix;
                            if(field.page != null) {
                                text += " (en page " + (field.page + 1) + ")";
                            }
                            text +="</li>";
                        } else {
                            text += "<li>" + field.name + suffix;
                            if(field.page != null) {
                                text += " (en page " + (field.page + 1) + ")";
                            }
                            text +="</li>";
                        }
                    });
                }
                text += "</ul>"
                bootbox.alert(text, function () {
                    let field = $('#' + warningFields[0].name);
                    setTimeout(function () {
                        self.focusField(field)
                    }, 100);
                });
                resolveOk = $(this)[0].name;
                $('#sendModal').modal('hide');
            }
            resolve(resolveOk);
        });
    }

}
