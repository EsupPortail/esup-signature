export default class SelectUser {

    constructor(selectName, limit) {
        console.debug("init select-user : " + selectName);
        this.slimSelect = null;
        this.selectField = $("#" + selectName);
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