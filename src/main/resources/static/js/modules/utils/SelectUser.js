export default class SelectUser {

    constructor(selectName, limit, signRequestId) {
        console.debug("init select-user : " + selectName);
        this.slimSelect = null;
        this.selectField = $("#" + selectName);
        this.signRequestId = signRequestId;
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
        if (this.signRequestId != null) {
            let recipientEmails = this.slimSelect.selected()
            $.ajax({
                url: "/user/signrequests/is-temp-users/" + this.signRequestId,
                type: 'GET',
                dataType: 'json',
                contentType: "application/json",
                data: {"recipientEmails": JSON.stringify(recipientEmails)},
                success: data => this.displayTempUsersSuccess(data)
            });
        }
        if (this.flag === true) {
            if (!e[e.length - 1].text.includes('(')) {
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
        let tempUsersDiv = $('#tempUsers');
        tempUsersDiv.empty();
        data.forEach(e => this.appendTempUser(e));
    }

    appendTempUser(e) {
        let tempUsersDiv = $('#tempUsers');
        tempUsersDiv.append(" " +
            "<div>" +
            "<b>Destinataire externe : <span>"+ e.email +"</span></b>" +
            "<div class=\"form-inline\">" +
            "<label for=\"name\">Nom</label>" +
            "<input id=\"name\" class=\"form-control mr-2\" type=\"text\" name=\"names\" value=\""+ e.name +"\" required>" +
            "<label for=\"firstname\">Prénom</label>" +
            "<input id=\"firstname\" class=\"form-control mr-2\" type=\"text\" name=\"firstnames\" value=\""+ e.firstname +"\" required>\n" +
            "<label for=\"phones\">Mobile</label>\n" +
            "<input id=\"phones\" class=\"form-control  mr-2\" type=\"text\" name=\"phones\" value=\""+ e.eppn +"\" required>\n" +
            "</div>\n" +
            "</div>" +
            "");
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
        console.log(this.favorites);
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
        console.log(this.favorites);
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
                if (search.length < 4) {
                    callback('Merci de saisir au moins 4 caractères');
                } else {
                    fetch('/user/users/search-user?searchString=' + search, {
                        method: 'get',
                        signal: signal,
                    })
                        .then((response) => {
                            return response.json()
                        })
                        .then((json) => {
                            if (json.length > 0) {
                                let data = []
                                for (let i = 0; i < json.length; i++) {
                                    data.push({text: json[i].displayName + ' (' + json[i].mail + ')', value: valuePrefix + json[i].mail});
                                }
                                callback(data);
                            } else {
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
                                        let data = []
                                        for (let i = 0; i < json.length; i++) {
                                            data.push({text: json[i].mailAlias, value: valuePrefix + json[i].mailAlias});
                                        }
                                        if(data.length > 0) {
                                            callback(data);
                                        }
                                    })
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