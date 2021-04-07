export default class SelectUser {

    constructor(selectName, limit, signRequestId, csrf) {
        console.debug("init select-user : " + selectName);
        this.slimSelect = null;
        this.selectField = $("#" + selectName);
        this.signRequestId = signRequestId;
        this.csrf = csrf;
        this.valuePrefix = "";
        this.limit = 99;
        this.flag = false;
        this.favorites = null;
        let selectNameSplit = selectName.split("_");
        if(selectNameSplit.length === 2) {
            this.valuePrefix = selectNameSplit[1] + "*";
        }
        if(limit != null) {
            this.limit = limit;
        }
        this.createUserSelect(selectName,  this.valuePrefix);
        this.selectField.addClass("slim-select-hack");
        this.populateWithFavorites();
        this.initListeners();
    }

    initListeners() {
    }

    displayTempUsers(e) {
        // if (this.selectField.attr('id') === 'recipientsEmailsInfinite') {
        //     if (this.slimSelect.selected().length > 0) {
        //         $('#launchAllSignButton').hide();
        //         $('#launchSignButton').show();
        //     } else {
        //         $('#launchAllSignButton').show();
        //         $('#launchSignButton').hide();
        //     }
        // }
        if (this.selectField.attr('id') === 'recipientsEmailsWiz') {
            if (this.slimSelect.selected().length > 0) {
                $('#addNew').show();
                $('#endStart').hide();
                $('#end').hide();
            } else {
                $('#addNew').hide();
                $('#endStart').show();
                $('#end').show();
            }
        }
        let recipientEmails = this.slimSelect.selected()
        let csrf = this.csrf;
        $.ajax({
            url: "/user/users/check-temp-users/?" + csrf.parameterName + "=" + csrf.token,
            type: 'POST',
            contentType: "application/json",
            dataType: 'json',
            data: JSON.stringify(recipientEmails),
            success: data => this.displayTempUsersSuccess(data)
        });
        if (this.flag === true && e.length > 0) {
            let text = e[e.length - 1].text;
            if ( text != null && !text.includes('(')) {
                $.ajax({
                    url: "/user/users/search-user-list?searchString=" + e[e.length - 1].text,
                    type: 'GET',
                    dataType: 'json',
                    contentType: "application/json",
                    success: response => this.addListMembers(response, e[e.length - 1].text)
                });
            }
        }
    }

    addListMembers(data, selectValue) {
        if(data.length > 0) {
            this.flag = false;
            let array = [];
            let array2 = [];
            let prevData = this.slimSelect.selected()
            if (Array.isArray(prevData)) {
                for (let i = 0; i < prevData.length; i++) {
                    let onePrevData = prevData[i];
                    if (onePrevData !== selectValue && onePrevData !== "undefined" && onePrevData !== "") {
                        array.push({
                            text: onePrevData,
                            value: onePrevData,
                            display: true
                        });
                        array2.push(onePrevData);
                    }
                }
            } else {
                array.push({
                    text: prevData,
                    value: prevData,
                    display: true
                });
                array2.push(prevData);
            }
            if (data.length > 10) {
                alert("Merci de sélectionner une liste contenant au maximum 10 personnes. La liste sélectionnée dépasse cette limite.");
            } else {
                for (let i = 0; i < data.length; i++) {
                    array.push({text: data[i], value: this.valuePrefix + data[i], display: true})
                    array2.push(data[i]);
                }
            }
            this.slimSelect.setData(array);
            this.slimSelect.set(array2);
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
        tempUsersDiv.append(
            "<div class='alert alert-primary' id='externalUserInfos_" + e.email + "'>" +
            "<b>Destinataire externe : <span>"+ e.email +"</span></b>" +
            "<input id=\"emails\" class=\"form-control \" type=\"hidden\" name=\"emails\" value=\""+ e.email +"\">" +
            "<div class=\"d-flex col-10\"><label for=\"name\" class='col-2'>Nom</label>" +
            "<input id=\"names\" class=\"form-control \" type=\"text\" name=\"names\" value=\""+ e.name +"\" required></div>" +
            "<div class=\"d-flex col-10\"><label for=\"firstname\" class='col-2'>Prénom</label>" +
            "<input id=\"firstnames\" class=\"form-control \" type=\"text\" name=\"firstnames\" value=\""+ e.firstname +"\" required></div>" +
            "<div class=\"d-flex col-10\"><label for=\"phones\" class='col-2'>Mobile</label>" +
            "<input id=\"phones\" class=\"form-control \" type=\"text\" name=\"phones\" value='' required></div>" +
            "</div>");
    }

    validateEmail(email) {
        const re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
        if(re.test(String(email).toLowerCase())) {
            return email;
        }
        return false;
    }

    setFavorites(response) {
        let typeValues = [];
        for(let j = 0; j < response.length; j++) {
            let value = response[j];
            typeValues[j] = {
                text : value,
                value : this.valuePrefix + value,
            };
        }
        this.favorites = typeValues;
        this.slimSelect.setData(this.favorites);
        this.slimSelect.set();
    }

    populateWithFavorites() {
        $.ajax({
            url: "/user/users/get-favorites",
            type: 'GET',
            dataType: 'json',
            contentType: "application/json",
            success: response => this.setFavorites(response)
        });
    }

    createUserSelect(selectName, valuePrefix) {
        let controller = new AbortController();
        let signal = controller.signal;
        this.slimSelect = new SlimSelect({
            select: "#" + selectName,
            data: this.favorites,
            placeholder: 'Choisir un ou plusieurs participants',
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
                signal = controller.signal
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
                                data.push({text: json[i].displayName + ' (' + json[i].mail + ')', value: valuePrefix + json[i].mail});
                            }
                            this.flag = true;
                            controller.abort();
                            controller = new AbortController()
                            signal = controller.signal
                            fetch('/user/users/search-list?searchString=' + search, {
                                method: 'get',
                                signal: signal,
                            })
                                .then(function (response){
                                    return response.json()
                                })
                                .then(function (json) {
                                    for (let i = 0; i < json.length; i++) {
                                        data.unshift({text: json[i].mailAlias, value: valuePrefix + json[i].mailAlias});
                                        if(data.length > 0) {
                                            callback(data);
                                        }
                                    }
                                })
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

}