export default class SelectUser {

    constructor(selectName, limit, signRequestId) {
        console.debug("init select-user : " + selectName);
        this.slimSelect = null;
        this.selectField = $("#" + selectName);
        this.signRequestId = signRequestId;
        let valuePrefix = "";
        this.limit = 99;
        let selectNameSplit = selectName.split("_");
        if(selectNameSplit.length === 2) {
            valuePrefix = selectNameSplit[1] + "*";
        }
        if(limit != null) {
            this.limit = limit;
        }
        this.createUserSelect(selectName, valuePrefix);
        this.selectField.addClass("slim-select-hack");
        this.initListeners();
    }

    initListeners() {
    }

    displayTempUsers(e) {
        let recipientEmails = this.slimSelect.selected()
        $.ajax({
            url: "/user/signrequests/is-temp-users/" + this.signRequestId,
            type: 'GET',
            dataType : 'json',
            contentType: "application/json",
            data: {"recipientEmails" : JSON.stringify(recipientEmails)},
            success: data => this.displayTempUsersSuccess(data)
        });
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
            "<b>Destinataire : <span>"+ e.email +"</span></b>" +
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

    createUserSelect(selectName, valuePrefix) {
        var controller = new AbortController()
        var signal = controller.signal
        this.slimSelect = new SlimSelect({
            select: "#" + selectName,
            placeholder: 'Choisir un ou plusieurs participants',
            searchText: 'Aucun résultat',
            searchPlaceholder: 'Rechercher',
            searchHighlight: false,
            hideSelectedOption: false,
            closeOnSelect: true,
            limit: this.limit,
            onChange : e => this.displayTempUsers(e),
            addable: e => this.validateEmail(e),
            searchFilter: (option, search) => {
                return true;
            },
            ajax: function (search, callback) {
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
                        .then(function (response) {
                            return response.json()
                        })
                        .then(function (json) {
                            let data = []
                            for (let i = 0; i < json.length; i++) {
                                data.push({text: json[i].displayName + ' (' + json[i].mail + ')', value: valuePrefix + json[i].mail});
                            }
                            callback(data);
                        })
                        .catch(function () {
                            callback("Recherche en cours");
                        })
                }
            }
        });
    }
}