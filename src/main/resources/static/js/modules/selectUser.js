export default class SelectUser {

    constructor(selectName) {
        this.createUserSelect(selectName)
    }

    createUserSelect(selectName) {
        new SlimSelect({
            select: "#" + selectName,
            placeholder: 'Choisir un ou plusieurs participants',
            searchText: 'Aucun résultat',
            searchPlaceholder: 'Rechercher',
            searchHighlight: true,
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
                                data.push({text: json[i].displayName + ' (' + json[i].mail + ')', value: json[i].mail});
                            }
                            callback(data)
                        })
                        .catch(function (error) {
                            callback(false)
                        })
                }
            }
        });
    }

}