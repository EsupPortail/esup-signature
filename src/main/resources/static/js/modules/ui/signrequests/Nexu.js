export class Nexu {

    constructor(addExtra, id, currentSignType) {
        this.globalProperties = JSON.parse(sessionStorage.getItem("globalProperties"));
        this.nexuUrl = this.globalProperties.nexuUrl;
        this.nexuVersion = this.globalProperties.nexuVersion;
        Nexu.rootUrl = this.globalProperties.rootUrl;
        Nexu.addExtra = addExtra;
        Nexu.id = id;
        Nexu.version;
        this.tokenId = null;
        this.keyId = null;
        this.bindingPorts = "9795, 9886, 9887, 9888";
        this.detectedPort = "";
        this.successDiv = $("#success");
        this.successDiv.hide();
        $("#warning-text").html("Esup-DSS-Client n'a pas été détecté sur le poste !");
        $("#nexu_missing_alert").show();
        let self = this;
        $(document).ready(function() {
            self.checkNexuClient().then(function(e) {
                console.info("Esup-DSS-Client est lancé !");
                $("#warning-text").html("");
                $("#nexu_missing_alert").hide();
                $("#alertNexu").remove();
                $("#noOptions").hide();
                $("#selectTypeDiv").show();
                if(id != null) {
                    self.loadScript();
                }
            }).catch(function(e){
                console.info("Esup-DSS-Client non lancé !");
                if(currentSignType === 'nexuSign') {
                    $("#alertNexu").show();
                    $("#signLaunchButton").hide();
                    let secondTools = $("#second-tools");
                    secondTools.removeClass("d-flex");
                    secondTools.hide();
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
            console.info("esup-dss-client version : " + Nexu.version);
            console.log("init tokenId : " + this.tokenId + "," + this.keyId);
            let url = "/user/nexu-sign/get-data-to-sign";
            let toSend = { signingCertificate: signingCertificate, certificateChain: certificateChain, encryptionAlgorithm: encryptionAlgorithm };
            callUrl(Nexu.rootUrl + url + "?addExtra=" + Nexu.addExtra + "&id=" + Nexu.id, "POST",  JSON.stringify(toSend), Nexu.sign, Nexu.error);
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
        let bar = $('#bar');
        bar.removeClass('progress-bar-striped active');
        bar.addClass('progress-bar-success');
        window.location.href = "/user/signrequests/" + Nexu.id;
        $("#success").show();
    }

    static error(error) {
        console.log(error);
        $('#bar').removeClass('progress-bar-success active').addClass('progress-bar-danger');
        if (error!= null && error.responseJSON !=null) {
            let jsonResp = error.responseJSON;
            if (jsonResp.trace != null) {
                $("#errorcontent").html(jsonResp.trace.split('\n')[0]);
            } else if (jsonResp.message !=null){
                $("#errorcontent").html(jsonResp.message);
            } else if (jsonResp.errorMessage !=null){
                $("#errorcontent").html(jsonResp.errorMessage);
            } else if (jsonResp.error != null){
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
            console.log("Start checking Esup-DSS-Client");
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
                    console.info("Esup-DSS-Client detected on " + url);
                    detectNexu = true;
                    self.detectedPort = port.trim();
                    self.checkNexu(data);
                    Nexu.version = data.version;
                    $("#nexu_missing_alert").hide();
                    $("#noOptions").hide();
                    $("#selectTypeDiv").show();
                    breakOut = true;
                    resolve("detected");
                }).fail(function (jqXHR, textStatus, errorThrown) {
                    if(i >= ports.length - 1) {
                        reject(0);
                    }
                    i++;
                });
            });
        });
    }

    checkNexu(data) {
        console.log("Contrôle de l'application Esup-DSS-Client");
        if(data.version.startsWith("1.0") || data.version.startsWith("1.24") || data.version.startsWith("1.23") || data.version.startsWith("1.22") || data.version.startsWith("1.8")) {
            $("#nexu_ready_alert").show();
            $("#submit-button").prop('disabled', false);
        } else {
            // need update
            $("#nexu_version_alert").show();
            console.log("bad esup-dss-client version : " + data.version);
        }
    }

    loadScript() {
        let url = "http://127.0.0.1:" + this.detectedPort + "/nexu.js";
        console.info("loading esup-dss-client script : " + url);
        $.getScript(url, function() {
            nexu_get_certificates(Nexu.getDataToSign, Nexu.error);
        });
    }

}
