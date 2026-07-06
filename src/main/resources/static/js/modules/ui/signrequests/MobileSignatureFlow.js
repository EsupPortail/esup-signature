import NotificationCenter from "../NotificationCenter.js?version=@version@";

let activeMobileSignatureFlow = null;

export class MobileSignatureFlow {

    constructor(owner, callbacks) {
        this.owner = owner;
        this.callbacks = callbacks;
    }

    static clearActiveFlow(flow) {
        if (activeMobileSignatureFlow === flow) {
            activeMobileSignatureFlow = null;
        }
    }

    getModalElement() {
        return document.getElementById("signRequestMobileSignModal");
    }

    getModal() {
        const modalElement = this.getModalElement();
        return modalElement && window.bootstrap ? bootstrap.Modal.getOrCreateInstance(modalElement) : null;
    }

    ensureModalListeners() {
        const modalElement = this.getModalElement();
        if (modalElement == null || modalElement.dataset.mobileSignBound === "true") {
            return;
        }

        modalElement.addEventListener("shown.bs.modal", () => {
            const flow = activeMobileSignatureFlow ?? this;
            if (flow.callbacks.isTouchDevice()) {
                flow.initLocalSignaturePad();
            }
        });

        modalElement.addEventListener("hidden.bs.modal", () => {
            const flow = activeMobileSignatureFlow ?? this;
            flow.destroyLocalSignaturePad();
            if (activeMobileSignatureFlow != null) {
                if (activeMobileSignatureFlow.owner.mobilePreviewApplied) {
                    return;
                }
                activeMobileSignatureFlow.reset({clearToken: true});
                activeMobileSignatureFlow = null;
            }
        });
        modalElement.dataset.mobileSignBound = "true";
    }

    resetStatus() {
        $("#signRequestMobileSignModalStatus")
            .addClass("d-none")
            .removeClass("alert-success alert-danger alert-warning")
            .empty();
    }

    showStatus(message, level) {
        $("#signRequestMobileSignModalStatus")
            .removeClass("d-none alert-success alert-danger alert-warning")
            .addClass("alert-" + level)
            .text(message);
    }

    stopPolling() {
        if (this.owner.mobileSignPollingInterval != null) {
            window.clearInterval(this.owner.mobileSignPollingInterval);
            this.owner.mobileSignPollingInterval = null;
        }
    }

    reset({clearToken = false, force = false} = {}) {
        if (this.owner.mobilePreviewApplied && (clearToken || clearToken === undefined) && !force) {
            return;
        }
        this.stopPolling();
        this.owner.mobilePreviewRequested = false;
        this.owner.mobilePreviewApplied = false;
        this.owner.mobilePreviewFinished = false;
        this.owner.mobileSignatureSaved = false;
        this.owner.mobilePersistPromise = null;
        this.owner.mobilePersistedSignImageNumber = null;
        this.owner.lastReceivedPreviewTimestamp = null;
        if (clearToken) {
            this.owner.mobileSignToken = null;
        }
        this.resetStatus();
    }

    startPolling() {
        this.stopPolling();
        this.owner.mobileSignPollingInterval = window.setInterval(() => {
            this.pollTokenStatus();
        }, 2000);
    }

    applyPreview(signImageBase64) {
        if (!signImageBase64) {
            return;
        }

        this.callbacks.ensureCanvasReadyForMobilePreview();
        this.callbacks.loadSignatureImage(signImageBase64);
        this.callbacks.enableDragAfterMobilePreview();
        this.callbacks.showEraseButton();
        this.owner.mobilePreviewApplied = true;
        this.owner.mobilePreviewRequested = false;
        this.showStatus("Signature recue. Verifiez l'apercu dans le document.", "success");
        NotificationCenter.showSnackbar("Signature mobile chargee dans le document", "success", {delay: 3000});
        this.startPolling();
        this.getModal()?.hide();
        if (this.owner.mobilePreviewFinished) {
            this.persistPreviewIfNeeded();
        }
    }

