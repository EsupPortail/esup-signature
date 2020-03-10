export class Nexu {

    nexuUrl;
    nexuVersion;
    rootUrl;
    tokenId;
    keyId;
    successDiv = $("#success");

    constructor(nexuUrl, nexuVersion, rootUrl) {
        this.nexuUrl = nexuUrl;
        this.nexuVersion = nexuVersion;
        this.rootUrl = rootUrl;
        // if (!String.prototype.startsWith) {
        //     String.prototype.startsWith = function(searchString, position){
        //         return this.substr(position || 0, searchString.length) === searchString;
        //     };
        // }
        this.checkNexuClient();
        this.successDiv.hide();
    }

    getCertificates() {
        updateProgressBar("Chargement des certificats", "25%");
        try {
            nexu_get_certificates(getDataToSign, error);
        }
        catch(e) {
            console.error(e);
            const merror = {
                errorMessage: "Problème avec l'application NexU"
            };
            error(Object.create(merror));
        }
    }

    getDataToSign(certificateData) {
        if(certificateData.response == null) {
            const merror = {
                errorMessage: "Erreur au moment de lire le certificat"
            };
            error(Object.create(merror));
        } else {
            updateProgressBar("Préparation de la signature", "35%");
            var signingCertificate = certificateData.response.certificate;
            var certificateChain = certificateData.response.certificateChain;
            var encryptionAlgorithm = certificateData.response.encryptionAlgorithm;
            tokenId = certificateData.response.tokenId.id;
            keyId = certificateData.response.keyId;
            var toSend = { signingCertificate: signingCertificate, certificateChain: certificateChain, encryptionAlgorithm: encryptionAlgorithm };

            callUrl("[[${rootUrl}]]/user/nexu-sign/get-data-to-sign", "POST",  JSON.stringify(toSend), sign, error);
        }
    }

    sign(dataToSignResponse) {
        if (dataToSignResponse == null) {
            const merror = {
                errorMessage: "Erreur lors de la vérification du certificat"
            };
            error(Object.create(merror));
        } else {
            updateProgressBar("Signature du/des documents(s)", "50%");
            var digestAlgo = "SHA256";
            nexu_sign_with_token_infos(tokenId, keyId, dataToSignResponse.dataToSign, digestAlgo, signDocument, error);
        }
    }

    signDocument(signatureData) {
        updateProgressBar("Enregistrement du/des documents(s)", "75%");
        if(signatureData.response != null) {
            var signatureValue = signatureData.response.signatureValue;
            var toSend = {signatureValue: signatureValue};
            callUrl("[[${rootUrl}]]/user/nexu-sign/sign-document", "POST", JSON.stringify(toSend), downloadSignedDocument, error);
        } else {
            const merror = {
                errorMessage: "Erreur au moment de la signature du document"
            };
            error(Object.create(merror));
        }
    }

    downloadSignedDocument(signDocumentResponse) {
        updateProgressBar("Signature terminée", "100%");
        $('#bar').removeClass('progress-bar-striped active');
        $('#bar').addClass('progress-bar-success');
        $("#success").show();
    }

    error(error) {
        console.log(error);
        $('#bar').removeClass('progress-bar-success active').addClass('progress-bar-danger');
        if (error!= null && error.errorMessage !=null) {
            if (error.errorMessage !=null){
                $("#errorcontent").html(error.errorMessage);
            }
            else {
                $("#errorcontent").html(error.error);
            }
        }
        $("#error").show();
        $("#success").hide();
    }

    updateProgressBar(action, percent) {
        $('#bar-text').html(action);
        $('#bar').width(percent);
    }

    checkNexuClient() {
        console.log("Start checking NexU");
        $.ajax({
            type: "GET",
            url: this.nexuUrl + "/nexu-info",
            crossDomain: true,
            dataType: "jsonp",
            context : this,
            success: data => this.checkNexu(data)
        }).fail(function (error) {
            console.warn("NexU not detected or not started ! " + JSON.stringify(error));
            $("#warning-text").html("NexU not detected or not started ! ");
            $("#nexu_missing_alert").show();
        });

    }

    checkNexu(data) {
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
        var xhrObj = new XMLHttpRequest();
        xhrObj.open('GET', this.nexuUrl + "/nexu.js");
        xhrObj.send(null);
        var se = document.createElement('script');
        se.type = "text/javascript";
        se.text = xhrObj.responseText;
        document.getElementsByTagName('head')[0].appendChild(se);
        console.log("NexU script loaded");
    }

}