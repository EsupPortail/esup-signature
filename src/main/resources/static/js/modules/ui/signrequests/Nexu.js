export class Nexu {

    constructor(addExtra, id, currentSignType) {
        this.globalProperties = JSON.parse(sessionStorage.getItem("globalProperties"));
        this.nexuUrl = this.globalProperties.nexuUrl;
        this.nexuVersion = this.globalProperties.nexuVersion;
        Nexu.rootUrl = this.globalProperties.rootUrl;
        Nexu.addExtra = addExtra;
        Nexu.id = id;
        this.tokenId = null;
        this.keyId = null;
        this.bindingPorts = "9795, 9886, 9887, 9888";
        this.detectedPort = "";
        this.successDiv = $("#success");
        this.successDiv.hide();
        $("#warning-text").html("NexU not detected or not started ! ");
        $("#nexu_missing_alert").show();
        let self = this;
        $(document).ready(function() {
            self.checkNexuClient().then(function(e) {
                console.info("NexU detected !");
                $("#warning-text").html("");
                $("#nexu_missing_alert").hide();
                $("#alertNexu").remove();
                $("#noOptions").hide();
                $("#selectTypeDiv").show();
                if(id != null) {
                    self.loadScript();
                }
            }).catch(function(e){
                if(currentSignType === 'nexuSign') {
                    $("#alertNexu").show();
                    $("#signLaunchButton").hide();
                    $("#second-tools").removeClass("d-flex");
                    $("#second-tools").hide();
                }
                $("#certType > option[value='nexuCert']").attr('disabled', 'disabled');
            });
        });
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
            callUrl(Nexu.rootUrl + "/user/nexu-sign/get-data-to-sign?addExtra=" + Nexu.addExtra + "&id=" + Nexu.id, "POST",  JSON.stringify(toSend), Nexu.sign, Nexu.error);
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
            callUrl(Nexu.rootUrl + "/user/nexu-sign/sign-document?id=" + Nexu.id, "POST", JSON.stringify(toSend), Nexu.downloadSignedDocument, Nexu.error);
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
            let jsonResp = error.responseJSON;
            if (jsonResp.message !=null){
                $("#errorcontent").html(jsonResp.message);
            } else if (jsonResp.errorMessage !=null){
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
        return new Promise((resolve, reject) => {
            console.log("Start checking NexU");
            let ports = this.bindingPorts.split(",");
            let detectNexu = false;
            let self = this;
            let breakOut = false;
            let i = 0;
            ports.forEach(function (port) {
                if(breakOut) {
                    return false;
                }
                let url = "http://127.0.0.1:" + port.trim() + "/nexu-info";
                console.info("check nexu on " + url);
                $.ajax({
                    type: "GET",
                    url: url,
                    crossDomain: true,
                    dataType: "json",
                    async: true,
                    cache: false,
                }).done(function (data) {
                    i++;
                    console.info("nexu detected on " + url);
                    detectNexu = true;
                    self.detectedPort = port.trim();
                    self.checkNexu(data);
                    $("#nexu_missing_alert").hide();
                    $("#noOptions").hide();
                    $("#selectTypeDiv").show();
                    breakOut = true;
                    resolve("detected");
                }).fail(function (jqXHR, textStatus, errorThrown) {
                    i++;
                    if(i === ports.length) {
                        console.debug("nexu not detected on " + url);
                        reject(0);
                    }
                });
            });
        });
    }

    checkNexu(data) {
        console.log("Check NexU");
        if(data.version.startsWith(this.nexuVersion) || data.version.startsWith("1.23") || data.version.startsWith("1.22") || data.version.startsWith("1.8")) {
            $("#nexu_ready_alert").show();
            $("#submit-button").prop('disabled', false);
        } else {
            // need update
            $("#nexu_version_alert").show();
            console.log("Bad NexU version " + data.version + " instead of " + this.nexuVersion);
        }
    }

    loadScript() {
        let url = "http://127.0.0.1:" + this.detectedPort + "/nexu.js";
        console.info("loading nexu script : " + url);
        $.getScript(url, function() {
            nexu_get_certificates(Nexu.getDataToSign, Nexu.error);
        });
    }

}
