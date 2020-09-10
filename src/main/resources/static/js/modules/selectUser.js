export default class SelectUser {

    constructor(selectName, valuePrefix, limit) {
        this.slimSelect = null;
        this.selectField = $("#" + selectName);
        console.info("Enable slim-select for " + selectName);
        if(valuePrefix == null) {
            valuePrefix = "";
        }
        this.limit = 99;
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
        this.slimSelect = new SlimSelect({
            select: "#" + selectName,
            placeholder: 'Choisir un ou plusieurs participants',
            searchText: 'Aucun résultat',
            searchPlaceholder: 'Rechercher',
            searchHighlight: false,
            hideSelectedOption: true,
            closeOnSelect: true,
            limit: this.limit,
            addable: e => this.validateEmail(e),
            searchFilter: (option, search) => {
                return true;
            },
            ajax: function (search, callback) {
                if (search.length < 3) {
                    callback('Merci de saisir au moins 3 caractères')
                } else {
                    fetch('/user/users/search-user?searchString=' + search)
                        .then(function (response) {
                            return response.json()
                        })
                        .then(function (json) {
                            let data = []
                            for (let i = 0; i < json.length; i++) {
                                data.push({text: json[i].displayName + ' (' + json[i].mail + ')', value: valuePrefix + json[i].mail});
                            }
                            callback(data)
                        })
                        .catch(function () {
                            callback(false)
                        })
                }
            }
        });
    }
}