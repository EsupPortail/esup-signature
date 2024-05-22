export class Nexu {

    constructor(addExtra, ids, currentSignType, urlProfil) {
        Nexu.urlProfil = urlProfil;
        this.globalProperties = JSON.parse(sessionStorage.getItem("globalProperties"));
        this.nexuUrl = this.globalProperties.nexuUrl;
        this.nexuVersion = this.globalProperties.nexuVersion;
        Nexu.rootUrl = this.globalProperties.rootUrl;
        Nexu.addExtra = addExtra;
        Nexu.ids = ids;
        Nexu.i = 0;
        Nexu.version;
        Nexu.certificateData;
        this.tokenId = null;
        this.keyId = null;
        this.bindingPorts = "9795";
        this.detectedPort = "";
        this.successDiv = $("#success");
        this.successDiv.hide();
        let self = this;
        $(document).ready(function() {
            self.checkNexuClient().then(function(e) {
                console.info("Esup-DSS-Client est lancé !");
                $("#nexu_missing_alert").hide();
                $("#no-options").hide();
                $("#selectTypeDiv").show();
                if(ids != null) {
                    self.loadScript();
                }
                $("#certType > option[value='nexuCert']").removeAttr('disabled');
                if(currentSignType === 'nexuSign') {
                    $("#certType").val("nexuCert");
                    $("#nexu_ready_alert").show();
                    $("#alertNexu").hide();
                    $("#signLaunchButton").show();
                    let secondTools = $("#second-tools");
                    secondTools.addClass("d-flex");
                    secondTools.show();
                }
                self.updateSignModal();
            }).catch(function(e){
                console.info("Esup-DSS-Client non lancé !");
                $("#nexu_ready_alert").hide();
                $("#certType > option[value='nexuCert']").attr('disabled', 'disabled');
                if(currentSignType === 'nexuSign') {
                    $("#alertNexu").show();
                    $("#nexu_missing_alert").show();
                    $("#signLaunchButton").hide();
                    let secondTools = $("#second-tools");
                    secondTools.removeClass("d-flex");
                    secondTools.hide();
                    $("#certType").val("");
                    alert("Esup-DSS-Client n'a pas été détecté sur le poste !");
                }
                self.updateSignModal()
            });
        });
    }

    updateSignModal() {
        $("#certType").children().each(function (e) {
            let nbOptions = $("#certType option:not([disabled])").length;
            if (nbOptions === 0) {
                // $("#nexuCheck").removeClass("d-none");
                $("#no-options").show();
                $("#signCommentDiv").hide();
                // $("#selectTypeDiv").hide();
                $("#checkValidateSignButtonEnd").hide();
                $("#checkValidateSignButtonNext").hide();
            } else {
                // $("#nexuCheck").addClass("d-none");
                $("#no-options").hide();
                $("#signCommentDiv").show();
                // $("#selectTypeDiv").show();
                $("#checkValidateSignButtonEnd").show();
                $("#checkValidateSignButtonNext").show();
            }
            if($("#certType > option[value='imageStamp']").attr('selected')) {
                $("#noSeal").show();
            }
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
            Nexu.certificateData = certificateData;
            Nexu.updateProgressBar("Préparation de la signature", "35%");
            let signingCertificate = certificateData.response.certificate;
            let certificateChain = certificateData.response.certificateChain;
            let encryptionAlgorithm = certificateData.response.encryptionAlgorithm;
            Nexu.tokenId = certificateData.response.tokenId.id;
            Nexu.keyId = certificateData.response.keyId;
            console.info("esup-dss-client version : " + Nexu.version);
            console.log("init tokenId : " + this.tokenId + "," + this.keyId);
            let url = "/nexu-sign/get-data-to-sign";
            let toSend = { signingCertificate: signingCertificate, certificateChain: certificateChain, encryptionAlgorithm: encryptionAlgorithm };
            callUrl(Nexu.rootUrl + url + "?addExtra=" + Nexu.addExtra + "&id=" + Nexu.ids[Nexu.i], "POST", JSON.stringify(toSend), Nexu.sign, Nexu.error);
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
            callUrl(Nexu.rootUrl + "/nexu-sign/sign-document?id=" + Nexu.ids[Nexu.i], "POST", JSON.stringify(toSend), Nexu.downloadSignedDocument, Nexu.error);
        } else {
            const merror = {
                errorMessage: "Erreur au moment de la signature du document"
            };
            error(Object.create(merror));
        }
    }

    static downloadSignedDocument() {
        if(Nexu.ids.length > Nexu.i + 1) {
            Nexu.i++
            $("#current-doc-num").html(Nexu.i + 1);
            Nexu.getDataToSign(Nexu.certificateData);
            // nexu_get_certificates(Nexu.getDataToSign, Nexu.error);
        } else {
            Nexu.updateProgressBar("Signature terminée", "100%");
            let bar = $('#bar');
            bar.removeClass('progress-bar-striped active');
            bar.addClass('progress-bar-success');
            if(Nexu.ids.length === 1) {
                window.location.href = "/" + Nexu.urlProfil + "/signrequests/" + Nexu.ids[0];
            } else {
                window.location.href = "/" + Nexu.urlProfil + "/signbooks";
            }
            $("#success").show();
        }
    }

    static error(error) {
        console.error(error);
        $('#bar').removeClass('progress-bar-success active').addClass('progress-bar-danger');
        if (error!= null) {
            if (error.responseJSON !=null) {
                let jsonResp = error.responseJSON;
                if (jsonResp.feedback != null && jsonResp.feedback.stacktrace != null) {
                    $("#errorcontent").html(jsonResp.feedback.stacktrace);
                    if (jsonResp.feedback.stacktrace.includes("No slot")) {
                        $("#errorText").html("Aucune clé n'a été détecté");
                    } else if (jsonResp.feedback.stacktrace.includes("keystore password was incorrect")) {
                        $("#errorText").html("Le mot de passe du keystore est incorrect");
                    } else if (jsonResp.feedback.stacktrace.includes("CKR_PIN_INCORRECT")) {
                        $("#errorText").html("Le code pin est incorrect");
                    }
                } else if (jsonResp.message != null) {
                    if (jsonResp.message.includes("is expired")) {
                        $("#errorText").html("Votre certificat est expiré");
                    } else if (jsonResp.message.includes("revoked") || jsonResp.message.includes("suspended")) {
                        $("#errorText").html("Votre certificat est révoqué");
                    }
                    $("#errorcontent").html(jsonResp.message);
                } else if (jsonResp.errorMessage != null) {
                    if (jsonResp.errorMessage.includes("The user has cancelled the operation")) {
                        $("#errorText").html("Opération annulée par l'utilisateur");
                    }
                    $("#errorcontent").html(jsonResp.errorMessage);
                } else if (jsonResp.trace != null) {
                    if (jsonResp.trace.includes("is expired")) {
                        $("#errorText").html("Votre certificat est expiré");
                    } else if (jsonResp.trace.includes("revoked") || jsonResp.message.includes("suspended")) {
                        $("#errorText").html("Votre certificat est révoqué");
                    }
                    $("#errorcontent").html(jsonResp.trace);
                } else if (jsonResp.error != null) {
                    $("#errorcontent").html(jsonResp.error);
                } else {
                    $("#errorcontent").html(JSON.stringify(jsonResp));
                }
            } else {
                $("#errorcontent").html(error.responseText);
                if(error.responseText.includes("Expired")) {
                    $("#errorText").html("Votre certificat est expiré");
                }
            }
        }
        $("#error").show();
        $("#success").hide();
        $.ajax({
            type: "POST",
            url: Nexu.rootUrl + "/nexu-sign/error?ids=" + Nexu.ids,
            crossDomain: true,
            dataType: "json",
            async: true,
            cache: false,
        });
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
                    $("#no-options").hide();
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
            $("#nexu_version_alert").show();
            console.log("bad esup-dss-client version : " + data.version);
        }
    }

    loadScript() {
        let url = "http://127.0.0.1:" + this.detectedPort + "/nexu.js";
        console.info("loading esup-dss-client script : " + url);
        $("#current-doc-num").html("1");
        $.getScript(url, function() {
            nexu_get_certificates(Nexu.getDataToSign, Nexu.error);
        });
    }

}
