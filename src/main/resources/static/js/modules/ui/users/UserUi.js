import {UserSignaturePad} from "./UserSignaturePad.js?version=@version@";
import {UserSignatureCrop} from "./UserSignatureCrop.js?version=@version@";
import {SignRequestParams} from '../../../prototypes/SignRequestParams.js?version=@version@';
import {attachDirtyIndicator} from '../DirtyIndicator.js?version=@version@';

export class UserUi {

    constructor(userName, signRequestParams, signImages, userType, defaultSignImageNumber, signatureUiConfig = null) {
        console.log('Starting user UI');
        this.userName = userName;
        this.signImages = signImages;
        this.userType = userType;
        this.defaultSignImageNumber = defaultSignImageNumber;
        this.signatureUiConfig = signatureUiConfig;
        this.emailAlertFrequencySelect = $("#emailAlertFrequency_id");
        this.emailAlertDay = $("#emailAlertDayDiv");
        this.emailAlertHour = $("#emailAlertHourDiv");
        this.userSignaturePad = new UserSignaturePad("canvas", 1, 4);
        this.userSignatureCrop = new UserSignatureCrop();
        this.saveSignRequestParams = false;
        this.dirtyIndicator = null;
        this.checkAlertFrequency();
        this.signRequestParamsDefault = signRequestParams;
        if(signRequestParams != null) {
            this.saveSignRequestParams = true;
        } else {
            this.signRequestParamsDefault = {
                "addWatermark": null,
                "extraText": "",
                "addExtra": true,
                "extraOnTop": true,
                "extraType": null,
                "extraName": null,
                "extraDate": null,
                "isExtraText": null,
                "signImageNumber": 0
            };
        }
        this.toggleSaveSignRequest();
        this.initListeners();
    }

    initListeners() {
        $("#saveButton").on('click', () => this.save());
        document.addEventListener('resetUserSignatureModal', e => this.resetSignatureModal(e));
        document.addEventListener('uiMeLoaded', e => this.applyUiMe(e.detail));
        this.initDirtyIndicatorWhenReady();

        this.userSignatureCrop.addEventListener("started", () => this.userSignaturePad.clear());
        $("#sign-div, #sign-pad").on('shown.bs.collapse', () => this.refreshSignaturePadLayout());
        $("#add-sign-image").on('shown.bs.modal', () => this.refreshSignaturePadLayout());
        this.emailAlertFrequencySelect?.on("change", () => this.checkAlertFrequency());
        $(document).on('click', '[id^="deleteSign_"]', e => this.handleDeleteSign(e));
        $("input[name='saveSignRequestParams']").on("change", () => this.toggleSaveSignRequest());
        $("#signRequestParamsClean").on("click", () => this.clearLocalStorage())
        this.applyUiMe(this.readSessionJson('uiMe'));
    }

    initDirtyIndicatorWhenReady() {
        if (document.documentElement.dataset.globalUiReady === 'true') {
            queueMicrotask(() => this.initDirtyIndicator());
            return;
        }

        document.addEventListener('globalUiReady', () => this.initDirtyIndicator(), {once: true});
    }

    initDirtyIndicator() {
        if (this.dirtyIndicator != null) {
            return;
        }

        const form = document.getElementById('userParamsForm');
        const saveButton = document.getElementById('saveButton');

        if (!form || !saveButton) {
            return;
        }

        this.dirtyIndicator = attachDirtyIndicator({
            form,
            saveButton,
            extraInputs: [
                document.getElementById('signImageBase64'),
                document.getElementById('sign-request-params')
            ].filter(Boolean),
            extraStateProviders: [
                () => this.userSignaturePad.hasPendingDirtyState()
            ]
        });
    }

    readSessionJson(key) {
        try {
            const rawValue = sessionStorage.getItem(key);
            return rawValue ? JSON.parse(rawValue) : null;
        } catch (e) {
            console.debug('Unable to parse session UI payload', key, e);
            return null;
        }
    }

    handleDeleteSign(e) {
        e.preventDefault();
        const signId = $(e.currentTarget).attr("data-es-id");
        bootbox.confirm("Voulez-vous vraiment supprimer cette signature ?", result => {
            if (result) {
                $('#deleteForm-' + signId).submit();
            }
        });
    }

    escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }

    getCsrfToken() {
        return document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
    }

    getCsrfParameterName() {
        const userParamsForm = document.getElementById('userParamsForm');
        if (userParamsForm?.action) {
            try {
                const actionUrl = new URL(userParamsForm.action, window.location.origin);
                const csrfToken = this.getCsrfToken();
                for (const [parameterName, parameterValue] of actionUrl.searchParams.entries()) {
                    if (parameterValue === csrfToken || parameterName.toLowerCase().includes('csrf')) {
                        return parameterName;
                    }
                }
            } catch (e) {
                console.debug('Unable to resolve CSRF parameter name from userParamsForm action', e);
            }
        }
        return '_csrf';
    }

    applyUiMe(me) {
        if (me?.user == null) {
            return;
        }
        this.userType = me.user.userType?.toLowerCase() === 'external' ? 'otp' : 'user';
        this.defaultSignImageNumber = me.user.defaultSignImageNumber;
        this.renderSignaturesTable(me.userImagesIds || []);
        this.renderDeleteForms(me.userImagesIds || []);
        this.renderKeystore(me.keystoreFileName ?? null);
    }

    renderSignaturesTable(signImageIds) {
        const tbody = document.getElementById('user-settings-signatures-body');
        if (tbody == null) {
            return;
        }
        const resolvedUserType = this.userType === 'otp' ? 'otp' : 'user';
        const defaultSignImageNumber = this.defaultSignImageNumber;
        const renderStarClass = imageNumber => defaultSignImageNumber === imageNumber ? 'text-warning' : 'text-secondary';
        const customRows = (Array.isArray(signImageIds) ? signImageIds : []).map((signImageId, index) => `
            <tr>
                <td class="text-left w-100" style="position: relative;height: 74px;" >
                    <img id="sign-image-${index}" class="thumbnail" src="/ws-secure/ui/signatures/${encodeURIComponent(signImageId)}" alt="Image de la signature ${index}"/>
                </td>
                <td>
                    <a href="/${resolvedUserType}/users/set-default-sign-image/${index}" role="button" class="btn btn-sm btn-transparent ${renderStarClass(index)}">
                        <i class="fi fi-rr-star"></i>
                    </a>
                </td>
                <td>
                    <button id="deleteSign_${signImageId}" data-es-id="${signImageId}" role="button" class="btn btn-sm btn-danger text-white">
                        <i class="fi fi-rr-trash"></i>
                    </button>
                </td>
            </tr>
        `).join('');

        tbody.innerHTML = `
            <tr>
                <td class="text-left w-100" style="position: relative;height: 74px;" >
                    <img class="thumbnail" src="/ws-secure/ui/signatures/default-paraphe" alt="Image avec paraphe"/>
                </td>
                <td>
                    <a href="/${resolvedUserType}/users/set-default-sign-image/999997" role="button" class="btn btn-sm btn-transparent ${renderStarClass(999997)}">
                        <i class="fi fi-rr-star"></i>
                    </a>
                </td>
                <td></td>
            </tr>
            <tr>
                <td class="text-left w-100" style="position: relative;height: 74px;" >
                    <img class="thumbnail" src="/ws-secure/ui/signatures/default-image" alt="Image avec nom prénom"/>
                </td>
                <td>
                    <a href="/${resolvedUserType}/users/set-default-sign-image/999998" role="button" class="btn btn-sm btn-transparent ${renderStarClass(999998)}">
                        <i class="fi fi-rr-star"></i>
                    </a>
                </td>
                <td></td>
            </tr>
            ${customRows}
        `;
    }

    renderDeleteForms(signImageIds) {
        const formsContainer = document.getElementById('user-settings-delete-sign-forms');
        if (formsContainer == null) {
            return;
        }
        const resolvedUserType = this.userType === 'otp' ? 'otp' : 'user';
        const csrfToken = this.escapeHtml(this.getCsrfToken());
        const csrfParameterName = this.escapeHtml(this.getCsrfParameterName());
        const csrfInput = csrfToken !== ''
            ? `<input type="hidden" name="${csrfParameterName}" value="${csrfToken}" />`
            : '';
        formsContainer.innerHTML = (Array.isArray(signImageIds) ? signImageIds : []).map(signImageId => `
            <form id="deleteForm-${signImageId}" action="/${resolvedUserType}/users/delete-sign/${encodeURIComponent(signImageId)}" method="post">
                <input type="hidden" name="_method" value="delete" />
                ${csrfInput}
            </form>
        `).join('');
    }

    renderKeystore(keystoreFileName) {
        const keystoreContainer = document.getElementById('user-settings-current-keystore');
        if (keystoreContainer == null) {
            return;
        }
        if (keystoreFileName == null || keystoreFileName === '') {
            keystoreContainer.innerHTML = '';
            return;
        }
        keystoreContainer.innerHTML = `
            <div class="alert alert-secondary">
                Keystore actuel : <a href="/ws-secure/ui/keystore"><span>${this.escapeHtml(keystoreFileName)}</span></a>
                <br>
                <button type="button" class="btn btn-sm btn-primary text-left" data-bs-toggle="modal" data-bs-target="#testKeystore">
                    <i class="fi fi-rr-badge"></i> Tester mon certificat
                </button>
                <button type="button" class="btn btn-sm btn-danger text-left" data-bs-toggle="modal" data-bs-target="#removeKeystore">
                    <i class="fi fi-rr-trash"></i> Supprimer mon certificat
                </button>
            </div>
        `;
    }

    resolveSpecialSignImageNumbers() {
        const signImages = Array.isArray(this.signImages) ? this.signImages : [];
        if (signImages.length === 0) {
            return {
                generatedSignImageNumber: null,
                parapheSignImageNumber: null
            };
        }

        const uiMe = this.readSessionJson('uiMe');
        const userImageIds = Array.isArray(uiMe?.userImagesIds) ? uiMe.userImagesIds : null;
        if (userImageIds != null) {
            const generatedSignImageNumber = signImages.length > userImageIds.length
                ? userImageIds.length
                : null;
            return {
                generatedSignImageNumber,
                parapheSignImageNumber: generatedSignImageNumber != null && signImages.length > generatedSignImageNumber + 1
                    ? generatedSignImageNumber + 1
                    : null
            };
        }

        if (signImages.length === 1) {
            return {
                generatedSignImageNumber: 0,
                parapheSignImageNumber: null
            };
        }

        return {
            generatedSignImageNumber: signImages.length - 2,
            parapheSignImageNumber: signImages.length - 1
        };
    }

    teardownSignRequestParamsPreview() {
        if (this.signRequestParams?.resizeNamespace) {
            $(window).off("resize" + this.signRequestParams.resizeNamespace);
        }
        $(document).off(".userSignRequestParamsPreview");

        if (typeof this.signRequestParams?.resetMobileSignatureFlow === 'function') {
            this.signRequestParams.resetMobileSignatureFlow({clearToken: true, force: true});
        }

        $("#pdf .cross").remove();
        $("#pdf > input[id^='canvas_']").remove();
        this.signRequestParams = null;
    }

    initializeSignRequestParamsPreviewStorage(previewParams, signImageNumber) {
        const resolvedPreviewParams = previewParams ?? {};
        const hasExtraText = String(resolvedPreviewParams.extraText ?? '') !== '';
        const previewStorage = {
            addWatermark: resolvedPreviewParams.addWatermark === true,
            addExtra: resolvedPreviewParams.addExtra === true,
            extraOnTop: resolvedPreviewParams.extraOnTop !== false,
            extraType: resolvedPreviewParams.extraType === true,
            extraName: resolvedPreviewParams.extraName === true,
            extraDate: resolvedPreviewParams.extraDate === true,
            extraText: hasExtraText,
            addImage: true
        };

        Object.entries(previewStorage).forEach(([key, value]) => {
            localStorage.setItem(key, JSON.stringify(value));
        });

        const normalizedSignImageNumber = Number.parseInt(signImageNumber, 10);
        if (Number.isFinite(normalizedSignImageNumber)) {
            localStorage.setItem('signNumber', String(normalizedSignImageNumber));
        }
    }

    applySignRequestParamsPreviewText(previewParams) {
        if (this.signRequestParams == null) {
            return;
        }

        const extraText = String(previewParams?.extraText ?? '');
        if (extraText === '') {
            return;
        }

        const textarea = $("#textExtra_" + this.signRequestParams.id);
        if (!textarea.length) {
            return;
        }

        this.signRequestParams.savedText = extraText;
        this.signRequestParams.extraText = extraText;
        this.signRequestParams.isExtraText = false;
        textarea.val(extraText).trigger("input");
    }

    computePreviewSignScale(previewParams) {
        const explicitScale = Number.parseFloat(previewParams?.signScale * 2);
        if (Number.isFinite(explicitScale) && explicitScale > 0) {
            return explicitScale;
        }

        const width = Number.parseFloat(previewParams?.signWidth);
        const height = Number.parseFloat(previewParams?.signHeight);
        let computedScale = null;

        if (previewParams?.addExtra === true && previewParams?.extraOnTop === false) {
            computedScale = height / 100;
        } else {
            computedScale = width / 200;
        }

        return Number.isFinite(computedScale) && computedScale > 0 ? computedScale : 1;
    }

    closePreviewExtraTools() {
        const signRequestParams = this.signRequestParams;
        if (signRequestParams?.id == null) {
            return;
        }

        $("#extraTools_" + signRequestParams.id).addClass("d-none");
        if (signRequestParams.cross?.hasClass("ui-resizable")) {
            try {
                signRequestParams.cross.resizable("enable");
            } catch (e) {
                console.debug('Unable to re-enable preview resizing', e);
            }
        }
    }

    bindSignRequestParamsPreviewUi() {
        const signRequestParams = this.signRequestParams;
        if (signRequestParams?.id == null) {
            return;
        }

        const previewId = signRequestParams.id;
        $(document).off(".userSignRequestParamsPreview");

        $(document).on("mousedown.userSignRequestParamsPreview", event => {
            const target = $(event.target);
            if (target.closest("#cross_" + previewId).length || target.closest("#crossTools_" + previewId).length) {
                return;
            }
            this.closePreviewExtraTools();
        });

        $("#displayMoreTools_" + previewId).off(".userSignRequestParamsPreview")
            .on("mousedown.userSignRequestParamsPreview", () => {
                window.setTimeout(() => {
                    const extraTools = $("#extraTools_" + previewId);
                    if (extraTools.hasClass("d-none")) {
                        this.closePreviewExtraTools();
                    }
                }, 0);
            });
    }

    recenterSignRequestParamsPreview() {
        if (typeof this.signRequestParams?.centerOnCurrentViewport !== 'function') {
            return;
        }
        requestAnimationFrame(() => {
            requestAnimationFrame(() => {
                this.signRequestParams?.centerOnCurrentViewport();
            });
        });
    }

    buildSerializableSignRequestParams() {
        const params = this.signRequestParams ?? this.signRequestParamsDefault ?? {};
        return {
            pdSignatureFieldName: params.pdSignatureFieldName ?? null,
            signImageNumber: Number.isFinite(Number.parseInt(params.signImageNumber, 10)) ? Number.parseInt(params.signImageNumber, 10) : 0,
            signPageNumber: Number.isFinite(Number.parseInt(params.signPageNumber, 10)) ? Number.parseInt(params.signPageNumber, 10) : 1,
            signDocumentNumber: Number.isFinite(Number.parseInt(params.signDocumentNumber, 10)) ? Number.parseInt(params.signDocumentNumber, 10) : 0,
            signWidth: Number.isFinite(Number.parseInt(params.signWidth, 10)) ? Number.parseInt(params.signWidth, 10) : 200,
            signHeight: Number.isFinite(Number.parseInt(params.signHeight, 10)) ? Number.parseInt(params.signHeight, 10) : 100,
            xPos: 0,
            yPos: 0,
            rotate: Number.isFinite(Number.parseInt(params.rotate, 10)) ? Number.parseInt(params.rotate, 10) : 0,
            extraText: String(params.extraText ?? ''),
            isExtraText: params.isExtraText === true,
            addWatermark: params.addWatermark === true,
            allPages: params.allPages === true,
            addImage: params.addImage !== false,
            addExtra: params.addExtra === true,
            extraType: params.extraType === true,
            extraName: params.extraName === true,
            extraDate: params.extraDate === true,
            extraOnTop: params.extraOnTop !== false,
            textPart: params.textPart ?? null,
            signScale: Number.isFinite(Number.parseFloat(params.signScale)) ? Number.parseFloat(params.signScale) : 1,
            red: Number.isFinite(Number.parseInt(params.red, 10)) ? Number.parseInt(params.red, 10) : 0,
            green: Number.isFinite(Number.parseInt(params.green, 10)) ? Number.parseInt(params.green, 10) : 0,
            blue: Number.isFinite(Number.parseInt(params.blue, 10)) ? Number.parseInt(params.blue, 10) : 0,
            fontSize: Number.isFinite(Number.parseInt(params.fontSize, 10)) ? Number.parseInt(params.fontSize, 10) : 16,
            recipientId: params.recipientId ?? null
        };
    }

    enableSignRequestParams() {
        this.teardownSignRequestParamsPreview();

        const previewParams = this.signRequestParamsDefault ?? {};
        const normalizedPreviewParams = {
            ...previewParams,
            signScale: this.computePreviewSignScale(previewParams)
        };
        const initialSignImageNumber = Number.parseInt(normalizedPreviewParams?.signImageNumber, 10);
        const fallbackSignImageNumber = Number.isFinite(initialSignImageNumber)
            ? initialSignImageNumber
            : this.defaultSignImageNumber;
        this.initializeSignRequestParamsPreviewStorage(normalizedPreviewParams, fallbackSignImageNumber);

        this.signRequestParams = new SignRequestParams(
            this.userType === 'otp',
            normalizedPreviewParams,
            0,
            1,
            1,
            this.userName,
            this.userName,
            true,
            true,
            false,
            false,
            null,
            false,
            this.signImages,
            0,
            undefined,
            undefined,
            this.signatureUiConfig
        );

        const {generatedSignImageNumber, parapheSignImageNumber} = this.resolveSpecialSignImageNumbers();
        this.signRequestParams.generatedSignImageNumber = generatedSignImageNumber;
        this.signRequestParams.parapheSignImageNumber = parapheSignImageNumber;
        this.bindSignRequestParamsPreviewUi();
        this.recenterSignRequestParamsPreview();

        if (Number.isFinite(Number.parseInt(fallbackSignImageNumber, 10))) {
            this.signRequestParams.changeSignImage(fallbackSignImageNumber).catch(error => {
                console.debug('Unable to initialize signature preview image', error);
            }).finally(() => this.applySignRequestParamsPreviewText(normalizedPreviewParams));
            return;
        }

        this.applySignRequestParamsPreviewText(normalizedPreviewParams);
    }

    clearLocalStorage() {
        bootbox.confirm("Vous allez remettre à zéro les paramètres par défaut de votre signature", function(result){
            if(result) {
                localStorage.clear();
                bootbox.alert("Vous paramètres ont été réinitialisés", null);

            }
        });
    }

    toggleSaveSignRequest() {
        if(this.saveSignRequestParams) {
            localStorage.clear();
            this.saveSignRequestParams = false;
            $("#signRequestParamsFormDiv").removeClass("d-none");
            $("#signRequestParamsCleanDiv").addClass("d-none");
            this.enableSignRequestParams();
        } else {
            this.saveSignRequestParams = true;
            this.teardownSignRequestParamsPreview();
            $("#signRequestParamsFormDiv").addClass("d-none");
            $("#signRequestParamsCleanDiv").removeClass("d-none");
        }

    }

    save() {
        this.userSignaturePad.checkSignatureUpdate();
        const userParamsForm = $("#userParamsForm");
        const formElement = userParamsForm.get(0);
        const isFetchModalContext = userParamsForm.closest("#add-sign-image").length > 0;
        if(!this.saveSignRequestParams) {
            let signRequestParams = JSON.stringify(this.buildSerializableSignRequestParams());
            $("#sign-request-params").val(signRequestParams);
            sessionStorage.setItem("favoriteSignRequestParams", signRequestParams);
        } else {
            sessionStorage.setItem("favoriteSignRequestParams", "null");
        }
        if($("#name").val() === "" || $("#firstname").val() === "") {
            $("#submitUserParamsForm").click();
            return;
        }
        if(this.userSignaturePad.signaturePad.isEmpty() && this.userSignatureCrop.signImageBase64 === null && !this.userSignaturePad.signImageBase64Val) {
            $("#signImageBase64").val("");
        }
        if(isFetchModalContext && formElement) {
            document.dispatchEvent(new CustomEvent("userParamsPrepared", {detail: {form: formElement}}));
            return;
        }
        userParamsForm.submit();
    }

    applyMobileSignaturePreview(imageBase64) {
        if (!imageBase64) {
            return;
        }

        this.userSignatureCrop.reset();
        this.userSignaturePad.loadImage(imageBase64);
        this.refreshSignaturePadLayout();
    }

    resetSignatureModal(event) {
        if (event?.detail?.form?.id && event.detail.form.id !== 'userParamsForm') {
            return;
        }
        const signDiv = $("#sign-div");
        $("#erase").trigger('click');
        this.userSignaturePad.firstClear = true;
        this.userSignaturePad.signImageBase64.val("");
        this.userSignaturePad.signImageBase64Val = null;
        this.userSignaturePad.canvas.css("background", "");
        this.userSignatureCrop.reset();
        if (signDiv.length) {
            signDiv.collapse('hide');
        }
        this.refreshSignaturePadLayout();
    }

    refreshSignaturePadLayout() {
        const refresh = () => {
            if (this.userSignaturePad == null) {
                return;
            }
            this.userSignaturePad.cachedWidth = null;
            this.userSignaturePad.resizeCanvas();
        };
        window.requestAnimationFrame(() => {
            refresh();
            window.setTimeout(refresh, 50);
        });
    }

    checkAlertFrequency() {
        let selectedValue = this.emailAlertFrequencySelect.val();
        console.info("selected check alert frequency : " + selectedValue);
        this.emailAlertDay.toggleClass("d-none", selectedValue !== 'weekly');
        this.emailAlertHour.toggleClass("d-none", selectedValue !== 'daily');
    }
}