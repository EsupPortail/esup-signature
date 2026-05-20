export class Nexu {

    constructor(addExtra, ids, currentSignType, urlProfil, massSignReportId, rootUrl, rootElement = null) {
        Nexu.urlProfil = urlProfil;
        if(massSignReportId != null) {
            Nexu.massSignReportId = massSignReportId;
        } else {
            Nexu.massSignReportId = "";
        }
        Nexu.rootUrl = rootUrl ?? window.location.origin;
        Nexu.addExtra = addExtra;
        Nexu.ids = ids;
        Nexu.rootElement = rootElement instanceof jQuery ? rootElement.get(0) : rootElement;
        Nexu.i = 0;
        Nexu.version;
        Nexu.certificateData;
        this.tokenId = null;
        this.keyId = null;
        this.bindingPorts = "9795";
        this.detectedPort = "";
        this.successDiv = Nexu.find("#success");
        this.successDiv.hide();
        let self = this;
        $(document).ready(function() {
            self.checkNexuClient().then(function(e) {
                console.info("Esup-DSS-Client est lancé !");
                $("#nexu_missing_alert").hide();
                $("#no-options").hide();
                $("#no-options-alert").hide();
                $("#selectTypeDiv").show();
                if(ids != null) {
                    self.loadScript();
                }
                // $("#certType > option[value='nexuCert']").removeAttr('disabled');
                if(currentSignType === 'nexuSign') {
                    $("#certType").val("nexuCert").trigger("change");
                    $("#nexu_ready_alert").show();
                    Nexu.find("#alertNexu").hide();
                    $("#signLaunchButton").show();
                }
                self.updateSignModal();
                $("#certType > option[value='nexuCert']").removeAttr('unavailable');
            }).catch(function(e){
                console.info("Esup-DSS-Client non lancé !");
                $("#nexu_ready_alert").hide();
                Nexu.find("#alertNexu").show();
                $("#nexu_missing_alert").show();
                self.updateSignModal()
                $("#certType > option[value='nexuCert']").attr('unavailable', 'unavailable');
            });
        });
    }

    static getRootElement() {
        return Nexu.rootElement ?? null;
    }

    static find(selector) {
        const rootElement = Nexu.getRootElement();
        if (rootElement == null) {
            return $(selector);
        }
        const root = $(rootElement);
        if (root.is(selector)) {
            return root;
        }
        const scopedMatch = root.find(selector);
        return scopedMatch.length ? scopedMatch : $(selector);
    }

    static getGlobalScope() {
        if (typeof globalThis !== "undefined") {
            return globalThis;
        }
        if (typeof window !== "undefined") {
            return window;
        }
        return {};
    }

    static getClientFunction(functionName) {
        const globalScope = Nexu.getGlobalScope();
        return typeof globalScope[functionName] === "function"
            ? globalScope[functionName]
            : null;
    }

    static waitForClientFunction(functionName, maxAttempts = 20, delayMs = 150) {
        return new Promise((resolve, reject) => {
            let attempts = 0;

            const checkFunction = () => {
                const clientFunction = Nexu.getClientFunction(functionName);
                if (clientFunction != null) {
                    resolve(clientFunction);
                    return;
                }

                attempts += 1;
                if (attempts >= maxAttempts) {
                    reject(new Error("Esup-DSS-Client ne fournit pas la fonction " + functionName));
                    return;
                }

                window.setTimeout(checkFunction, delayMs);
            };

            checkFunction();
        });
    }

    static reportMissingClientFunction(functionName) {
        Nexu.error({
            errorMessage: "La fonction " + functionName + " n’a pas été fournie par Esup-DSS-Client. Vérifiez que l’application est bien démarrée, compatible, puis relancez la procédure."
        });
    }

    updateSignModal() {
        $("#certType").children().each(function (e) {
            let nbOptions = $("#certType option:not(:disabled):not([unavailable])").length;
            if (nbOptions === 0) {
                // $("#nexuCheck").removeClass("d-none");
                $("#no-options").show();
                $("#no-options-alert").show();
                $("#signCommentDiv").hide();
                $("#signGotoNextContainer").hide();
                // $("#selectTypeDiv").hide();
                $("#checkValidateAdvancedSignButton").hide();
                $("#launchNoInfiniteSignButton").hide();
                $("#launch-infinite-sign-button").hide();
                $("#checkValidateSignButtonEnd").hide();
                $("#checkValidateSignButtonNext").hide();
            } else {
                // $("#nexuCheck").addClass("d-none");
                $("#no-options").hide();
                $("#no-options-alert").hide();
                $("#signCommentDiv").show();
                $("#signGotoNextContainer").show();
                // $("#selectTypeDiv").show();
                $("#checkValidateAdvancedSignButton").show();
                $("#launchNoInfiniteSignButton").show();
                $("#launch-infinite-sign-button").show();
                $("#checkValidateSignButtonEnd").show();
                $("#checkValidateSignButtonNext").show();
            }
            if($("#certType").val() === "imageStamp") {
                $("#noSeal").show();
            }
        });
    }

    static getDataToSign(certificateData) {
        if(certificateData.response == null) {
            const merror = {
                errorMessage: "Erreur au moment de lire le certificat"
            };
            Nexu.error(Object.create(merror));
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
            callUrl(Nexu.rootUrl + url + "?massSignReportId=" + Nexu.massSignReportId + "&addExtra=" + Nexu.addExtra + "&id=" + Nexu.ids[Nexu.i], "POST", JSON.stringify(toSend), Nexu.sign, Nexu.error);
        }
    }

    static sign(dataToSignResponse) {
        if (dataToSignResponse == null) {
            const merror = {
                errorMessage: "Erreur lors de la vérification du certificat"
            };
            Nexu.error(Object.create(merror));
        } else {
            Nexu.updateProgressBar("Signature du/des documents(s)", "50%");
            console.log("token : " + Nexu.tokenId + "," + Nexu.keyId);
            let digestAlgo = "SHA256";
            const signWithTokenInfos = Nexu.getClientFunction("nexu_sign_with_token_infos");
            if (signWithTokenInfos == null) {
                Nexu.reportMissingClientFunction("nexu_sign_with_token_infos");
                return;
            }
            signWithTokenInfos(Nexu.tokenId, Nexu.keyId, dataToSignResponse.dataToSign, digestAlgo, Nexu.signDocument, Nexu.error);
        }
    }

    static signDocument(signatureData) {
        Nexu.updateProgressBar("Enregistrement du/des documents(s)", "75%");
        if(signatureData.response != null) {
            let signatureValue = signatureData.response.signatureValue;
            let toSend = {signatureValue: signatureValue};
            callUrl(Nexu.rootUrl + "/nexu-sign/sign-document?massSignReportId=" + Nexu.massSignReportId + "&id=" + Nexu.ids[Nexu.i], "POST", JSON.stringify(toSend), Nexu.downloadSignedDocument, Nexu.error);
        } else {
            const merror = {
                errorMessage: "Erreur au moment de la signature du document"
            };
            Nexu.error(Object.create(merror));
        }
    }

    static downloadSignedDocument() {
        if(Nexu.ids.length > Nexu.i + 1) {
            Nexu.i++
            Nexu.find("#current-doc-num").html(Nexu.i + 1);
            Nexu.getDataToSign(Nexu.certificateData);
            // nexu_get_certificates(Nexu.getDataToSign, Nexu.error);
        } else {
            Nexu.updateProgressBar("Signature terminée", "100%");
            let bar = Nexu.find('#bar');
            bar.removeClass('progress-bar-striped active');
            bar.addClass('progress-bar-success');
            if(Nexu.ids.length === 1) {
                window.location.href = "/" + Nexu.urlProfil + "/signrequests/" + Nexu.ids[0];
            } else {
                window.location.href = "/" + Nexu.urlProfil + "/signbooks";
            }
            Nexu.find("#success").show();
        }
    }

    static error(error) {
        console.error(error);
        Nexu.find('#bar').removeClass('progress-bar-success active').addClass('progress-bar-danger');
        if (error!= null) {
            if (error.responseJSON !=null) {
                let jsonResp = error.responseJSON;
                console.error(jsonResp);
                if (jsonResp.feedback != null && jsonResp.feedback.stacktrace != null) {
                    Nexu.find("#errorcontent").html(jsonResp.feedback.stacktrace);
                    if (jsonResp.feedback.stacktrace.includes("No slot")) {
                        Nexu.find("#errorText").html("Aucune clé n'a été détecté");
                    } else if (jsonResp.feedback.stacktrace.includes("keystore password was incorrect")) {
                        Nexu.find("#errorText").html("Le mot de passe du keystore est incorrect");
                    } else if (jsonResp.feedback.stacktrace.includes("CKR_PIN_INCORRECT")) {
                        Nexu.find("#errorText").html("Le code PIN est incorrect. Attention, après 3 essais infructueux, votre certificat sera définitivement bloqué !");
                    }
                } else if (jsonResp.message != null) {
                    if (jsonResp.message.includes("is expired")) {
                        Nexu.find("#errorText").html("Votre certificat est expiré");
                    } else if (jsonResp.message.includes("revoked") || jsonResp.message.includes("suspended")) {
                        Nexu.find("#errorText").html("Votre certificat est révoqué");
                    }
                    Nexu.find("#errorcontent").html(jsonResp.message);
                } else if (jsonResp.errorMessage != null) {
                    if (jsonResp.errorMessage.includes("The user has cancelled the operation")) {
                        Nexu.find("#errorText").html("Opération annulée par l'utilisateur");
                    }
                    Nexu.find("#errorcontent").html(jsonResp.errorMessage);
                } else if (jsonResp.trace != null) {
                    if (jsonResp.trace.includes("is expired")) {
                        Nexu.find("#errorText").html("Votre certificat est expiré");
                    } else if (jsonResp.trace.includes("revoked") || (jsonResp.message != null && jsonResp.message.includes("suspended"))) {
                        Nexu.find("#errorText").html("Votre certificat est révoqué");
                    } else {
                        Nexu.find("#errorText").html("Erreur indéterminée : " + jsonResp.message);
                    }
                    Nexu.find("#errorcontent").html(jsonResp.trace);
                } else if (jsonResp.error != null) {
                    Nexu.find("#errorcontent").html(jsonResp.error);
                } else {
                    Nexu.find("#errorcontent").html(JSON.stringify(jsonResp));
                }
            } else {
                const responseText = error.responseText ?? error.errorMessage ?? "";
                Nexu.find("#errorcontent").html(responseText);
                if(responseText.includes("Expired")) {
                    Nexu.find("#errorText").html("Votre certificat est expiré");
                } else if (error.errorMessage != null && error.errorMessage !== "") {
                    Nexu.find("#errorText").html("Erreur lors de la communication avec Esup-DSS-Client");
                }
            }
        }
        Nexu.find("#error").show();
        Nexu.find("#success").hide();
        $.ajax({
            type: "POST",
            url: Nexu.rootUrl + "/nexu-sign/error?massSignReportId=" + Nexu.massSignReportId + "&ids=" + Nexu.ids,
            crossDomain: true,
            dataType: "json",
            async: true,
            cache: false,
        });
    }

    static updateProgressBar(action, percent) {
        console.log("update " + action);
        Nexu.find('#bar-text').html(action);
        Nexu.find('#bar').width(percent);
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
                    $("#no-options-alert").hide();
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
        Nexu.find("#current-doc-num").html("1");

        const startCertificateRetrieval = () => {
            Nexu.waitForClientFunction("nexu_get_certificates")
                .then(getCertificates => {
                    getCertificates(Nexu.getDataToSign, Nexu.error);
                })
                .catch(error => {
                    console.error("Esup-DSS-Client API unavailable", error);
                    Nexu.reportMissingClientFunction("nexu_get_certificates");
                });
        };

        if (Nexu.getClientFunction("nexu_get_certificates") != null) {
            startCertificateRetrieval();
            return;
        }

        $.getScript(url).done(function() {
            startCertificateRetrieval();
        }).fail(function(jqXHR, textStatus, errorThrown) {
            const details = errorThrown || textStatus || "chargement du script impossible";
            Nexu.error({
                errorMessage: "Impossible de charger le script Esup-DSS-Client : " + details
            });
        });
    }

}
