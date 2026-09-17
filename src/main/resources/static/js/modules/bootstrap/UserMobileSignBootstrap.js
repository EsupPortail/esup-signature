$(document).ready(function() {
    var checkInterval = null;
    var currentToken = null;
    var previewApplied = false;
    var previewRequested = false;
    var previewSignatureBase64 = null;
    var lastReceivedPreviewTimestamp = null;

    var modalElement = document.getElementById('mobileSignModal');
    var mobileSignModal = modalElement && window.bootstrap ? bootstrap.Modal.getOrCreateInstance(modalElement) : null;
    var canvasElement = document.getElementById('canvas');
    var signPadLabelElement = document.getElementById('signPadLabel');
    var localSignaturePad = null;

    function isTouchDevice() {
        return (('ontouchstart' in window) ||
            (navigator.maxTouchPoints > 0) ||
            (navigator.msMaxTouchPoints > 0));
    }

    function initLocalMobileSignaturePad() {
        const canvas = document.getElementById("canvasMobile");
        if (!canvas) return;

        const ratio = Math.max(window.devicePixelRatio || 1, 1);
        canvas.width = canvas.offsetWidth * ratio;
        canvas.height = canvas.offsetHeight * ratio;
        canvas.getContext("2d").scale(ratio, ratio);

        if (localSignaturePad && typeof localSignaturePad.destroy === 'function') {
            localSignaturePad.destroy();
        }

        localSignaturePad = new SignaturePad(canvas);

        const eraseButton = document.getElementById("eraseMobile");
        const validateButton = document.getElementById("validateMobile");

        if (eraseButton) {
            $(eraseButton).off("click").on("click", () => {
                localSignaturePad.clear();
            });
        }

        if (validateButton) {
            $(validateButton).off("click").on("click", () => {
                if (localSignaturePad.isEmpty()) {
                    alert("Veuillez dessiner votre signature.");
                    return;
                }
                const signImageBase64 = localSignaturePad.toDataURL("image/png");
                applySignaturePreview(signImageBase64);
                mobileSignModal?.hide();
            });
        }
    }

    function stopPolling() {
        if (checkInterval !== null) {
            window.clearInterval(checkInterval);
            checkInterval = null;
        }
    }

    function showModalStatus(message, type) {
        var statusElement = $('#mobileSignModalStatus');
        statusElement.removeClass('d-none alert-success alert-danger alert-warning');
        statusElement.addClass('alert-' + type);
        statusElement.text(message);
    }

    $('#drawOnMobileBtn').on('click', function() {
        clearPreviewState({clearToken: true});
        resetModalStatus();

        if (isTouchDevice()) {
            $('#mobileSignModalLocal').removeClass('d-none');
            $('#mobileSignModalQR').addClass('d-none');
            if (mobileSignModal) {
                mobileSignModal.show();
            }
            return;
        }

        $('#mobileSignModalLocal').addClass('d-none');
        $('#mobileSignModalQR').removeClass('d-none');

        const isOtp = window.location.pathname.startsWith('/otp');
        const tokenUrl = isOtp ? '/otp/users/mobile-sign/generate-token' : '/user/users/mobile-sign/generate-token';
        $.ajax({
            url: tokenUrl,
            type: 'GET',
            success: function(response) {
                if (response && response.qrcodeUrl && response.token) {
                    currentToken = response.token;
                    $('#qrcodeImage').attr('src', response.qrcodeUrl);
                    $('#mobileSignUrl').attr('href', response.url).text(response.url);
                    if (mobileSignModal) {
                        mobileSignModal.show();
                    }
                    startSignaturePolling();
                    pollTokenStatus();
                } else {
                    if (mobileSignModal) {
                        mobileSignModal.show();
                    }
                    showModalStatus('Erreur lors de la génération du lien mobile. Veuillez réessayer.', 'danger');
                }
            },
            error: function() {
                if (mobileSignModal) {
                    mobileSignModal.show();
                }
                showModalStatus('Erreur lors de la génération du lien mobile. Veuillez réessayer.', 'danger');
            }
        });
    });

    function lockLocalDrawing() {
        if (canvasElement) {
            canvasElement.style.pointerEvents = 'none';
            canvasElement.style.opacity = '0.6';
        }
        if (signPadLabelElement) {
            signPadLabelElement.textContent = 'Une signature mobile a été chargée. Réinitialisez la signature pour dessiner à nouveau ici.';
        }
    }

    function unlockLocalDrawing() {
        if (canvasElement) {
            canvasElement.style.pointerEvents = '';
            canvasElement.style.opacity = '';
        }
        if (signPadLabelElement) {
            signPadLabelElement.textContent = 'Vous pouvez dessiner une signature dans le rectangle ci dessous';
        }
    }

    function startSignaturePolling() {
        stopPolling();
        checkInterval = window.setInterval(function() {
            pollTokenStatus();
        }, 2000);
    }

    function clearPreviewState(options) {
        var shouldClearToken = options && (options.clearToken === true || options.clearToken === undefined);
        var force = options && options.force === true;
        if (previewApplied && shouldClearToken && !force) {
            return;
        }
        stopPolling();
        previewApplied = false;
        previewRequested = false;
        previewSignatureBase64 = null;
        lastReceivedPreviewTimestamp = null;
        if (shouldClearToken) {
            currentToken = null;
        }
        $('#mobileSignToken').val('');
        unlockLocalDrawing();
    }

    function applySignaturePreview(signImageBase64) {
        if (!signImageBase64) {
            return;
        }

        if (!window.userUi || typeof window.userUi.applyMobileSignaturePreview !== 'function') {
            showModalStatus('Signature reçue mais impossible de l’afficher automatiquement sur cette page.', 'warning');
            return;
        }

        previewApplied = true;
        previewRequested = false;
        previewSignatureBase64 = signImageBase64;
        $('#mobileSignToken').val(currentToken || '');
        window.userUi.applyMobileSignaturePreview(signImageBase64);
        lockLocalDrawing();
        showModalStatus('Signature reçue. Vérifiez l’aperçu sur cette page puis utilisez le bouton d’enregistrement.', 'success');
        startSignaturePolling();
        if (mobileSignModal) {
            mobileSignModal.hide();
        }
    }

    function resetModalStatus() {
        $('#mobileSignModalStatus').addClass('d-none').removeClass('alert-success alert-danger alert-warning').empty();
    }

    function fetchSignaturePreview() {
        if (!currentToken || previewRequested) {
            return;
        }

        previewRequested = true;
        $.ajax({
            url: '/public/mobile-sign/' + encodeURIComponent(currentToken) + '/preview',
            type: 'GET',
            success: function(response) {
                previewRequested = false;
                if (response && response.success && response.signImageBase64) {
                    applySignaturePreview(response.signImageBase64);
                    return;
                }
                showModalStatus('La signature mobile a été détectée, mais son aperçu est indisponible.', 'warning');
            },
            error: function(xhr) {
                previewRequested = false;
                var message = 'Erreur lors de la récupération de la signature mobile.';
                if (xhr && xhr.responseJSON && xhr.responseJSON.message) {
                    message = xhr.responseJSON.message;
                }
                showModalStatus(message, 'warning');
            }
        });
    }

    function pollTokenStatus() {
        if (!currentToken) {
            return;
        }

        $.ajax({
            url: '/public/mobile-sign/' + encodeURIComponent(currentToken) + '/status',
            type: 'GET',
            success: function(response) {
                if (response && response.used) {
                    stopPolling();
                    showModalStatus('Ce lien a déjà été utilisé. Générez-en un nouveau si besoin.', 'warning');
                    return;
                }

                if (response && response.previewAvailable && !previewRequested) {
                    if (response.previewTimestamp != null && response.previewTimestamp !== lastReceivedPreviewTimestamp) {
                        lastReceivedPreviewTimestamp = response.previewTimestamp;
                        fetchSignaturePreview();
                    }
                }

                if (response && response.valid === false) {
                    stopPolling();
                    showModalStatus(response.message || 'Ce lien n’est plus valide. Veuillez en générer un nouveau.', 'warning');
                }
            },
            error: function() {
                stopPolling();
                showModalStatus('Erreur lors de la vérification du lien mobile.', 'warning');
            }
        });
    }

    $('#signImageBase64').on('input change', function() {
        if (!previewApplied || !previewSignatureBase64) {
            return;
        }

        var currentSignatureValue = $(this).val() || '';
        if (currentSignatureValue !== previewSignatureBase64) {
            clearPreviewState();
        }
    });

    $('#erase').on('click', function() {
        clearPreviewState();
    });

    if (modalElement && modalElement.dataset.mobileSignBound !== 'true') {
        modalElement.addEventListener('shown.bs.modal', function() {
            if (isTouchDevice()) {
                initLocalMobileSignaturePad();
            }
        });
        modalElement.addEventListener('hidden.bs.modal', function() {
            if (localSignaturePad && typeof localSignaturePad.destroy === 'function') {
                localSignaturePad.destroy();
            }
            localSignaturePad = null;
            if (previewApplied) {
                return;
            }
            stopPolling();
            previewRequested = false;
            currentToken = null;
            resetModalStatus();
        });
        modalElement.dataset.mobileSignBound = 'true';
    }

    var addSignatureModalElement = document.getElementById('add-signature-modal');
    if (addSignatureModalElement) {
        addSignatureModalElement.addEventListener('hidden.bs.modal', function() {
            clearPreviewState({clearToken: true, force: true});
            resetModalStatus();
        });
    }
});
