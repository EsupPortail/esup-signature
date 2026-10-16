// Await both modules before allowing the viewer to start. The legacy build
// supplies polyfills, so check its capabilities only after it has loaded.
import('/webjars/pdfjs-dist/legacy/build/pdf.mjs').then(function (pdfjs) {
    pdfjs.GlobalWorkerOptions.workerSrc = '/webjars/pdfjs-dist/legacy/build/pdf.worker.min.mjs?version=' + encodeURIComponent(pdfjs.version);
    return import('/webjars/pdfjs-dist/legacy/web/pdf_viewer.mjs');
}).then(function () {
    if (typeof Promise.withResolvers !== 'function' || typeof window.DOMMatrix !== 'function' ||
        typeof window.Path2D !== 'function' || typeof window.ReadableStream !== 'function') {
        window.esupPdfCompatibility.incompatible = true;
        window.esupPdfCompatibility.complete(new Error());
        return;
    }
    window.esupPdfCompatibility.complete();
}).catch(function (error) {
    window.esupPdfCompatibility.complete(error);
});