    persistPreviewIfNeeded() {
        if (!this.owner.mobilePreviewApplied || this.owner.mobileSignatureSaved || !this.owner.mobileSignToken || !this.callbacks.hasSignatureImage()) {
            return Promise.resolve(null);
        }
        if (this.owner.mobilePersistPromise != null) {
            return this.owner.mobilePersistPromise;
        }

        const data = {
            token: this.owner.mobileSignToken,
            signImageBase64: this.callbacks.getSignatureImageBase64(),
            signRequestId: this.owner.signRequestId
        };
        if (this.owner.csrf?.parameterName && this.owner.csrf?.token) {
            data[this.owner.csrf.parameterName] = this.owner.csrf.token;
        }

        this.owner.mobilePersistPromise = new Promise((resolve, reject) => {
            $.ajax({
                url: "/ws-secure/ui/profile/signatures/mobile",
                type: "POST",
                data: data,
                success: response => {
                    const fallbackNumber = Array.isArray(response?.signImageIds) && response.signImageIds.length > 0
                        ? response.signImageIds.length - 1
                        : null;
                    const savedSignImageNumber = Number.parseInt(response?.savedSignImageNumber ?? fallbackNumber, 10);
                    if (!Number.isFinite(savedSignImageNumber)) {
                        this.owner.mobilePersistPromise = null;
                        reject(new Error("Unable to resolve saved mobile signature index"));
                        return;
                    }

                    this.owner.mobileSignatureSaved = true;
                    this.owner.mobilePersistedSignImageNumber = savedSignImageNumber;
                    this.owner.signImageNumber = savedSignImageNumber;
                    this.owner.signImages = Array.isArray(response?.signImages) ? response.signImages : this.owner.signImages;
                    localStorage.setItem("signNumber", savedSignImageNumber);
                    document.dispatchEvent(new CustomEvent("userSignatureUpdated", {detail: response}));
                    Promise.resolve(this.callbacks.changeSignImage(savedSignImageNumber))
                        .then(() => {
                            this.stopPolling();
                            this.showStatus("Signature mobile enregistree.", "success");
                            resolve(response);
                        })
                        .catch(error => {
                            this.owner.mobilePersistPromise = null;
                            reject(error);
                        });
                },
                error: xhr => {
                    this.owner.mobilePersistPromise = null;
                    let message = "Erreur lors de l'enregistrement de la signature mobile.";
                    if (xhr.responseJSON && xhr.responseJSON.message) {
                        message = xhr.responseJSON.message;
                    }
                    this.showStatus(message, "warning");
                    NotificationCenter.showSnackbar(message, "warning", {delay: 4000});
                    reject(new Error(message));
                }
            });
        });

        return this.owner.mobilePersistPromise;
    }

    fetchPreview() {
        if (!this.owner.mobileSignToken || this.owner.mobilePreviewRequested) {
            return;
        }

        this.owner.mobilePreviewRequested = true;
        $.ajax({
            url: "/public/mobile-sign/" + encodeURIComponent(this.owner.mobileSignToken) + "/preview",
            type: "GET",
            success: response => {
                this.owner.mobilePreviewRequested = false;
                if (response && response.success && response.signImageBase64) {
                    this.applyPreview(response.signImageBase64);
                    return;
                }
                this.showStatus("La signature mobile a ete detectee mais son apercu est indisponible.", "warning");
            },
            error: xhr => {
                this.owner.mobilePreviewRequested = false;
                let message = "Erreur lors de la recuperation de la signature mobile.";
                if (xhr.responseJSON && xhr.responseJSON.message) {
                    message = xhr.responseJSON.message;
                }
                this.showStatus(message, "warning");
            }
        });
    }

