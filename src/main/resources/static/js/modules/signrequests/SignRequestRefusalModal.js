function toggleSource() {
    const useExisting = $("#docSourceExisting").is(":checked");
    $("#existingBlock").toggle(useExisting);
    $("#uploadBlock").toggle(!useExisting);
    $("#multipartFiles1").prop("required", !useExisting);
}

$('input[name="documentSource"]').on("change", toggleSource);
toggleSource();

$("#multipartFiles1").fileinput({
    language: "fr",
    showCaption: false,
    minFileSize: 1,
    showClose: false,
    showCancel: false,
    showBrowse: true,
    showUpload: false,
    showUploadStats: false,
    progressDelay: 50,
    maxAjaxThreads: 1,
    uploadAsync: false,
    theme: "explorer-fa6",
    pdfRendererUrl: "http://plugins.krajee.com/pdfjs/web/viewer.html",
    overwriteInitial: false,
    preferIconicPreview: true,
    showRemove: true,
    dropZoneEnabled: true,
    browseOnZoneClick: true,
    allowedFileTypes: [],
    previewFileIconSettings: {
        pdf: '<i class="fi fi-rr-file-pdf text-danger"></i>',
        doc: '<i class="fi fi-rr-file-word text-primary"></i>',
        xls: '<i class="fi fi-rr-file-excel text-success"></i>',
        ppt: '<i class="fi fi-rr-file-powerpoint text-danger"></i>',
        zip: '<i class="fi fi-rr-file-zipper text-warning"></i>',
        htm: '<i class="fi fi-rr-file-code text-info"></i>',
        txt: '<i class="fi fi-rr-text text-info"></i>',
        mov: '<i class="fi fi-rr-video-camera text-warning"></i>',
        mp3: '<i class="fi fi-rr-file-audio text-warning"></i>',
        jpg: '<i class="fi fi-rr-file-image text-danger"></i>',
        gif: '<i class="fi fi-rr-file-image text-muted"></i>',
        png: '<i class="fi fi-rr-file-image text-primary"></i>',
        signed: '<i class="fi fi-rr-document-signed text-success"></i>',
        other: '<i class="fi fi-rr-file text-muted"></i>'
    },
    previewFileExtSettings: {
        other: () => true,
        signed: ext => ext.match(/(sce)$/i),
        pdf: ext => ext.match(/(pdf)$/i),
        doc: ext => ext.match(/(doc|docx|odt)$/i),
        xls: ext => ext.match(/(xls|xlsx)$/i),
        ppt: ext => ext.match(/(odp|ppt|pptx)$/i),
        zip: ext => ext.match(/(zip|rar|tar|gzip|gz|7z)$/i),
        htm: ext => ext.match(/(htm|html)$/i),
        txt: ext => ext.match(/(txt|ini|csv|java|php|js|css)$/i),
        mov: ext => ext.match(/(avi|mpg|mkv|mov|mp4|3gp|webm|wmv)$/i),
        mp3: ext => ext.match(/(mp3|wav)$/i)
    }
});
