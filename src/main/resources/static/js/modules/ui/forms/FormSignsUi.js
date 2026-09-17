import {Message} from "../../../prototypes/Message.js?version=@version@";
import NotificationCenter from "../NotificationCenter.js?version=@version@";
import {attachDirtyIndicator} from "../DirtyIndicator.js?version=@version@";

export default class FormSignsUi {

    constructor(domain, formId, csrf) {
        console.info("Starting Form Signs UI");
        this.notificationCenter = new NotificationCenter();
        this.dirtyIndicator = null;
        this.domain = domain;
        this.formId = formId;
        this.csrf = csrf;
        this.initListeners();
    }

    initListeners() {
        $("#saveButton").on("click", e => this.save());
        this.initDirtyIndicator();
    }

    initDirtyIndicator() {
        if (this.dirtyIndicator != null) {
            return;
        }

        const formContainer = document.querySelector('#updateSignsModal .modal-content');
        const saveButton = document.getElementById('saveButton');

        if (!formContainer || !saveButton) {
            return;
        }

        this.dirtyIndicator = attachDirtyIndicator({
            form: formContainer,
            saveButton
        });
    }

    save() {
        let message = new Message();
        message.type = "info";
        message.text = "Enregistrement en cours";
        message.object = null;
        let self = this;
        let srpMap = [];
        let fieldsUpdates = $('select[id^="srp_"]');
        let i = 0;
        fieldsUpdates.each(function() {
            srpMap.push($(this).attr("id").split("_")[1], $(this).val());
            i++;
        });
        $.ajax({
            type: "POST",
            url: "/" + self.domain + "/forms/update-signs-order/" + self.formId + "?"+ this.csrf.parameterName +"=" + this.csrf.token,
            data: {"srpMap" : JSON.stringify(srpMap)},
            success: function(data,status) {
                self.dirtyIndicator?.markClean();
                document.location.reload();
            },
            error: function(data, status) {
                let message = new Message();
                message.type = "error";
                message.text = "Problème lors de l'enregistrement";
                message.object = null;
                self.notificationCenter.launch(message);
            },
        });
    }
}