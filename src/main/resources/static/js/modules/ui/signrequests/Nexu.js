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
        // mark that certificate retrieval flow ended (we got a callback)
        Nexu._retrieving = false;
        try { if (Nexu._retrievingTimeoutId) { clearTimeout(Nexu._retrievingTimeoutId); Nexu._retrievingTimeoutId = null; } } catch (er) {}
        console.debug('getDataToSign called - cleared _retrieving flag');
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
        // if user explicitly cancelled the operation, don't flood with error reports
        try {
            const jsonResp = error && error.responseJSON ? error.responseJSON : null;
                if (jsonResp != null && (jsonResp.error === 'user.cancel' || (jsonResp.errorMessage && jsonResp.errorMessage.includes('The user has cancelled')))) {
                Nexu.find('#errorText').html("Opération annulée par l'utilisateur");
                Nexu.find('#error').show();
                Nexu.find('#success').hide();
                // set cooldown to avoid immediate re-trigger (increase to 30s to be conservative)
                Nexu._userCanceled = true;
                Nexu._cancelCooldownUntil = Date.now() + (30 * 1000);
                console.debug('User cancelled - setting cooldown until', new Date(Nexu._cancelCooldownUntil).toISOString());
                // ensure we are not considered retrieving anymore
                Nexu._retrieving = false;
                console.debug('Cleared _retrieving due to user cancel');
                return;
            }
        } catch (e) {
            // ignore
        }

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
        try {
            // avoid flooding the backend when many errors happen (one report per ids set)
            Nexu._reportedErrorKeys = Nexu._reportedErrorKeys || new Set();
            const idsParam = Array.isArray(Nexu.ids) ? Nexu.ids.join(',') : (Nexu.ids ?? '');
            const key = (Nexu.massSignReportId ?? '') + '::' + idsParam;
            if (Nexu._reportedErrorKeys.has(key)) {
                console.debug('Error already reported for', key);
                return;
            }
            Nexu._reportedErrorKeys.add(key);
            // clear key after 5 minutes so we can report again later if needed
            setTimeout(() => { Nexu._reportedErrorKeys.delete(key); }, 5 * 60 * 1000);

            const url = Nexu.rootUrl + '/nexu-sign/error';
            const payload = new URLSearchParams();
            if (Nexu.massSignReportId != null && Nexu.massSignReportId !== '') payload.append('massSignReportId', Nexu.massSignReportId);
            if (idsParam !== '') payload.append('ids', idsParam);

            // Prefer navigator.sendBeacon for reliability and to avoid socket exhaustion in the browser
            if (typeof navigator !== 'undefined' && typeof navigator.sendBeacon === 'function') {
                try {
                    const blob = new Blob([payload.toString()], { type: 'application/x-www-form-urlencoded' });
                    const sent = navigator.sendBeacon(url, blob);
                    console.debug('sendBeacon result', sent, url, payload.toString());
                    return;
                } catch (e) {
                    console.warn('sendBeacon failed, will fallback to fetch', e);
                }
            }

            // fallback to fetch with keepalive which is better than many simultaneous ajax requests
            if (typeof fetch === 'function') {
                try {
                    fetch(url, {
                        method: 'POST',
                        body: payload.toString(),
                        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                        mode: 'cors',
                        credentials: 'same-origin',
                        keepalive: true
                    }).then(resp => console.debug('error report resp', resp.status)).catch(err => console.warn('error reporting failed', err));
                    return;
                } catch (e) {
                    console.warn('fetch reporting failed, will fallback to ajax', e);
                }
            }

            // final fallback to jQuery ajax (old behavior), but with short timeout and no concurrency
            $.ajax({
                type: 'POST',
                url: url + '?' + payload.toString(),
                crossDomain: true,
                dataType: 'json',
                async: true,
                cache: false,
                timeout: 5000
            }).always(function() { console.debug('legacy error report sent'); });
        } catch (e) {
            console.warn('Unable to report error to server', e);
        }
    }

    static updateProgressBar(action, percent) {
        console.log("update " + action);
        Nexu.find('#bar-text').html(action);
        const bar = Nexu.find('#bar');
        bar.width(percent);
        bar.closest('.progress').attr('aria-valuenow', Number.parseInt(percent, 10));
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

        // coordination between tabs: use BroadcastChannel when available and localStorage as fallback
        const LS_KEY = 'esup-dss-client-ready';
        let bc = null;
        try {
            if (typeof BroadcastChannel !== 'undefined') {
                bc = new BroadcastChannel('esup-dss-client');
                bc.onmessage = function(ev) {
                    try {
                        if (ev && ev.data === 'nexu-ready') {
                            console.info('Received nexu-ready from other tab');
                            startCertificateRetrieval();
                        }
                    } catch (e) {
                        console.warn('Error handling broadcast message', e);
                    }
                };
            }
        } catch (e) {
            // ignore
        }

        const startCertificateRetrieval = () => {
            // Give more time to the client to attach functions
            // if a user recently cancelled, avoid immediate re-trigger
            if (Nexu._userCanceled && Nexu._cancelCooldownUntil && Date.now() < Nexu._cancelCooldownUntil) {
                console.debug('Skipping certificate retrieval due to recent user cancel');
                return;
            }
            if (Nexu._retrieving) {
                console.debug('Certificate retrieval already in progress, skipping duplicate start');
                return;
            }

            Nexu.waitForClientFunction("nexu_get_certificates", 40, 200)
                .then(getCertificates => {
                    try {
                        // mark retrieving to avoid duplicate requests from multiple tabs
                        Nexu._retrieving = true;
                        // set a watchdog to avoid stuck retrieving state if client never calls back
                        try {
                            if (Nexu._retrievingTimeoutId) {
                                clearTimeout(Nexu._retrievingTimeoutId);
                            }
                            Nexu._retrievingTimeoutId = setTimeout(() => {
                                if (Nexu._retrieving) {
                                    Nexu._retrieving = false;
                                    console.warn('Clearing stale _retrieving flag after timeout');
                                }
                            }, 2 * 60 * 1000); // 2 minutes watchdog
                        } catch (e) {
                            // ignore
                        }
                        console.debug('Marked _retrieving = true before calling getCertificates');
                        getCertificates(Nexu.getDataToSign, Nexu.error);
                        // notify other tabs that client is ready
                        try {
                            if (bc) bc.postMessage('nexu-ready');
                            if (window.localStorage) localStorage.setItem(LS_KEY, '1');
                        } catch (e) {
                            // ignore
                        }
                    } catch (e) {
                        Nexu._retrieving = false;
                        try { if (Nexu._retrievingTimeoutId) { clearTimeout(Nexu._retrievingTimeoutId); Nexu._retrievingTimeoutId = null; } } catch (er) {}
                        console.error('Error while calling getCertificates', e);
                        console.debug('Cleared _retrieving due to exception in getCertificates');
                    }
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

        Nexu.loadClientScript(url)
            .then(() => startCertificateRetrieval())
            .catch(error => {
                const details = error?.message || "chargement du script impossible";
                Nexu.error({
                    errorMessage: "Impossible de charger le script Esup-DSS-Client : " + details
                });
            });
    }

    static loadClientScript(url) {
        if (Nexu._scriptLoadingPromise != null) {
            return Nexu._scriptLoadingPromise;
        }
        Nexu._scriptLoadingPromise = new Promise((resolve, reject) => {
            const script = document.createElement('script');
            script.type = 'text/javascript';
            script.src = url;
            script.async = true;
            script.onload = () => {
                console.debug("Esup-DSS-Client script loaded", {src: url});
                resolve();
            };
            script.onerror = () => {
                Nexu._scriptLoadingPromise = null;
                reject(new Error("erreur de chargement de " + url));
            };
            document.head.appendChild(script);
        });
        return Nexu._scriptLoadingPromise;
    }

}
