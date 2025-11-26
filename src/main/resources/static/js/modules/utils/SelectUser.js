export default class SelectUser {

    constructor(selectName, limit, signRequestId, csrf) {
        console.info("init select-user : " + selectName);
        this.selectField = $("#" + selectName);
        this.stepNumber = "1";
        if(selectName.split("-").length > 0) {
            this.stepNumber = selectName.split("-")[1];
        }
        this.globalProperties = JSON.parse(sessionStorage.getItem("globalProperties"));
        this.enableSms = JSON.parse(sessionStorage.getItem("enableSms"));
        this.slimSelect = null;
        this.checkList = this.selectField.attr("data-es-check-list");
        this.signRequestId = signRequestId;
        this.csrf = csrf;
        this.valuePrefix = "";
        this.limit = 99;
        this.flag = false;
        // let selectNameSplit = selectName.split("_");
        // if(selectNameSplit.length === 2) {
        //     this.valuePrefix = selectNameSplit[1] + "*";
        // }
        let defaultValues = [];
        $("#" + selectName + " > option").each(function() {
            if($(this).text() !== "") {
                defaultValues.push({
                    text: $(this).text(),
                    value: $(this).attr("value"),
                    selected: true
                });
                // $(this).remove();
            }
        });
        this.favorites = defaultValues;
        if(limit != null) {
            this.limit = limit;
        }
        this.createUserSelect(selectName,  this.valuePrefix);
        // this.selectField.addClass("slim-select-hack");
        $("." + this.slimSelect.settings.id).each(function() {
        // $("." + this.slimSelect.config.id).each(function() {
           $(this).removeAttr("style");
        });
        this.selectField.slim = this.slimSelect;
        this.itiErrorMap = ["Numéro invalide", "Pays invalide", "Trop court", "Trop long", "Numéro invalide"];
        this.initListeners();
    }

    initListeners() {
    }

    bindEnterKeyPress() {
        this.slimSelect.render.content.search.input.removeEventListener('keydown', (e) => this.enterkeyPressAction(e))
        this.slimSelect.render.content.search.input.addEventListener('keydown', (e) => this.enterkeyPressAction(e));
    }

    enterkeyPressAction(e) {
        if (e.key === 'Enter') {
            e.preventDefault()
            this.slimSelect.render.content.list.querySelectorAll("div").forEach(item => {
                console.info(item);
                const highlighted = item.classList.contains(this.slimSelect.render.classes.highlighted);
                if (highlighted) {
                    item.click();
                }
            });
        }
    }

    createUserSelect(selectName, valuePrefix) {
        let controller = new AbortController();
        let signal = controller.signal;
        let placeHolder;
        if(this.limit > 1) {
            placeHolder = "Choisir une ou plusieurs personnes";
        } else {
            placeHolder = "Choisir une personne";
        }
        let self = this;
        let position = "absolute";
        if(this.selectField.attr("data-es-relative")) {
            position = "relative";
        }
        this.slimSelect = new SlimSelect({
            select: "#" + selectName,
            settings: {
                placeholderText: placeHolder,
                searchText: 'Aucun résultat',
                searchingText: 'Recherche en cours',
                searchPlaceholder: 'Rechercher',
                searchHighlight: false,
                hideSelectedOption: true,
                closeOnSelect: true,
                maxSelected: self.limit,
                maxValuesShown: 40,
            },
            events: {
                beforeOpen: e => this.populateWithFavorites(),
                error: e => e => this.error(e),
                beforeChange: e=> this.checkSelect(e),
                afterChange: e => this.displayTempUsers(e),
                addable: e => this.validateEmail(e),
                searchFilter: (option, search) => {
                    return true;
                },
                search: (search, currentData) => {
                    return new Promise((resolve, reject) => {
                        controller.abort();
                        controller = new AbortController()
                        signal = controller.signal;
                        if (search.length < 3) {
                            return reject('Merci de saisir au moins 3 caractères');
                        } else {
                            fetch('/user/users/search-user?searchString=' + search, {
                                method: 'get',
                                signal: signal,
                            })
                                .then((response) => {
                                    return response.json()
                                })
                                .then((json) => {
                                    let data = []
                                    for (let i = 0; i < json.length; i++) {
                                        if(json[i].displayName != null) {
                                            if (json[i].displayName !== json[i].mail) {
                                                data.push({
                                                    text: json[i].displayName + ' (' + json[i].mail + ')',
                                                    value: valuePrefix + json[i].mail
                                                });
                                            } else {
                                                data.push({
                                                    text: json[i].displayName,
                                                    value: valuePrefix + json[i].mail
                                                });
                                            }
                                        } else {
                                            data.push({
                                                text: json[i].mail,
                                                value: valuePrefix + json[i].mail
                                            });
                                        }
                                    }
                                    this.flag = true;
                                    controller.abort();
                                    controller = new AbortController();
                                    signal = controller.signal;
                                    if(data.length > 0) {
                                        return resolve(data);
                                    } else {
                                        if(this.validateEmail(search)) {
                                            return reject("Email non trouvé, utilisez le '+' pour ajouter cette adresse");
                                        } else {
                                            return reject("Aucun résultat. Pour ajouter une personne externe entrer une adresse email");
                                        }
                                    }
                                })
                                .catch(function () {
                                    return reject("Recherche en cours");
                                });
                            }
                        });
                    }
                }
            });
        this.selectField.css("display", "block");
        this.selectField.css("position", "absolute");
        this.selectField.css("height", 38);
        this.selectField.css("opacity", 0);
        this.selectField.css("z-index", -1);
        this.bindEnterKeyPress();

    }

    getSelected() {
        return this.slimSelect.getSelected();
    }

    checkSelect(emails) {
        console.log(emails);
        this.slimSelect.setData(emails);
        this.bindEnterKeyPress();

    }

    displayTempUsers(e) {
        if(this.checkList !== "false") {
            if (this.selectField.attr('id') === 'recipientsEmails') {
                if (this.slimSelect.getSelected().length > 0) {
                    // $('#addNew').show();
                    $('#end-workflow-sign').hide();
                    $('#end').hide();
                } else {
                    // $('#addNew').hide();
                    $('#end-workflow-sign').show();
                    $('#end').show();
                }
            }
            let recipientEmails;
            if (Array.isArray(this.slimSelect.getSelected())) {
                recipientEmails = this.slimSelect.getSelected();
            } else {
                recipientEmails = new Array(this.slimSelect.getSelected());
            }
            if(!this.selectField.attr('id').includes("CC")) {
                if (recipientEmails.length > 1) {
                    $("#all-sign-to-complete-div-" + this.stepNumber).show();
                } else {
                    $("#all-sign-to-complete-div-" + this.stepNumber).hide();
                }
                if (this.csrf) {
                    let csrf = this.csrf;
                    $.ajax({
                        url: "/ws-secure/users/check-temp-users?" + csrf.parameterName + "=" + csrf.token,
                        type: 'POST',
                        contentType: "application/json",
                        dataType: 'json',
                        async: true,
                        data: JSON.stringify(recipientEmails),
                        success: datas => this.displayTempUsersSuccess(datas),
                        error: e => this.displayExternalsError(e)
                    });
                }
            }
            if (this.flag === true && e.length > 0) {
                let text = e[e.length - 1].value;
                console.info("check : " + text);
                if (text != null && !text.includes('(')) {
                    $.ajax({
                        url: "/user/users/search-user-list?searchString=" + text,
                        type: 'GET',
                        dataType: 'json',
                        contentType: "application/json",
                        success: response => this.addListMembers(response, text)
                    });
                }
            }
        }
    }

    displayExternalsError(e) {
        let name = '#tempUsers-' + this.selectField.attr("id");
        let tempUsersDiv = $(name);
        if ($("#recipient_").length === 0) {
            if(e.status === 500) {
                let message = "Une erreur inconnue est survenue :"
                if (e.responseJSON && e.responseJSON.message) {
                    message = e.responseJSON.message;
                }
                tempUsersDiv.append(
                    "<div class='alert alert-danger' id='recipient_'>" +
                    "<b>Le destinataire n’a pas pu être vérifié </b>: <br>Soit les destinataires externes ne sont pas autorisés, soit il s’agit d’un groupe vide.<br>" +
                    "<small>Detail de l'erreur : " + message + "</small>" +
                    "</div>");
            } else {
                tempUsersDiv.append(
                    "<div class='alert alert-danger' id='recipient_'>" +
                    "<b>Un problème s’est produit lors de la vérification des destinataires</b><br>Merci d’actualiser la page avant d’essayer de nouveau." +
                    "</div>");
            }
        }
    }

    addListMembers(data, selectValue) {
        if(data.length > 0) {
            this.flag = false;
            let arrayTexts = [];
            let arrayValues = [];
            let prevData = this.slimSelect.getSelected()
            if (Array.isArray(prevData)) {
                for (let i = 0; i < prevData.length; i++) {
                    let onePrevData = prevData[i];
                    if (onePrevData !== selectValue && onePrevData !== "undefined" && onePrevData !== "") {
                        arrayTexts.push({
                            text: onePrevData,
                            value: onePrevData,
                            display: true
                        });
                        arrayValues.push(onePrevData);
                    }
                }
            } else {
                arrayTexts.push({
                    text: prevData,
                    value: prevData,
                    display: true
                });
                arrayValues.push(prevData);
            }
            if (data.length > 10) {
                alert("Merci de sélectionner une liste contenant au maximum 10 personnes. La liste sélectionnée dépasse cette limite.");
            } else {
                for (let i = 0; i < data.length; i++) {
                    if(!arrayValues.includes(this.valuePrefix + data[i])) {
                        arrayTexts.push({text: data[i], value: this.valuePrefix + data[i], display: true})
                        arrayValues.push(this.valuePrefix + data[i]);
                    }
                }
            }
            this.slimSelect.setData(arrayTexts);
            this.slimSelect.setSelected(arrayValues);
        }
    }

    displayTempUsersSuccess(datas) {
        console.log("display temp users in " + this.selectField.attr("id"));
        let tempUsersDiv = $('#tempUsers-' + this.selectField.attr("id"));
        tempUsersDiv.empty();
        datas.forEach(e => this.appendTempUser(e));
    }

    appendTempUser(data) {
        console.warn(data);
        let id = data.email.replaceAll("@", "_").replaceAll(".", "_")
        let name = "#tempUsers-" + this.selectField.attr("id");
        let tempUsersDiv = $(name);
        if (data.phone == null) {
            data.phone = "";
        }
        let html = "<div class='alert alert-primary' id='recipient_" + id + "'>";
        if(this.globalProperties.smsRequired) {
            html +=
                "<b>Destinataire externe : <span>" + id + "</span></b>" +
                "<input id=\"email\" class=\"form-control \" type=\"hidden\" name=\"emails\" value=\"" + id + "\">" +
                "<div class=\"d-flex col-12\"><label for=\"name\" class='col-3'>Nom</label>" +
                "<input id=\"name_" + id + "\" class=\"form-control \" type=\"text\" name=\"names\" value=\"" + data.name + "\" required></div>" +
                "<div class=\"d-flex col-12\"><label for=\"firstname\" class='col-3'>Prénom</label>" +
                "<input id=\"firstname_" + id + "\" class=\"form-control \" type=\"text\" name=\"firstnames\" value=\"" + data.firstname + "\" required></div>" +
                "<div class=\"d-flex col-12\"><label for=\"phones\" class='col-3'>Mobile</label>" +
                "<input id=\"phone_" + id + "\" class=\"form-control \" type=\"text\" name=\"phones\" value=\"" + data.hidedPhone + "\">" +
                "<span id=\"valid-msg_" + id + "\" class=\"text-success my-auto d-none\">✓ Ok</span>\n" +
                "<span id=\"error-msg_" + id + "\" class=\"text-danger my-auto d-none\"></span>";
        } else {
            html +=
                "<b>Destinataire externe : <span>" + id + "</span></b>" +
                "<input id=\"email\" class=\"form-control \" type=\"hidden\" name=\"emails\" value=\"" + id + "\">" +
                "<div class=\"d-flex col-12\"><label for=\"name\" class='col-3'>Nom</label>" +
                "<input id=\"name_" + id + "\" class=\"form-control \" type=\"text\" name=\"names\" value=\"" + data.name + "\" required></div>" +
                "<div class=\"d-flex col-12\"><label for=\"firstname\" class='col-3'>Prénom</label>" +
                "<input id=\"firstname_" + id + "\" class=\"form-control \" type=\"text\" name=\"firstnames\" value=\"" + data.firstname + "\" required></div>";
            if (this.enableSms) {
                html += "<div class=\"d-flex col-12\"><label for=\"phones\" class='col-3'>Mobile</label>" +
                    "<input id=\"phone_" + id + "\" class=\"form-control \" type=\"text\" name=\"phones\" value=\"" + data.phone + "\">" +
                    "<span id=\"valid-msg_" + id + "\" class=\"text-success my-auto d-none\">✓ Ok</span>\n" +
                    "<span id=\"error-msg_" + id + "\" class=\"text-danger my-auto d-none\"></span>" +
                    "</div>" +
                    "<div class=\"d-flex col-12\"><label for=\"forcesms\" class='col-3'>Autentification SMS obligatoire</label>" +
                    "<input id=\"forcesms_" + id + "\" class=\"form-check-input \" type=\"checkbox\" name=\"forcesmses\" value='1'></div>";
            }
        }
        html += "</div>";
        tempUsersDiv.append(html);
        let phonesInput = document.querySelector("#phone_" + id);
        if(phonesInput != null) {
            let iti = intlTelInput(phonesInput, {
                validationNumberTypes: "FIXED_LINE_OR_MOBILE",
                strictMode: true,
                separateDialCode: false,
                nationalMode: true,
                countryOrder: ["fr"],
                initialCountry: "auto",
                geoIpLookup: callback => {
                    callback(navigator.language.split('-')[0]);
                },
                customPlaceholder: (selectedCountryPlaceholder, selectedCountryData) => "Saisir un numéro",
                searchPlaceholder: "Rechercher",
            });
            if (data.phone == null || data.phone === "") {
                iti.setCountry("fr");
            }
            this.validatePhone(iti, id)
            phonesInput.addEventListener("focusout", e => this.validatePhone(iti, id));
        }
    }

    validatePhone(iti, id) {
        const errorMsg = document.querySelector("#error-msg_" + id);
        const validMsg = document.querySelector("#valid-msg_" + id);
        if(iti.isValidNumber()) {
            errorMsg.classList.add("d-none");
            validMsg.classList.remove("d-none");
            $("#phone_" + id).val(iti.getNumber());
        } else {
            errorMsg.innerHTML = this.itiErrorMap[iti.getValidationError()];
            // errorMsg.classList.remove("d-none");
            // validMsg.classList.add("d-none");
        }
    }

    validateEmail(email) {
        const re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
        if(re.test(String(email).toLowerCase())) {
            let emailObject = {
                text: email,
                value: this.valuePrefix + email
            };
            this.slimSelect.setData(this.slimSelect.getData().filter(d => d.value !== email));
            return emailObject;
        }
        return false;
    }

    setFavorites(response) {
        if(response.length > 0) {
            let selected = this.slimSelect.getSelected();
            let favorites = this.slimSelect.getData();
            for (let j = 0; j < response.length; j++) {
                let user = response[j];
                let prefixedValue = this.valuePrefix + user.email;
                if (this.favorites.filter(f => f.value === prefixedValue).length === 0) {
                    if(favorites.filter(f => f.value === prefixedValue).length === 0) {
                        favorites.push({
                            text: user.firstname + " " + user.name + " (" + user.email + ")",
                            value: prefixedValue,
                            selected: false
                        });
                    }
                }
                if(favorites.length > 0) {
                    this.slimSelect.setData(favorites);
                    // this.slimSelect.setSelected(selected);
                }
            }
        }
        this.bindEnterKeyPress();
        // if(this.favorites.length > 0) {
        //     this.slimSelect.setData(this.favorites);
        //     let selectedFavorites = this.favorites.filter(f => f.selected).map(f => f.value);
        //     // if (selectedFavorites.length > 0) {
        //     //     this.slimSelect.set(selectedFavorites);
        //     // } else {
        //     //     this.slimSelect.set();
        //     // }
        // }
    }

    populateWithFavorites() {
        $.ajax({
            url: "/ws-secure/users/get-favorites",
            type: 'GET',
            dataType: 'json',
            contentType: "application/json",
            async: true,
            success: response => this.setFavorites(response)
        });
    }

    error(e) {
        console.error(e);
    }
}