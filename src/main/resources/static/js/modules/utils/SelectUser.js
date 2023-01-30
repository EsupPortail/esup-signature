export default class SelectUser {

    constructor(selectName, limit, signRequestId, csrf) {
        console.info("init select-user : " + selectName);
        this.slimSelect = null;
        this.selectField = $("#" + selectName);
        this.selectField.attr("stepSelection", "true");
        this.checkList = this.selectField.attr("data-es-check-list");
        this.signRequestId = signRequestId;
        this.csrf = csrf;
        this.valuePrefix = "";
        this.limit = 99;
        this.flag = false;
        let selectNameSplit = selectName.split("_");
        if(selectNameSplit.length === 2) {
            this.valuePrefix = selectNameSplit[1] + "*";
        }
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
        this.populateWithFavorites();
        this.selectField.addClass("slim-select-hack");
        //$("." + this.slimSelect.settings.id).each(function() { DANS slimselect 2.3.3
        $("." + this.slimSelect.config.id).each(function() {
           $(this).removeAttr("style");
        });
        this.selectField.slim = this.slimSelect;
        this.initListeners();
    }

    initListeners() {
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
        this.slimSelect = new SlimSelect({
            select: "#" + selectName,
            // data: self.favorites,
            placeholder: placeHolder,
            searchText: 'Aucun résultat',
            searchPlaceholder: 'Rechercher',
            searchHighlight: false,
            hideSelectedOption: true,
            closeOnSelect: true,
            limit: this.limit,
            onChange : e => this.displayTempUsers(e),
            addable: e => this.validateEmail(e),
            searchFilter: (option, search) => {
                return true;
            },
            ajax: (search, callback) => {
                callback('Recherche en cours');
                controller.abort();
                controller = new AbortController()
                signal = controller.signal;
                if (search.length < 3) {
                    callback('Merci de saisir au moins 3 caractères');
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
                            if(json[i].displayName !== json[i].mail) {
                                data.push({text: json[i].displayName + ' (' + json[i].mail + ')', value: valuePrefix + json[i].mail});
                            } else {
                                data.push({text: json[i].displayName, value: valuePrefix + json[i].mail});
                            }
                        }
                        this.flag = true;
                        controller.abort();
                        controller = new AbortController();
                        signal = controller.signal;
                        if(data.length > 0) {
                            callback(data);
                        } else {
                            if(this.validateEmail(search)) {
                                callback("Email non trouvé, utilisez le '+' pour ajouter cette adresse");
                            } else {
                                callback("Aucun résultat. Pour ajouter une personne externe entrer une adresse email");
                            }
                        }
                    })
                    .catch(function () {
                        callback("Recherche en cours");
                    })
                }
            }
        });
    }

    displayTempUsers(e) {
        if(this.checkList !== "false") {
            if (this.selectField.attr('id') === 'recipientsEmailsWiz') {
                if (this.slimSelect.selected().length > 0) {
                    // $('#addNew').show();
                    $('#endStart').hide();
                    $('#end').hide();
                } else {
                    // $('#addNew').hide();
                    $('#endStart').show();
                    $('#end').show();
                }
            }
            let recipientEmails;
            if (Array.isArray(this.slimSelect.selected())) {
                recipientEmails = this.slimSelect.selected();
            } else {
                recipientEmails = new Array(this.slimSelect.selected());
            }
            $('[id^="allSignToComplete-"]').each(function () {
                if (recipientEmails.length > 1) {
                    $(this).show();
                } else {
                    $(this).hide();
                }
            })
            if (this.csrf) {
                let csrf = this.csrf;
                $.ajax({
                    url: "/ws-secure/users/check-temp-users/?" + csrf.parameterName + "=" + csrf.token,
                    type: 'POST',
                    contentType: "application/json",
                    dataType: 'json',
                    async: true,
                    data: JSON.stringify(recipientEmails),
                    success: data => this.displayTempUsersSuccess(data),
                    error: e => this.displayExternalsError()
                });
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

    displayExternalsError() {
        let name = '#tempUsers-' + this.selectField.attr("id");
        let tempUsersDiv = $(name);
        tempUsersDiv.append(
            "<div class='alert alert-danger' id='externalUserInfos_'>" +
            "<b>Le destinataire saisi n’est pas conforme</b><br>Soit les destinataires externes ne sont pas autorisés, soit il s’agit d’un groupe vide" +
            "</div>");
    }

    addListMembers(data, selectValue) {
        if(data.length > 0) {
            this.flag = false;
            let arrayTexts = [];
            let arrayValues = [];
            let prevData = this.slimSelect.selected()
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
                    arrayTexts.push({text: data[i], value: this.valuePrefix + data[i], display: true})
                    arrayValues.push(this.valuePrefix + data[i]);
                }
            }
            this.slimSelect.setData(arrayTexts);
            this.slimSelect.set(arrayValues);
        }
    }

    displayTempUsersSuccess(data) {
        console.log("display temp users");
        let tempUsersDiv = $('#tempUsers-' + this.selectField.attr("id"));
        tempUsersDiv.empty();
        data.forEach(e => this.appendTempUser(e));
    }

    appendTempUser(e) {
        let name = '#tempUsers-' + this.selectField.attr("id");
        let tempUsersDiv = $(name);
        if(e.phone == null) {
            e.phone = "";
        }
        tempUsersDiv.append(
            "<div class='alert alert-primary' id='externalUserInfos_" + e.email + "'>" +
            "<b>Destinataire externe : <span>"+ e.email +"</span></b>" +
            "<input id=\"emails\" class=\"form-control \" type=\"hidden\" name=\"emails\" value=\""+ e.email +"\">" +
            "<div class=\"d-flex col-10\"><label for=\"name\" class='col-2'>Nom</label>" +
            "<input id=\"names\" class=\"form-control \" type=\"text\" name=\"names\" value=\""+ e.name +"\" required></div>" +
            "<div class=\"d-flex col-10\"><label for=\"firstname\" class='col-2'>Prénom</label>" +
            "<input id=\"firstnames\" class=\"form-control \" type=\"text\" name=\"firstnames\" value=\""+ e.firstname +"\" required></div>" +
            "<div class=\"d-flex col-10\"><label for=\"phones\" class='col-2'>Mobile</label>" +
            "<input id=\"phones\" class=\"form-control \" type=\"text\" name=\"phones\" value=\""+ e.phone +"\"></div>" +
            "</div>");
    }

    validateEmail(email) {
        const re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
        if(re.test(String(email).toLowerCase())) {
            return {
                text: email,
                value: this.valuePrefix + email
            }
        }
        return false;
    }

    setFavorites(response) {
        if(response.length > 0) {
            let toto = [];
            for (let j = 0; j < response.length; j++) {
                let value = response[j];
                if (this.favorites.filter(f => f.text === value).length === 0) {
                    toto.push({
                        text: value,
                        value: this.valuePrefix + value,
                        selected: false
                    });
                }
                if(toto.length > 0) {
                    this.slimSelect.setData(toto);
                    this.slimSelect.set();
                }
            }
        }
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

}