export class PdfLinkFieldValidator {

    constructor(viewer) {
        this.viewer = viewer;
    }

    ensureLinkFieldStyles() {
        if (document.getElementById('pdf-link-style')) {
            return;
        }
        const css = `
            .pdf-link-input {
                border-radius: 0 !important;
                padding-right: 72px !important;
            }
            .pdf-link-disabled-input {
                cursor: pointer;
                background: #fff !important;
                color: inherit !important;
            }
            .pdf-link-disabled-input:focus {
                outline: 1px solid #0d6efd;
                outline-offset: 0;
            }
            .pdf-link-test-btn,
            .pdf-link-display-btn {
                position: absolute;
                top: 0;
                right: 0;
                height: 100%;
                min-width: 68px;
                border: 1px solid #6c757d;
                background: #fff;
                color: #212529;
                padding: 0 10px;
                cursor: pointer;
                text-decoration: none;
                display: inline-flex;
                align-items: center;
                justify-content: center;
                z-index: 2;
            }
            .pdf-link-test-btn[disabled],
            .pdf-link-display-btn.is-disabled {
                color: #6c757d;
                background: #f1f3f5;
                cursor: not-allowed;
                pointer-events: none;
            }
            .pdf-link-invalid {
                border-color: #c82333 !important;
            }
            .pdf-link-valid {
                border-color: #198754 !important;
            }
        `;
        const style = document.createElement('style');
        style.id = 'pdf-link-style';
        style.appendChild(document.createTextNode(css));
        document.head.appendChild(style);
    }

    normalizeLinkValue(value) {
        const trimmedValue = (value || '').trim();
        if (!trimmedValue) {
            return '';
        }
        if (/^[a-zA-Z][a-zA-Z0-9+\-.]*:/.test(trimmedValue)) {
            return trimmedValue;
        }
        return 'https://' + trimmedValue;
    }

    isValidLinkValue(value) {
        const normalizedValue = this.normalizeLinkValue(value);
        if (!normalizedValue) {
            return false;
        }
        try {
            const url = new URL(normalizedValue);
            if (!['http:', 'https:'].includes(url.protocol)) {
                return false;
            }
            if (!url.hostname || !url.hostname.includes('.')) {
                return false;
            }
            return true;
        } catch (e) {
            return false;
        }
    }

    clearLinkReachabilityCheck(fieldName) {
        const timer = this.viewer.linkValidationTimers.get(fieldName);
        if (timer) {
            clearTimeout(timer);
            this.viewer.linkValidationTimers.delete(fieldName);
        }
        const controller = this.viewer.linkValidationControllers.get(fieldName);
        if (controller) {
            controller.abort();
            this.viewer.linkValidationControllers.delete(fieldName);
        }
    }

    async checkLinkReachability(url, signal) {
        try {
            await fetch(url, {
                method: 'HEAD',
                mode: 'no-cors',
                cache: 'no-store',
                redirect: 'follow',
                signal: signal,
            });
            return true;
        } catch (headError) {
            if (signal?.aborted) {
                throw headError;
            }
            await fetch(url, {
                method: 'GET',
                mode: 'no-cors',
                cache: 'no-store',
                redirect: 'follow',
                signal: signal,
            });
            return true;
        }
    }

    scheduleLinkReachabilityCheck(fieldName, value, onStateChange) {
        const normalizedValue = this.normalizeLinkValue(value);
        if (!normalizedValue) {
            const emptyState = { status: 'empty', value: value, normalizedValue: '' };
            this.viewer.linkValidationStates.set(fieldName, emptyState);
            onStateChange(emptyState);
            return;
        }

        this.clearLinkReachabilityCheck(fieldName);
        const sequence = (this.viewer.linkValidationSeq.get(fieldName) || 0) + 1;
        this.viewer.linkValidationSeq.set(fieldName, sequence);

        const checkingState = {
            status: 'checking',
            value: value,
            normalizedValue: normalizedValue,
        };
        this.viewer.linkValidationStates.set(fieldName, checkingState);
        onStateChange(checkingState);

        const timer = setTimeout(async () => {
            const controller = new AbortController();
            this.viewer.linkValidationControllers.set(fieldName, controller);
            const timeoutId = setTimeout(() => controller.abort(), 4000);

            try {
                await this.checkLinkReachability(normalizedValue, controller.signal);
                if (this.viewer.linkValidationSeq.get(fieldName) !== sequence) {
                    return;
                }
                const reachableState = {
                    status: 'reachable',
                    value: value,
                    normalizedValue: normalizedValue,
                };
                this.viewer.linkValidationStates.set(fieldName, reachableState);
                onStateChange(reachableState);
            } catch (error) {
                if (this.viewer.linkValidationSeq.get(fieldName) !== sequence) {
                    return;
                }
                const unreachableState = {
                    status: 'unreachable',
                    value: value,
                    normalizedValue: normalizedValue,
                };
                this.viewer.linkValidationStates.set(fieldName, unreachableState);
                onStateChange(unreachableState);
            } finally {
                clearTimeout(timeoutId);
                this.viewer.linkValidationControllers.delete(fieldName);
            }
        }, 350);

        this.viewer.linkValidationTimers.set(fieldName, timer);
    }
}
