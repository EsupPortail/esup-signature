
export class FormUi {

    constructor() {

        this.initListeners();
    }

    initListeners() {
        $("#multipartModel").on('change', function () {
            $("#submitModel").removeClass("d-none");
        });
    }
}