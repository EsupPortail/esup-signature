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
        if(this.emailAlertFrequencySelect != null) {
            this.emailAlertFrequencySelect.on("change", e => this.checkAlertFrequency(e));
        }
        this.bindDeleteSignButtons();
        $("input[name='saveSignRequestParams']").on("change", e => this.toggleSaveSignRequest(e));
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

    bindDeleteSignButtons() {
        $('[id^="deleteSign_"]').each(function() {
            $(this).off('click').on('click', function (e){
                e.preventDefault();
                let target = e.currentTarget;
                bootbox.confirm("Voulez-vous vraiment supprimer cette signature ?", function(result){
                    if(result) {
                        $('#deleteForm-' + $(target).attr("data-es-id")).submit();
                    }
                });
            });
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
        this.bindDeleteSignButtons();
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
                    <i class="fa-solid fa-certificate"></i> Tester mon certificat
                </button>
                <button type="button" class="btn btn-sm btn-danger text-left" data-bs-toggle="modal" data-bs-target="#removeKeystore">
                    <i class="fa-solid fa-trash-alt"></i> Supprimer mon certificat
                </button>
            </div>
        `;
    }

    enableSignRequestParams() {
        this.signRequestParams = new SignRequestParams(false, this.signRequestParamsDefault, 0, 1, 1, this.userName, this.userName, false, true, false, false, null, true, this.signImages, undefined, undefined, undefined, this.signatureUiConfig);
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
            this.signRequestParams.xPos = 0;
            this.signRequestParams.yPos = 0;
            let signRequestParams = JSON.stringify(this.signRequestParams);
            $("#sign-request-params").val(signRequestParams);
        }
        if($("#name").val() === "" || $("#firstname").val() === "") {
            $("#submitUserParamsForm").click();
            return;
        }
        if(this.userSignaturePad.signaturePad.isEmpty() && this.userSignatureCrop.signImageBase64 === null) {
            $("#signImageBase64").val("");
        }
        if(isFetchModalContext && formElement) {
            document.dispatchEvent(new CustomEvent("userParamsPrepared", {
                detail: {form: formElement}
            }));
            return;
        }
        userParamsForm.submit();
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
        if(selectedValue === 'daily') {
            this.emailAlertDay.addClass("d-none");
            this.emailAlertHour.removeClass("d-none");
        } else if(selectedValue === 'weekly') {
            this.emailAlertDay.removeClass("d-none");
            this.emailAlertHour.addClass("d-none");
        } else {
            this.emailAlertDay.addClass("d-none");
            this.emailAlertHour.addClass("d-none");
        }
    }
}