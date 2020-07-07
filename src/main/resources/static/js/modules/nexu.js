export class Nexu {

    constructor(nexuUrl, nexuVersion, rootUrl) {
        this.nexuUrl = nexuUrl;
        this.nexuVersion = nexuVersion;
        Nexu.rootUrl = rootUrl;
        this.tokenId = null;
        this.keyId = null;
        this.checkNexuClient();
        this.successDiv = $("#success");
        this.successDiv.hide();
    }

    getCertificates() {
        Nexu.updateProgressBar("Chargement des certificats", "25%");
        try {
            nexu_get_certificates(Nexu.getDataToSign, Nexu.error);
        }
        catch(e) {
            console.error(e);
            const merror = {
                errorMessage: "Problème avec l'application NexU"
            };
            error(Object.create(merror));
        }
    }

    static getDataToSign(certificateData) {
        if(certificateData.response == null) {
            const merror = {
                errorMessage: "Erreur au moment de lire le certificat"
            };
            error(Object.create(merror));
        } else {
            console.log(Nexu.rootUrl);
            Nexu.updateProgressBar("Préparation de la signature", "35%");
            let signingCertificate = certificateData.response.certificate;
            let certificateChain = certificateData.response.certificateChain;
            let encryptionAlgorithm = certificateData.response.encryptionAlgorithm;
            Nexu.tokenId = certificateData.response.tokenId.id;
            Nexu.keyId = certificateData.response.keyId;
            console.log("init tokenId : " + this.tokenId + "," + this.keyId);
            let toSend = { signingCertificate: signingCertificate, certificateChain: certificateChain, encryptionAlgorithm: encryptionAlgorithm };
            callUrl(Nexu.rootUrl + "/user/nexu-sign/get-data-to-sign", "POST",  JSON.stringify(toSend), Nexu.sign, Nexu.error);
        }
    }

    static sign(dataToSignResponse) {
        if (dataToSignResponse == null) {
            const merror = {
                errorMessage: "Erreur lors de la vérification du certificat"
            };
            error(Object.create(merror));
        } else {
            Nexu.updateProgressBar("Signature du/des documents(s)", "50%");
            console.log("token : " + Nexu.tokenId + "," + Nexu.keyId);
            let digestAlgo = "SHA256";
            nexu_sign_with_token_infos(Nexu.tokenId, Nexu.keyId, dataToSignResponse.dataToSign, digestAlgo, Nexu.signDocument, Nexu.error);
        }
    }

    static signDocument(signatureData) {
        Nexu.updateProgressBar("Enregistrement du/des documents(s)", "75%");
        if(signatureData.response != null) {
            let signatureValue = signatureData.response.signatureValue;
            let toSend = {signatureValue: signatureValue};
            callUrl(Nexu.rootUrl + "/user/nexu-sign/sign-document", "POST", JSON.stringify(toSend), Nexu.downloadSignedDocument, Nexu.error);
        } else {
            const merror = {
                errorMessage: "Erreur au moment de la signature du document"
            };
            error(Object.create(merror));
        }
    }

    static downloadSignedDocument() {
        Nexu.updateProgressBar("Signature terminée", "100%");
        $('#bar').removeClass('progress-bar-striped active');
        $('#bar').addClass('progress-bar-success');
        $("#success").show();
    }

    static error(error) {
        console.log(error);
        $('#bar').removeClass('progress-bar-success active').addClass('progress-bar-danger');
        if (error!= null && error.responseJSON !=null) {
            var jsonResp = error.responseJSON;
            if (jsonResp.errorMessage !=null){
                $("#errorcontent").html(jsonResp.errorMessage);
            }
            else if (jsonResp.error != null){
                $("#errorcontent").html(jsonResp.error);
            }
        }
        $("#error").show();
        $("#success").hide();
    }

    static updateProgressBar(action, percent) {
        console.log("update " + action);
        $('#bar-text').html(action);
        $('#bar').width(percent);
    }

    checkNexuClient() {
        console.log("Start checking NexU");
        $.ajax({
            type: "GET",
            url: this.nexuUrl + "/nexu-info",
            crossDomain: true,
            dataType: "json",
            context : this,
            success: data => this.checkNexu(data)
        }).fail(function (error) {
            console.warn("NexU not detected or not started ! " + JSON.stringify(error));
            $("#warning-text").html("NexU not detected or not started ! ");
            $("#nexu_missing_alert").show();
        });

    }

    checkNexu(data) {
        console.log("Check NexU");
        if(data.version.startsWith(this.nexuVersion)) {
            console.log("Loading script...");
            this.loadScript();
            $("#nexu_ready_alert").show();
            $("#submit-button").prop('disabled', false);
        } else {
            // need update
            $("#nexu_version_alert").show();
            console.log("Bad NexU version " + data.version + " instead of " + this.nexuVersion);

        }
    }

    loadScript() {
        let xhrObj = new XMLHttpRequest();
        xhrObj.open('GET', this.nexuUrl + "/nexu.js");
        xhrObj.send(null);
        let se = document.createElement('script');
        se.type = "text/javascript";
        se.text = xhrObj.responseText;
        document.getElementsByTagName('head')[0].appendChild(se);
        console.log("NexU script loaded");
    }

}