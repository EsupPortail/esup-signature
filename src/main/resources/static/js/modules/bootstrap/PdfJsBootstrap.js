if (globalThis.pdfjsLib?.GlobalWorkerOptions) {
    globalThis.pdfjsLib.GlobalWorkerOptions.workerSrc = "/webjars/pdfjs-dist/legacy/build/pdf.worker.min.mjs";
}
