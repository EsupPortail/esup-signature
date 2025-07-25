import {DocumentPreview} from "../../prototypes/DocumentPreview.js?version=@version@";
import {CsrfToken} from "../../prototypes/CsrfToken.js?version=@version@";
import {EventFactory} from "./EventFactory.js?version=@version@";

export default class FilesInput extends EventFactory {

    constructor(input, maxSize, csrf, documents, readOnly, signBookId) {
        super();
        this.first = true;
        this.input = input;
        this.signBookId = signBookId;
        this.maxSize = 1000000;
        if(maxSize != null) {
            this.maxSize = maxSize / 1000;
        }
        console.info("Enable Bootstrap FileInput for : " + name);
        this.csrf = new CsrfToken(csrf);
        // this.uploadUrl = '/ws-secure/global/add-docs?'+ this.csrf.parameterName + '=' + this.csrf.token;
        this.title = $("#title-wiz");
        this.fileInput = this.initFileInput(documents, readOnly);
        this.initListeners();
    }

    initListeners() {
        this.input.on('fileselect', e => this.checkUniqueFile());
        this.input.on('fileremoved', e => this.checkUniqueFile());
        this.input.on('filecleared', e => this.checkUniqueFile());
        this.input.on('fileclear', e => this.input.fileinput('unlock'));
    }

    initFileInput(documents, readOnly) {
        let urls = [];
        let previews = [];
        let self = this;
        if (documents != null) {
            documents.forEach(function (document) {
                let type;
                urls.push("/ws-secure/global/get-file/" + document.id);
                switch (document.contentType.split('/')[1]) {
                    case "pdf" :
                        type = "pdf";
                        break;
                    case "gif" || "jpeg" || "png" :
                        type = "image";
                        break;
                    case "doc" || "xls" || "ppt" :
                        type = "office";
                        break;
                    default :
                        type = "other";
                }
                let deleteUrl = "";
                if(!readOnly) {
                    deleteUrl = "/ws-secure/global/remove-doc/" + document.id + "?" + self.csrf.parameterName + "=" + self.csrf.token;
                }
                let preview = new DocumentPreview(
                    type,
                    document.size,
                    document.contentType,
                    document.fileName,
                    deleteUrl,
                    document.id,
                    "/ws-secure/global/get-file/" + document.id,
                    document.fileName
                    );
                previews.push(preview);
            });
        }
        let fileInput = this.input.fileinput({
            language: "fr",
            showCaption: false,
            minFileSize: 1,
            maxFileSize: this.maxSize,
            showClose: false,
            showCancel: false,
            showBrowse: true,
            showUpload: false,
            showUploadStats: false,
            progressDelay: 50,
            showRemove: !readOnly,
            dropZoneEnabled: !readOnly,
            browseOnZoneClick: !readOnly,
            uploadUrl: function() {
                return self.changeUploadUrl();
            },
            // enableResumableUpload: true,
            maxAjaxThreads: 1,
            uploadAsync: true,
            theme: 'explorer-fa6',
            pdfRendererUrl: 'http://plugins.krajee.com/pdfjs/web/viewer.html',
            initialPreview: urls,
            initialPreviewConfig : previews,
            initialPreviewAsData: true,
            initialPreviewShowDelete: !readOnly,
            tabIndexConfig: {
                browse: 0,
                remove: 500,
                upload: 500,
                cancel: null,
                pause: null,
                modal: -1
            },
            overwriteInitial: false,
            preferIconicPreview: true,
            allowedFileTypes : [],
            previewFileIconSettings: {
                'pdf': '<i class="fa-solid fa-file-pdf text-danger fa-xl"></i>',
                'doc': '<i class="fa-solid fa-file-word text-primary fa-xl"></i>',
                'xls': '<i class="fa-solid fa-file-excel text-success fa-xl"></i>',
                'ppt': '<i class="fa-solid fa-file-powerpoint text-danger fa-xl"></i>',
                'zip': '<i class="fa-solid fa-file-archive text-muted fa-xl"></i>',
                'htm': '<i class="fa-solid fa-file-code text-info fa-xl"></i>',
                'txt': '<i class="fa-solid fa-file-alt text-info fa-xl"></i>',
                'mov': '<i class="fa-solid fa-file-video text-warning fa-xl"></i>',
                'mp3': '<i class="fa-solid fa-file-audio text-warning fa-xl"></i>',
                'jpg': '<i class="fa-solid fa-file-image text-danger fa-xl"></i>',
                'gif': '<i class="fa-solid fa-file-image text-muted fa-xl"></i>',
                'png': '<i class="fa-solid fa-file-image text-primary fa-xl"></i>',
                'other': '<i class="fa-solid fa-file text-muted fa-xl"></i>'
            },
            previewFileExtSettings: {
                'other': function() {
                    return true;
                },
                'pdf': function(ext) {
                    return ext.match(/(pdf)$/i);
                },
                'doc': function(ext) {
                    return ext.match(/(doc|docx|odt)$/i);
                },
                'xls': function(ext) {
                    return ext.match(/(xls|xlsx)$/i);
                },
                'ppt': function(ext) {
                    return ext.match(/(odp|ppt|pptx)$/i);
                },
                'zip': function(ext) {
                    return ext.match(/(zip|rar|tar|gzip|gz|7z)$/i);
                },
                'htm': function(ext) {
                    return ext.match(/(htm|html)$/i);
                },
                'txt': function(ext) {
                    return ext.match(/(txt|ini|csv|java|php|js|css)$/i);
                },
                'mov': function(ext) {
                    return ext.match(/(avi|mpg|mkv|mov|mp4|3gp|webm|wmv)$/i);
                },
                'mp3': function(ext) {
                    return ext.match(/(mp3|wav)$/i);
                }
            },
            fileActionSettings: {
                showDrag: false,
                showZoom: function(config) {
                    return config.type === 'pdf' || config.type === 'image';
                },
                showRemove: !readOnly
            }
        });
        this.input.on('filezoomshown', function(event, params) {
            $('.kv-zoom-body').each(function (e){
                $(this).removeAttr('style');
            });
        });
        return fileInput;
    }

    checkUniqueFile() {
        let nbFiles = this.input.fileinput('getFilesCount', true);
        let compare = 1;
        if(nbFiles > compare) {
            $('#forceAllSign').removeClass('d-none');
        } else {
            $('#forceAllSign').addClass('d-none');
        }
    }

    changeUploadUrl() {
        return "/ws-secure/global/add-docs/" + this.signBookId + "?" + this.csrf.parameterName + "=" + this.csrf.token
    }
}