    pollTokenStatus() {
        if (!this.owner.mobileSignToken) {
            return;
        }

        $.ajax({
            url: "/public/mobile-sign/" + encodeURIComponent(this.owner.mobileSignToken) + "/status",
            type: "GET",
            success: response => {
                if (response.used) {
                    this.stopPolling();
                    if (!this.owner.mobileSignatureSaved) {
                        this.showStatus("Ce lien a deja ete utilise. Generez-en un nouveau si besoin.", "warning");
                    }
                    return;
                }
                if (response.finished) {
                    this.owner.mobilePreviewFinished = true;
                }
                if (response.previewAvailable && !this.owner.mobilePreviewRequested) {
                    if (response.previewTimestamp != null && response.previewTimestamp !== this.owner.lastReceivedPreviewTimestamp) {
                        this.owner.lastReceivedPreviewTimestamp = response.previewTimestamp;
                        this.fetchPreview();
                    }
                }
                if (response.finished && this.owner.mobilePreviewApplied) {
                    this.persistPreviewIfNeeded();
                }
                if (!response.valid) {
                    this.stopPolling();
                    this.showStatus(response.message || "Ce lien n'est plus valide. Veuillez en generer un nouveau.", "warning");
                }
            },
            error: () => {
                this.stopPolling();
                this.showStatus("Erreur lors de la verification du lien mobile.", "warning");
            }
        });
    }

    destroyLocalSignaturePad() {
        if (this.owner.localSignaturePad && typeof this.owner.localSignaturePad.destroy === 'function') {
            this.owner.localSignaturePad.destroy();
        }
        this.owner.localSignaturePad = null;
    }

    initLocalSignaturePad() {
        const canvas = document.getElementById("canvasMobile");
        if (!canvas) return;

        const ratio = Math.max(window.devicePixelRatio || 1, 1);
        canvas.width = canvas.offsetWidth * ratio;
        canvas.height = canvas.offsetHeight * ratio;
        canvas.getContext("2d").scale(ratio, ratio);

        this.destroyLocalSignaturePad();

        this.owner.localSignaturePad = new SignaturePad(canvas);

        const eraseButton = document.getElementById("eraseMobile");
        const validateButton = document.getElementById("validateMobile");

        if (eraseButton) {
            $(eraseButton).off("click").on("click", () => {
                this.owner.localSignaturePad.clear();
            });
        }

        if (validateButton) {
            $(validateButton).off("click").on("click", () => {
                if (this.owner.localSignaturePad.isEmpty()) {
                    alert("Veuillez dessiner votre signature.");
                    return;
                }
                const signImageBase64 = this.owner.localSignaturePad.toDataURL("image/png");
                this.applyPreview(signImageBase64);
                this.getModal()?.hide();
            });
        }
    }

    start() {
        if (activeMobileSignatureFlow != null && activeMobileSignatureFlow !== this) {
            activeMobileSignatureFlow.reset({clearToken: true});
        }
        activeMobileSignatureFlow = this;
        this.reset({clearToken: true});

        if (this.callbacks.isTouchDevice()) {
            $("#signRequestMobileSignModalLocal").removeClass("d-none");
            $("#signRequestMobileSignModalQR").addClass("d-none");
            this.getModal()?.show();
            return;
        }

        let url = (this.owner.isOtp ? "/otp" : "/user") + "/signrequests/" + this.owner.signRequestId + "/generate-mobile-token";
        $.ajax({
            url: url,
            type: "GET",
            success: response => {
                if (!(response && response.qrcodeUrl && response.token)) {
                    this.showStatus("Erreur lors de la generation du lien mobile. Veuillez reessayer.", "danger");
                    return;
                }

                this.owner.mobileSignToken = response.token;
                $("#signRequestQrcodeImage").attr("src", response.qrcodeUrl);
                $("#signRequestMobileSignUrl").attr("href", response.url).text(response.url);
                this.resetStatus();
                this.getModal()?.show();
                this.startPolling();
                this.pollTokenStatus();
            },
            error: () => {
                this.showStatus("Erreur lors de la generation du lien mobile. Veuillez reessayer.", "danger");
                this.getModal()?.show();
            }
        });
    }
}
