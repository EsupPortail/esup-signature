const userParamsForm = document.getElementById('userParamsForm');
                    const saveButton = document.getElementById('saveButton');
                    const signWithGeneratedButton = document.getElementById('signWithGeneratedButton');
                    const nameInput = document.getElementById('name');
                    const firstnameInput = document.getElementById('firstname');
                    const userParamsIdInput = document.getElementById('user-params-id');
                    const generatedSignaturePreviewImage = document.getElementById('generated-signature-preview-image');
                    const generatedSignaturePreviewEmpty = document.getElementById('generated-signature-preview-empty');
                    const signDivElement = document.getElementById('draw-pane');
                    const eraseButton = document.getElementById('erase');
                    const cancelNewSignBtn = document.getElementById('cancel-new-sign-btn');
                    const drawOnMobileButton = document.getElementById('drawOnMobileBtn');
                    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
                    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';
                    if (userParamsForm && saveButton) {
                    const currentUserType = document.body.dataset.esupUserType || 'user';
                    const userType = currentUserType === 'external' ? 'otp' : 'user';
                    const addSignImageModal = document.getElementById('add-sign-image');
                    const modalMobileSignToken = document.getElementById('modalMobileSignToken');
                    const mobileSignModalElement = document.getElementById('signRequestMobileSignModal');
                    const previewTab = document.getElementById('preview-tab');
                    const drawTab = document.getElementById('draw-tab');
                    const uploadTab = document.getElementById('upload-tab');
                    const mobileSignModal = mobileSignModalElement && window.bootstrap ? bootstrap.Modal.getOrCreateInstance(mobileSignModalElement) : null;
                        const signRequestId = addSignImageModal?.dataset.signRequestId || null;
                        if (signRequestId) {
                            addSignImageModal.addEventListener('shown.bs.modal', () => {
                                const activeTab = document.querySelector('#signTabs .nav-link.active');
                                if (activeTab && activeTab.id === 'draw-tab') {
                                    window.dispatchEvent(new Event('resize'));
                                }
                            });

                            document.querySelectorAll('#signTabs button[data-bs-toggle="tab"]').forEach(tabEl => {
                                tabEl.addEventListener('shown.bs.tab', (event) => {
                                    if (event.target.id === 'draw-tab') {
                                        window.dispatchEvent(new Event('resize'));
                                    }
                                });
                            });
                        }
                    const modalSignaturesContainer = document.getElementById('modal-signatures-container');
                    const navbarUserSignaturesContent = document.getElementById('navbar-user-signatures-content');
                    const initialSignImages = JSON.parse(document.getElementById('signrequest-sign-images-json')?.value || '[]');
                    let currentUserSignatureState = null;
                    let mobileCheckInterval = null;
                    let currentMobileToken = null;
                    let mobilePreviewApplied = false;
                    let mobilePreviewRequested = false;
                    let mobilePreviewSignatureBase64 = null;
                    let lastReceivedPreviewTimestamp = null;
                    let localSignaturePad = null;

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
                                applyMobileSignaturePreview(signImageBase64);
                                mobileSignModal?.hide();
                            });
                        }
                    }

                    function updateSaveButtonState() {
                        const activeTab = document.querySelector('#signTabs .nav-link.active');
                        if (!activeTab) return;
                        const signPad = document.getElementById('signPad');
                        const vanillaUpload = document.getElementById('vanilla-upload');
                        const cropDiv = document.getElementById('crop-div');
                        const signImageBase64 = document.getElementById('signImageBase64');
                        const hasSignaturePayload = `${signImageBase64?.value || ''}`.trim() !== '';

                        if (activeTab.id === 'preview-tab' || (activeTab.id === 'draw-tab' && (!signPad || signPad.classList.contains('d-none') || signPad.style.display === 'none'))) {
                            saveButton.classList.add('d-none');
                            saveButton.disabled = true;
                        } else if (activeTab.id === 'upload-tab' && (!vanillaUpload || vanillaUpload.value === '') && (!cropDiv || cropDiv.style.display === 'none')) {
                            saveButton.classList.add('d-none');
                            saveButton.disabled = true;
                        } else {
                            saveButton.classList.remove('d-none');
                            saveButton.innerHTML = '<i class="fi fi-rr-check"></i> Utiliser cette signature';
                            saveButton.disabled = !hasSignaturePayload;
                        }
                    }

                    function autoSave() {
                        if (nameInput.value !== "" && firstnameInput.value !== "") {
                            const formData = new FormData(userParamsForm);
                            const submitUrl = '/ws-secure/ui/profile';
                            if (signRequestId) {
                                formData.append('signRequestId', signRequestId);
                            }
                            fetch(submitUrl, {
                                method: 'POST',
                                headers: {
                                    [csrfHeader]: csrfToken,
                                    'X-Requested-With': 'XMLHttpRequest',
                                    'Accept': 'application/json'
                                },
                                body: formData
                            })
                            .then(async response => {
                                if (response.ok) {
                                    updateLiveUserState(await response.json());
                                    dispatchDocumentEvent('userSignatureUpdated', currentUserSignatureState);
                                }
                            });
                        }
                    }

                    function readUiMe() {
                        try {
                            return JSON.parse(sessionStorage.getItem('uiMe') || 'null');
                        } catch (e) {
                            return null;
                        }
                    }

                    const getUiProfile = uiMe => uiMe?.authUser || uiMe?.user || null;
                    const dispatchDocumentEvent = (name, detail) => document.dispatchEvent(new CustomEvent(name, {detail}));

                    function hydrateModalFromUiMe(uiMe) {
                        const profile = getUiProfile(uiMe);
                        if (!profile) {
                            return;
                        }
                        if (userParamsIdInput && profile.id != null) {
                            userParamsIdInput.value = profile.id;
                        }
                        if (nameInput && !nameInput.value) {
                            nameInput.value = profile.name || '';
                        }
                        if (firstnameInput && !firstnameInput.value) {
                            firstnameInput.value = profile.firstname || '';
                        }
                        updateLiveUserState({
                            ...currentUserSignatureState,
                            firstname: firstnameInput?.value || profile.firstname || '',
                            name: nameInput?.value || profile.name || '',
                            email: profile.email || currentUserSignatureState?.email || '',
                            signImageIds: Array.isArray(uiMe?.userImagesIds) ? uiMe.userImagesIds : (currentUserSignatureState?.signImageIds || []),
                            signImages: currentUserSignatureState?.signImages || initialSignImages
                        });
                    }

                    function enrichUserState(userState) {
                        if (!userState) {
                            return null;
                        }
                        const signImageIds = Array.isArray(userState.signImageIds) ? userState.signImageIds : [];
                        const signImages = Array.isArray(userState.signImages) ? userState.signImages : [];
                        const generatedSignImageNumber = signImages.length > signImageIds.length ? signImageIds.length : null;
                        const parapheSignImageNumber = generatedSignImageNumber != null && signImages.length > generatedSignImageNumber + 1
                            ? generatedSignImageNumber + 1
                            : null;
                        return {
                            ...userState,
                            signImageIds,
                            signImages,
                            generatedSignImageNumber,
                            parapheSignImageNumber
                        };
                    }

                    function getGeneratedSignaturePreviewSrc(userState = currentUserSignatureState) {
                        const generatedIndex = userState?.generatedSignImageNumber;
                        const generatedImage = generatedIndex != null ? userState?.signImages?.[generatedIndex] : null;
                        if (generatedImage) {
                            return `data:image/png;charset=utf-8;base64, ${generatedImage}`;
                        }
                        if (userState?.defaultSignImage) {
                            return `data:image/png;charset=utf-8;base64, ${userState.defaultSignImage}`;
                        }
                        return null;
                    }

                    function updateGeneratedSignaturePreview(userState = currentUserSignatureState) {
                        if (!generatedSignaturePreviewImage || !generatedSignaturePreviewEmpty) {
                            return;
                        }
                        const previewSrc = getGeneratedSignaturePreviewSrc(userState);
                        if (previewSrc) {
                            generatedSignaturePreviewImage.src = previewSrc;
                            generatedSignaturePreviewImage.classList.remove('d-none');
                            generatedSignaturePreviewEmpty.classList.add('d-none');
                        } else {
                            generatedSignaturePreviewImage.removeAttribute('src');
                            generatedSignaturePreviewImage.classList.add('d-none');
                            generatedSignaturePreviewEmpty.classList.remove('d-none');
                        }
                    }

                    function validateIdentityFields() {
                        const isValid = userParamsForm.checkValidity();
                        if (!isValid) {
                            userParamsForm.reportValidity();
                        }
                        return isValid;
                    }

                    function stopMobileSignaturePolling() {
                        if (mobileCheckInterval !== null) {
                            window.clearInterval(mobileCheckInterval);
                            mobileCheckInterval = null;
                        }
                    }

                    function resetMobileSignStatus() {
                        $('#signRequestMobileSignModalStatus')
                            .addClass('d-none')
                            .removeClass('alert-success alert-danger alert-warning')
                            .empty();
                    }

                    function showMobileSignStatus(message, level) {
                        $('#signRequestMobileSignModalStatus')
                            .removeClass('d-none alert-success alert-danger alert-warning')
                            .addClass('alert-' + level)
                            .text(message);
                    }

                    function lockLocalDrawing() {
                        const canvasElement = document.getElementById('canvas');
                        const signPadLabelElement = document.getElementById('signPadLabel');
                        if (canvasElement) {
                            canvasElement.style.pointerEvents = 'none';
                            canvasElement.style.opacity = '0.6';
                        }
                        if (signPadLabelElement) {
                            signPadLabelElement.textContent = 'Une signature mobile a été chargée. Réinitialisez la signature pour dessiner à nouveau ici.';
                        }
                    }

                    function unlockLocalDrawing() {
                        const canvasElement = document.getElementById('canvas');
                        const signPadLabelElement = document.getElementById('signPadLabel');
                        if (canvasElement) {
                            canvasElement.style.pointerEvents = '';
                            canvasElement.style.opacity = '';
                        }
                        if (signPadLabelElement) {
                            signPadLabelElement.textContent = 'Vous pouvez dessiner une signature dans le rectangle ci dessous';
                        }
                    }

                    function clearMobilePreviewState(options = {}) {
                        if (mobilePreviewApplied && (options.clearToken || options.clearToken === undefined)) {
                            // If a preview was applied, we keep the token and polling to allow multiple sends
                            // unless it's a forced reset (e.g. error or user explicitly wants to restart)
                            if (!options.force) {
                                return;
                            }
                        }
                        stopMobileSignaturePolling();
                        mobilePreviewApplied = false;
                        mobilePreviewRequested = false;
                        mobilePreviewSignatureBase64 = null;
                        lastReceivedPreviewTimestamp = null;
                        if (options.clearToken) {
                            currentMobileToken = null;
                        }
                        if (modalMobileSignToken) {
                            modalMobileSignToken.value = '';
                        }
                        unlockLocalDrawing();
                    }

                    function applyMobileSignaturePreview(signImageBase64) {
                        if (!signImageBase64) {
                            return;
                        }

                        if (!window.userUi || typeof window.userUi.applyMobileSignaturePreview !== 'function') {
                            showMobileSignStatus('Signature reçue mais impossible de l’afficher automatiquement dans cette fenêtre.', 'warning');
                            return;
                        }

                        mobilePreviewApplied = true;
                        mobilePreviewRequested = false;
                        mobilePreviewSignatureBase64 = signImageBase64;
                        if (modalMobileSignToken) {
                            modalMobileSignToken.value = currentMobileToken || '';
                        }
                        window.userUi.applyMobileSignaturePreview(signImageBase64);
                        lockLocalDrawing();
                        showMobileSignStatus('Signature reçue. Vérifiez l’aperçu puis enregistrez votre signature.', 'success');
                        // stopMobileSignaturePolling();  // Keep polling to allow multiple sends
                        startMobileSignaturePolling(); // Restart polling after applying preview to be sure it's running
                        mobileSignModal?.hide();
                    }

                    function fetchMobileSignaturePreview() {
                        if (!currentMobileToken || mobilePreviewRequested) {
                            return;
                        }

                        mobilePreviewRequested = true;
                        $.ajax({
                            url: '/public/mobile-sign/' + encodeURIComponent(currentMobileToken) + '/preview',
                            type: 'GET',
                            success: function(response) {
                                mobilePreviewRequested = false;
                                if (response && response.success && response.signImageBase64) {
                                    applyMobileSignaturePreview(response.signImageBase64);
                                    return;
                                }
                                showMobileSignStatus('La signature mobile a été détectée, mais son aperçu est indisponible.', 'warning');
                            },
                            error: function(xhr) {
                                mobilePreviewRequested = false;
                                let message = 'Erreur lors de la récupération de la signature mobile.';
                                if (xhr && xhr.responseJSON && xhr.responseJSON.message) {
                                    message = xhr.responseJSON.message;
                                }
                                showMobileSignStatus(message, 'warning');
                            }
                        });
                    }

                    function pollMobileTokenStatus() {
                        if (!currentMobileToken) {
                            return;
                        }

                        $.ajax({
                            url: '/public/mobile-sign/' + encodeURIComponent(currentMobileToken) + '/status',
                            type: 'GET',
                            success: function(response) {
                                if (response && response.used) {
                                    stopMobileSignaturePolling();
                                    showMobileSignStatus('Ce lien a déjà été utilisé. Générez-en un nouveau si besoin.', 'warning');
                                    return;
                                }

                                if (response && response.previewAvailable && !mobilePreviewRequested) {
                                    if (response.previewTimestamp != null && response.previewTimestamp !== lastReceivedPreviewTimestamp) {
                                        lastReceivedPreviewTimestamp = response.previewTimestamp;
                                        fetchMobileSignaturePreview();
                                    }
                                }

                                if (response && response.valid === false) {
                                    stopMobileSignaturePolling();
                                    showMobileSignStatus(response.message || 'Ce lien n’est plus valide. Veuillez en générer un nouveau.', 'warning');
                                }
                            },
                            error: function() {
                                stopMobileSignaturePolling();
                                showMobileSignStatus('Erreur lors de la vérification du lien mobile.', 'warning');
                            }
                        });
                    }

                    function startMobileSignaturePolling() {
                        stopMobileSignaturePolling();
                        mobileCheckInterval = window.setInterval(function() {
                            pollMobileTokenStatus();
                        }, 2000);
                    }

                    function setPendingUserSignatureAction(selectionMode, selectedSignImageNumber = '') {
                        userParamsForm.dataset.selectionMode = selectionMode;
                        userParamsForm.dataset.selectedSignImageNumber = selectedSignImageNumber;
                    }

                    function triggerUserParamsPrepared(selectionMode, selectedSignImageNumber = '', skipValidation = false) {
                        if (!skipValidation && !validateIdentityFields()) {
                            return;
                        }
                        setPendingUserSignatureAction(selectionMode, selectedSignImageNumber);
                        if (selectionMode !== 'custom') {
                            document.getElementById('signImageBase64').value = '';
                        }
                        dispatchDocumentEvent('userParamsPrepared', {form: userParamsForm});
                    }

                    function getDisplayName(userState) {
                        if (userState?.firstname && userState?.name) {
                            return `${userState.firstname} ${userState.name}`;
                        }
                        return userState?.email || '';
                    }

                    function getInitials(userState) {
                        const firstInitial = userState?.firstname ? userState.firstname.charAt(0) : '';
                        const lastInitial = userState?.name ? userState.name.charAt(0) : '';
                        const initials = `${firstInitial}${lastInitial}`.trim().toUpperCase();
                        return initials || '?';
                    }

                    function updateNavbarUser(userState) {
                        if (!userState) {
                            return;
                        }
                        const navbarDisplayName = document.getElementById('navbar-user-display-name');
                        if (navbarDisplayName) {
                            navbarDisplayName.textContent = getDisplayName(userState);
                        }
                        const navbarUserAvatar = document.getElementById('navbar-user-avatar');
                        if (navbarUserAvatar) {
                            navbarUserAvatar.textContent = getInitials(userState);
                        }
                        if (typeof user !== 'undefined') {
                            user.firstname = userState.firstname;
                            user.name = userState.name;
                            user.email = userState.email;
                        }
                    }

                    function buildCarouselHtml(carouselId, signImageIds, imageWidth, imageHeight, withDeleteButtons = false, withSignButtons = false, activeIndex = 0) {
                        const hasActionButtons = withDeleteButtons || withSignButtons;
                        const items = signImageIds.map((signImageId, index) => {
                            const deleteButton = withDeleteButtons
                                ? `<button type="button" data-delete-sign-id="${signImageId}" class="btn btn-sm btn-danger text-white delete-sign-btn"><i class="fi fi-rr-trash"></i></button>`
                                : '';
                            const signButton = withSignButtons
                                ? `<button type="button" data-sign-image-number="${index}" class="btn btn-primary sign-with-image-btn">Signer avec cette image</button>`
                                : '';
                            const sizeAttr = imageHeight != null ? `height="${imageHeight}"` : `width="${imageWidth}"`;
                            if (hasActionButtons) {
                                return `<div class="carousel-item${index === activeIndex ? ' active' : ''} h-100"><div class="h-100 bg-white"><div class="position-relative h-100 d-flex align-items-center justify-content-center px-5 pb-5 pt-2 overflow-hidden"><img ${sizeAttr} class="img-fluid" style="max-height: ${imageHeight}px; width: auto; object-fit: contain;" src="/ws-secure/ui/signatures/${signImageId}" alt="sign image" /><div class="d-flex justify-content-center gap-2" style="position: absolute; left: 50%; bottom: 8px; transform: translateX(-50%); z-index: 5; width: max-content;">${signButton}${deleteButton}</div></div></div></div>`;
                            }
                            return `<div class="carousel-item${index === activeIndex ? ' active' : ''} text-center"><img ${sizeAttr} class="bg-white" src="/ws-secure/ui/signatures/${signImageId}" alt="sign image" /></div>`;
                        }).join('');

                        const extraAttributes = imageHeight == null ? ' data-bs-ride="carousel"' : '';
                        const carouselStyle = carouselId === 'carouselSign2' ? `width: 600px; height: 300px; margin: 0 auto;` : `${imageHeight != null ? `height: ${hasActionButtons ? '210px' : '150px'};` : 'width: 250px;'}`;
                        let addSignButton = '';
                        if (carouselId === 'carouselSign2') {
                            addSignButton = `<div class="mt-2 text-center"><button type="button" id="add-new-sign-btn" class="btn btn-success text-white"><i class="fi fi-rr-pen-swirl"></i> Dessiner une nouvelle signature</button></div>`;
                        }
                        return `<div>
                                    <div style="${carouselStyle}" id="${carouselId}" class="carousel slide border rounded border-secondary overflow-hidden bg-white"${extraAttributes}>
                                        <div class="carousel-inner h-100">${items}</div>
                                        <button title="Signature précédente" class="carousel-control-prev" href="#${carouselId}" role="button" data-bs-slide="prev">
                                            <span class="text-dark" aria-hidden="true"><i class="fi fi-rr-angle-small-left"></i></span>
                                            <span class="sr-only">Previous</span>
                                        </button>
                                        <button title="Signature suivante" class="carousel-control-next" href="#${carouselId}" role="button" data-bs-slide="next">
                                            <span class="text-dark" aria-hidden="true"><i class="fi fi-rr-angle-small-right"></i></span>
                                            <span class="sr-only">Next</span>
                                        </button>
                                    </div>
                                    ${addSignButton}
                                </div>`;
                    }

                    function renderModalSignatures(userState, activeIndex = 0) {
                        if (!modalSignaturesContainer) {
                            return;
                        }
                        const signImageIds = userState?.signImageIds || [];
                        if (signImageIds.length > 0) {
                            modalSignaturesContainer.innerHTML = `${buildCarouselHtml('carouselSign2', signImageIds, null, 250, true, true, activeIndex)}`;
                            modalSignaturesContainer.classList.remove('d-none', 'alert', 'alert-light');
                            modalSignaturesContainer.style.display = 'block';
                            const signPad = document.getElementById('signPad');
                            const signPadLabel = document.getElementById('signPadLabel');
                            if (signPad) {
                                signPad.classList.add('d-none');
                                signPad.style.display = 'none';
                            }
                            if (signPadLabel) {
                                signPadLabel.classList.add('d-none');
                                signPadLabel.style.display = 'none';
                            }
                            if (cancelNewSignBtn) {
                                cancelNewSignBtn.classList.add('d-none');
                                cancelNewSignBtn.parentElement.classList.add('d-none');
                            }
                        } else {
                            modalSignaturesContainer.classList.add('d-none');
                            const signPad = document.getElementById('signPad');
                            const signPadLabel = document.getElementById('signPadLabel');
                            if (signPad) {
                                signPad.classList.remove('d-none');
                                signPad.style.display = 'block';
                            }
                            if (signPadLabel) {
                                signPadLabel.classList.remove('d-none');
                                signPadLabel.style.display = 'block';
                            }
                            if (cancelNewSignBtn) {
                                cancelNewSignBtn.classList.add('d-none');
                                cancelNewSignBtn.parentElement.classList.remove('d-none');
                            }
                            if (window.userUi && typeof window.userUi.refreshSignaturePadLayout === 'function') {
                                window.userUi.refreshSignaturePadLayout();
                            }
                        }
                        updateSaveButtonState();
                    }

                    function renderNavbarSignatures(userState) {
                        if (!navbarUserSignaturesContent) {
                            return;
                        }
                        const signImageIds = userState?.signImageIds || [];
                        navbarUserSignaturesContent.innerHTML = signImageIds.length > 0
                            ? buildCarouselHtml('carouselSign', signImageIds, 250, null, false)
                            : '<div class="text-secondary">pas d’image de signature personalisée</div>';
                    }

                    function focusModalSignatureCarousel(activeIndex = 0) {
                        const carouselElement = document.getElementById('carouselSign2');
                        if (!carouselElement || typeof bootstrap === 'undefined' || !bootstrap.Carousel) {
                            return;
                        }
                        const carousel = bootstrap.Carousel.getOrCreateInstance(carouselElement, {
                            interval: false,
                            ride: false
                        });
                        carousel.to(activeIndex);
                    }

                    function updateLiveUserState(userState, options = {}) {
                        currentUserSignatureState = enrichUserState(userState);
                        const modalActiveIndex = Number.isInteger(options.modalActiveIndex) ? options.modalActiveIndex : 0;
                        updateNavbarUser(currentUserSignatureState);
                        renderModalSignatures(currentUserSignatureState, modalActiveIndex);
                        renderNavbarSignatures(currentUserSignatureState);
                        updateGeneratedSignaturePreview(currentUserSignatureState);
                        if (currentUserSignatureState?.signImageIds?.length > 0) {
                            focusModalSignatureCarousel(Math.min(modalActiveIndex, currentUserSignatureState.signImageIds.length - 1));
                        }
                    }

                    function setActionButtonsDisabled(disabled) {
                        document.querySelectorAll('#saveButton, #drawOnMobileBtn, #signWithGeneratedButton, #erase, #cancel-new-sign-btn, .sign-with-image-btn, .delete-sign-btn').forEach(button => {
                            button.disabled = disabled;
                        });
                    }

                    function resetSaveButton() {
                        setActionButtonsDisabled(false);
                        updateSaveButtonState();
                    }

                    function resetCustomSignatureInputs(forceShowPad = false) {
                        eraseButton?.click();
                        document.getElementById('signImageBase64').value = '';
                        const signPad = document.getElementById('signPad');
                        const signPadLabel = document.getElementById('signPadLabel');
                        const cropDiv = document.getElementById('crop-div');
                        const vanillaUpload = document.getElementById('vanilla-upload');
                        if (forceShowPad) {
                            if (signPad) {
                                signPad.classList.remove('d-none');
                                signPad.style.display = 'block';
                            }
                            if (signPadLabel) {
                                signPadLabel.classList.remove('d-none');
                                signPadLabel.style.display = 'block';
                            }
                        }
                        if (cancelNewSignBtn) {
                            cancelNewSignBtn.classList.add('d-none');
                            cancelNewSignBtn.parentElement.classList.remove('d-none');
                        }
                        if (cropDiv) {
                            cropDiv.style.display = 'none';
                        }
                        if (vanillaUpload) {
                            vanillaUpload.value = '';
                        }
                        updateSaveButtonState();
                        clearMobilePreviewState({clearToken: true});
                    }

                    function collapseSignDiv() {
                        if (!signDivElement || typeof bootstrap === 'undefined' || !bootstrap.Collapse) {
                            return;
                        }
                        bootstrap.Collapse.getOrCreateInstance(signDivElement, {
                            toggle: false
                        }).hide();
                    }

                    async function deleteSignature(signImageId) {
                        const deleteUrl = '/ws-secure/ui/profile/signatures/' + signImageId + (signRequestId ? ('?signRequestId=' + signRequestId) : '');
                        try {
                            const response = await fetch(deleteUrl, {
                                method: 'DELETE',
                                headers: {
                                    [csrfHeader]: csrfToken,
                                    'X-Requested-With': 'XMLHttpRequest',
                                    'Accept': 'application/json'
                                }
                            });
                            if (!response.ok) {
                                alert('Erreur lors de la suppression de la signature');
                                return;
                            }
                            resetCustomSignatureInputs(true);
                            updateLiveUserState(await response.json());
                            dispatchDocumentEvent('userSignatureDeleted', currentUserSignatureState);
                        } catch (error) {
                            console.error('Erreur:', error);
                            alert('Erreur lors de la suppression');
                        }
                    }

                    currentUserSignatureState = enrichUserState({
                        firstname: firstnameInput?.value || '',
                        name: nameInput?.value || '',
                        email: '',
                        signImageIds: [],
                        signImages: initialSignImages,
                        defaultSignImage: null
                    });

                    updateLiveUserState(currentUserSignatureState);

                    readUiMe() && hydrateModalFromUiMe(readUiMe());
                    updateSaveButtonState();
                    document.addEventListener('uiMeLoaded', ({detail}) => hydrateModalFromUiMe(detail));
                    document.addEventListener('userSignatureUpdated', ({detail}) => {
                        if (!detail) {
                            return;
                        }
                        const savedSignImageNumber = Number.parseInt(detail.savedSignImageNumber, 10);
                        updateLiveUserState(detail, {
                            modalActiveIndex: Number.isFinite(savedSignImageNumber) ? savedSignImageNumber : 0
                        });
                    });
                    addSignImageModal?.addEventListener('shown.bs.modal', () => {
                        const firstTab = document.querySelector('#draw-tab');
                        if (firstTab) {
                            bootstrap.Tab.getOrCreateInstance(firstTab).show();
                        }
                        resetCustomSignatureInputs();
                        renderModalSignatures(currentUserSignatureState);
                        const drawPane = document.getElementById('draw-pane');
                        if (drawPane) {
                            drawPane.classList.add('show', 'active');
                        }
                        if (window.userUi && typeof window.userUi.resetSignatureModal === 'function') {
                            window.userUi.resetSignatureModal();
                        }
                        updateGeneratedSignaturePreview(currentUserSignatureState);
                        updateSaveButtonState();
                    });
                    [previewTab, drawTab, uploadTab].forEach(tab => {
                        tab?.addEventListener('shown.bs.tab', () => {
                            updateSaveButtonState();
                            if (tab.id === 'draw-tab') {
                                window.dispatchEvent(new Event('resize'));
                            }
                        });
                    });
                    [nameInput, firstnameInput].forEach(input => {
                       input?.addEventListener('input', () => {
                           autoSave();
                       });
                    });
                    signWithGeneratedButton?.addEventListener('click', () => triggerUserParamsPrepared('generated', '999998'));
                    saveButton.addEventListener('click', event => {
                        const signImageBase64 = document.getElementById('signImageBase64');
                        if (`${signImageBase64?.value || ''}`.trim() === '') {
                            event.preventDefault();
                            updateSaveButtonState();
                            return;
                        }
                        setPendingUserSignatureAction('custom', '');
                    });
                    drawOnMobileButton?.addEventListener('click', () => {
                        clearMobilePreviewState({clearToken: true});
                        resetMobileSignStatus();

                        if (isTouchDevice()) {
                            $('#signRequestMobileSignModalLocal').removeClass('d-none');
                            $('#signRequestMobileSignModalQR').addClass('d-none');
                            mobileSignModal?.show();
                            return;
                        }

                        $('#signRequestMobileSignModalLocal').addClass('d-none');
                        $('#signRequestMobileSignModalQR').removeClass('d-none');

                        const tokenUrl = signRequestId ? `/${userType}/signrequests/${signRequestId}/generate-mobile-token` : '/user/users/mobile-sign/generate-token';
                        $.ajax({
                            url: tokenUrl,
                            type: 'GET',
                            success: function(response) {
                                if (response && response.qrcodeUrl && response.token) {
                                    currentMobileToken = response.token;
                                    $('#signRequestQrcodeImage').attr('src', response.qrcodeUrl);
                                    $('#signRequestMobileSignUrl').attr('href', response.url).text(response.url);
                                    mobileSignModal?.show();
                                    startMobileSignaturePolling();
                                    pollMobileTokenStatus();
                                } else {
                                    mobileSignModal?.show();
                                    showMobileSignStatus('Erreur lors de la génération du lien mobile. Veuillez réessayer.', 'danger');
                                }
                            },
                            error: function() {
                                mobileSignModal?.show();
                                showMobileSignStatus('Erreur lors de la génération du lien mobile. Veuillez réessayer.', 'danger');
                            }
                        });
                    });
                    userParamsForm.addEventListener('submit', e => e.preventDefault());
                    document.getElementById('signImageBase64')?.addEventListener('input', event => {
                        if (!mobilePreviewApplied || !mobilePreviewSignatureBase64) {
                            updateSaveButtonState();
                            return;
                        }
                        if ((event.target.value || '') !== mobilePreviewSignatureBase64) {
                            clearMobilePreviewState();
                        }
                        updateSaveButtonState();
                    });
                    document.getElementById('signImageBase64')?.addEventListener('change', event => {
                        if (!mobilePreviewApplied || !mobilePreviewSignatureBase64) {
                            updateSaveButtonState();
                            return;
                        }
                        if ((event.target.value || '') !== mobilePreviewSignatureBase64) {
                            clearMobilePreviewState();
                        }
                        updateSaveButtonState();
                    });
                    eraseButton?.addEventListener('click', () => clearMobilePreviewState());
                    cancelNewSignBtn?.addEventListener('click', () => {
                        const signPadLabel = document.getElementById('signPadLabel');
                        if (signPadLabel) signPadLabel.classList.add('d-none');
                        const signPad = document.getElementById('signPad');
                        if (signPad) signPad.classList.add('d-none');
                        if (cancelNewSignBtn) {
                            cancelNewSignBtn.classList.add('d-none');
                            cancelNewSignBtn.parentElement.classList.add('d-none');
                        }
                        renderModalSignatures(currentUserSignatureState);
                        updateSaveButtonState();
                    });
                    addSignImageModal?.addEventListener('hidden.bs.modal', () => {
                        clearMobilePreviewState({clearToken: true, force: true});
                        resetMobileSignStatus();
                    });
                    mobileSignModalElement?.addEventListener('shown.bs.modal', () => {
                        if (isTouchDevice()) {
                            initLocalMobileSignaturePad();
                        }
                    });
                    mobileSignModalElement?.addEventListener('hidden.bs.modal', () => {
                        if (localSignaturePad && typeof localSignaturePad.destroy === 'function') {
                            localSignaturePad.destroy();
                        }
                        localSignaturePad = null;
                        if (mobilePreviewApplied) {
                            // If a preview was applied, we keep the token and polling to allow multiple sends
                            return;
                        }
                        stopMobileSignaturePolling();
                        mobilePreviewRequested = false;
                        currentMobileToken = null;
                        resetMobileSignStatus();
                    });
                    modalSignaturesContainer?.addEventListener('click', async event => {
                        const addNewSignBtn = event.target.closest('#add-new-sign-btn');
                        if (addNewSignBtn) {
                            event.preventDefault();
                            modalSignaturesContainer.classList.add('d-none');
                            const signPad = document.getElementById('signPad');
                            const signPadLabel = document.getElementById('signPadLabel');
                            if (signPad) {
                                signPad.classList.remove('d-none');
                                signPad.style.display = 'block';
                            }
                            if (signPadLabel) {
                                signPadLabel.classList.remove('d-none');
                                signPadLabel.style.display = 'block';
                            }
                            if (cancelNewSignBtn) {
                                cancelNewSignBtn.classList.remove('d-none');
                                cancelNewSignBtn.parentElement.classList.remove('d-none');
                            }
                            updateSaveButtonState();
                            if (window.userUi && typeof window.userUi.refreshSignaturePadLayout === 'function') {
                                window.userUi.refreshSignaturePadLayout();
                            }
                            return;
                        }
                        const deleteButton = event.target.closest('.delete-sign-btn');
                        if (deleteButton) {
                            event.preventDefault();
                            if (confirm('Êtes-vous sûr de vouloir supprimer cette signature ?')) {
                                await deleteSignature(deleteButton.getAttribute('data-delete-sign-id'));
                            }
                            return;
                        }
                        const signButton = event.target.closest('.sign-with-image-btn');
                        if (signButton) {
                            triggerUserParamsPrepared('existing', signButton.getAttribute('data-sign-image-number'));
                        }
                    });

                    document.addEventListener('userParamsPrepared', function(e) {
                        if (e.detail?.form !== userParamsForm) {
                            return;
                        }

                        const formData = new FormData(userParamsForm);
                        const submitUrl = '/ws-secure/ui/profile';
                        const hasCustomImagePayload = `${formData.get('signImageBase64') || ''}`.trim() !== '';
                        if (signRequestId) {
                            formData.append('signRequestId', signRequestId);
                        }

                        setActionButtonsDisabled(true);
                        saveButton.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Enregistrement...';

                        fetch(submitUrl, {
                            method: 'POST',
                            headers: {
                                [csrfHeader]: csrfToken,
                                'X-Requested-With': 'XMLHttpRequest',
                                'Accept': 'application/json'
                            },
                            body: formData
                        })
                        .then(async response => {
                            if(response.ok) {
                                const userState = await response.json();
                                const selectionMode = userParamsForm.dataset.selectionMode || 'custom';
                                const isSelectionAction = selectionMode === 'generated' || selectionMode === 'existing';
                                let selectedSignImageNumber = null;
                                if (selectionMode === 'generated') {
                                    updateLiveUserState(userState);
                                    selectedSignImageNumber = 999998;
                                } else if (selectionMode === 'existing') {
                                    updateLiveUserState(userState);
                                    selectedSignImageNumber = parseInt(userParamsForm.dataset.selectedSignImageNumber, 10);
                                } else {
                                    const lastImageIndex = hasCustomImagePayload && userState?.signImageIds?.length > 0 ? userState.signImageIds.length - 1 : 0;
                                    resetCustomSignatureInputs(true);
                                    updateLiveUserState(userState, { modalActiveIndex: lastImageIndex });
                                    if (hasCustomImagePayload && userState?.signImageIds?.length > 0) {
                                        triggerUserParamsPrepared('existing', userState.signImageIds.length - 1, true);
                                    }
                                }
                                resetSaveButton();
                                if (!isSelectionAction) {
                                    collapseSignDiv();
                                    dispatchDocumentEvent('resetUserSignatureModal', {form: userParamsForm});
                                }
                                dispatchDocumentEvent('userSignatureUpdated', currentUserSignatureState);
                                if (isSelectionAction) {
                                    dispatchDocumentEvent('userSignatureSelected', { ...currentUserSignatureState, selectedSignImageNumber });
                                    const modal = bootstrap.Modal.getInstance(addSignImageModal);
                                    if(modal) {
                                        modal.hide();
                                    }
                                }
                            } else {
                                alert('Erreur lors de l\'enregistrement');
                                resetSaveButton();
                            }
                        })
                        .catch(error => {
                            console.error('Erreur:', error);
                            alert('Erreur de connexion');
                            resetSaveButton();
                        });
                    });
}
