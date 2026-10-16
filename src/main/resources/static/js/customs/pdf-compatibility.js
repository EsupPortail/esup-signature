// Keep this script independent of modules and modern syntax: it must run even
// when the browser cannot parse PDF.js or the application modules.
(function (window, document) {
    'use strict';

    var resolveReady;
    var messageKey = null;
    var compatibility = {
        ready: typeof window.Promise === 'function' ? new window.Promise(function (resolve) {
            resolveReady = resolve;
        }) : null,
        incompatible: false,
        isCompatibilityError: function (error, initializing) {
            if (!error) return false;
            if (initializing && error.name === 'SyntaxError') return true;
            if (initializing && ((error.name === 'ReferenceError' && /is not defined/.test(error.message || '')) ||
                (error.name === 'TypeError' && /is not a (?:function|constructor)/.test(error.message || '')))) return true;
            // Do not classify network errors or invalid PDFs as incompatibilities.
            return (error.name === 'ReferenceError' || error.name === 'TypeError') &&
                /(?:Promise\.withResolvers|DOMMatrix|Path2D|ReadableStream|OffscreenCanvas|ImageData|Uint8Array\.(?:fromBase64|fromHex)|\.(?:toBase64|toHex))/.test(error.message || '');
        },
        fail: function (error, initializing) {
            compatibility.incompatible = compatibility.incompatible || compatibility.isCompatibilityError(error, initializing);
            messageKey = compatibility.incompatible ? 'browser-error' : 'load-error';
            showMessage();
        },
        complete: function (error) {
            if (error) compatibility.fail(error, true);
            if (resolveReady) {
                resolveReady(!error && !compatibility.incompatible);
                resolveReady = null;
            }
        }
    };
    window.esupPdfCompatibility = compatibility;

    function showMessage() {
        if (!messageKey || !document.body) return;
        var messages = document.getElementById('es-id-pdf-compatibility-messages');
        if (!messages) return;
        var alert = document.getElementById('es-id-pdf-compatibility-alert');
        if (!alert) {
            alert = document.createElement('div');
            alert.id = 'es-id-pdf-compatibility-alert';
            alert.className = 'alert alert-danger m-3';
            alert.setAttribute('role', 'alert');
            document.body.insertBefore(alert, document.body.firstChild);
        }
        alert.textContent = messages.getAttribute('data-' + messageKey);
    }

    document.addEventListener('DOMContentLoaded', showMessage);
    window.addEventListener('error', function (event) {
        var target = event.target;
        if (target && target.id === 'es-id-pdf-js-bootstrap') {
            compatibility.complete(event.error || new Error());
        } else if (/pdfjs-dist|PdfJsBootstrap\.js/.test(event.filename || '') && event.error) {
            compatibility.complete(event.error);
        }
    }, true);

    if (!('noModule' in document.createElement('script')) ||
        typeof window.globalThis === 'undefined' || typeof window.Promise !== 'function' ||
        typeof window.fetch !== 'function' || typeof window.URL !== 'function') {
        compatibility.incompatible = true;
        compatibility.complete(new Error());
    }
})(window, document);
