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
        this.input.on('change', e => this.updateZipOptionVisibility());
        this.updateZipOptionVisibility();
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
            showBrowse: !readOnly,
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
                'pdf': '<i class="fi fi-rr-file-pdf text-danger"></i>',
                'doc': '<i class="fi fi-rr-file-word text-primary"></i>',
                'xls': '<i class="fi fi-rr-file-excel text-success"></i>',
                'ppt': '<i class="fi fi-rr-file-powerpoint text-danger"></i>',
                'zip': '<i class="fi fi-rr-file-zipper text-warning"></i>',
                'htm': '<i class="fi fi-rr-file-code text-info"></i>',
                'txt': '<i class="fi fi-rr-document text-info"></i>',
                'mov': '<i class="fi fi-rr-video-camera text-warning"></i>',
                'mp3': '<i class="fi fi-rr-file-audio text-warning"></i>',
                'img': '<i class="fi fi-rr-file-image text-secondary"></i>',
                'signed': '<i class="fi fi-rr-document-signed text-success"></i>',
                'other': '<i class="fi fi-rr-file text-muted"></i>'
            },
            previewFileExtSettings: {
                'other': function() {
                    return true;
                },
                'signed': function(ext) {
                    return ext.match(/(sce)$/i);
                },
                'pdf': function(ext) {
                    return ext.match(/(pdf)$/i);
                },
                'doc': function(ext) {
                    return ext.match(/(doc|docx|odt|rtf)$/i);
                },
                'xls': function(ext) {
                    return ext.match(/(xls|xlsx|ods)$/i);
                },
                'ppt': function(ext) {
                    return ext.match(/(odp|ppt|pptx)$/i);
                },
                'zip': function(ext) {
                    return ext.match(/(zip|rar|tar|gzip|gz|7z|bz2|xz|tgz)$/i);
                },
                'htm': function(ext) {
                    return ext.match(/(htm|html|xhtml)$/i);
                },
                'txt': function(ext) {
                    return ext.match(/(txt|ini|csv|log|md|java|php|js|css|json|xml|yml|yaml)$/i);
                },
                'mov': function(ext) {
                    return ext.match(/(avi|mpg|mpeg|mkv|mov|mp4|3gp|webm|wmv)$/i);
                },
                'img': function(ext) {
                    return ext.match(/(jpg|jpeg|gif|png|svg|bmp|webp|tif|tiff)$/i);
                },
                'mp3': function(ext) {
                    return ext.match(/(mp3|wav|ogg|oga|m4a|aac|flac)$/i);
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
        this.updateZipOptionVisibility();
    }

    getUnzipOptionContainer() {
        return this.input.closest('form').find('[data-es-unzip-option-container]').first();
    }

    getUnzipOption() {
        return this.input.closest('form').find('[data-es-unzip-option]').first();
    }

    getSelectedFiles() {
        try {
            return Object.values(this.input.fileinput('getFileStack') || {}).filter(file => file != null);
        } catch (e) {
            return Array.from(this.input[0]?.files || []);
        }
    }

    isZipFile(file) {
        if(file == null) {
            return false;
        }
        const fileName = (file.name || '').toLowerCase();
        const contentType = (file.type || '').toLowerCase();
        return fileName.endsWith('.zip') || contentType.includes('zip');
    }

    updateZipOptionVisibility() {
        const unzipOptionContainer = this.getUnzipOptionContainer();
        const unzipOption = this.getUnzipOption();
        if(unzipOptionContainer.length === 0 || unzipOption.length === 0) {
            return;
        }
        const hasZip = this.getSelectedFiles().some(file => this.isZipFile(file));
        unzipOptionContainer.toggleClass('d-none', !hasZip);
        unzipOption.prop('checked', hasZip);
    }

    changeUploadUrl() {
        let uploadUrl = "/ws-secure/global/add-docs/" + this.signBookId + "?" + this.csrf.parameterName + "=" + this.csrf.token;
        const unzipOption = this.getUnzipOption();
        if(unzipOption.length > 0 && unzipOption.is(':checked')) {
            uploadUrl += "&unzip=true";
        }
        return uploadUrl;
    }
}
