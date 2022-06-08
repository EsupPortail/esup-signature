import {DocumentPreview} from "../../prototypes/DocumentPreview.js?version=@version@";
import {CsrfToken} from "../../prototypes/CsrfToken.js?version=@version@";
import {EventFactory} from "./EventFactory.js?version=@version@";

export default class FilesInput extends EventFactory {

    constructor(input, workflowName, name, documents, readOnly, csrf, signRequestId) {
        super();
        this.input = input;
        this.name = name;
        if(this.name == null) {
            this.name = "Demande personnalisée"
        }
        console.info("enable complete file input for : " + name);
        this.workflowName = workflowName;
        if(workflowName === "") {
            this.workflowName = "custom";
        }
        this.csrf = new CsrfToken(csrf);
        this.async = false;
        this.uploadUrl = null;
        if(signRequestId != null) {
            this.async = false;
            this.uploadUrl = '/user/signrequests/add-docs/' + signRequestId + '?'+ this.csrf.parameterName + '=' + this.csrf.token;
        } else {
            if(workflowName != null) {
                this.async = false;
                this.uploadUrl = '/ws-secure/signrequests/start-workflow/' + this.workflowName + '?' + this.csrf.parameterName + '=' + this.csrf.token;
            }
        }
        this.title = $("#titleWiz");
        this.initFileInput(documents, readOnly);
        this.initListeners();
    }

    initListeners() {
        $("#fileUploadBtn").on('click', e => this.fileUpload());
        if(!this.async) {
            console.info("set async");
            this.input.on('fileloaded', e => this.uploadFile());
        }
        this.input.on('fileloaded', e => this.checkUniqueFile());
        this.input.on('fileremoved', e => this.checkUniqueFile());
        this.input.on('fileclear', e => this.input.fileinput('unlock'));
        this.input.on('filecleared', e => this.checkUniqueFile());
    }

    uploadFile() {
        this.input.fileinput('upload');
    }

    initFileInput(documents, readOnly) {
        let urls = [];
        let previews = [];
        let csrf = this.csrf
        if (documents != null) {
            documents.forEach(function (document) {
                let type;
                urls.push("/ws-secure/signrequests/get-file/" + document.id);
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
                    deleteUrl = "/ws-secure/signrequests/remove-doc/" + document.id + "/?" + csrf.parameterName + "=" + csrf.token;
                }
                let preview = new DocumentPreview(
                    type,
                    document.size,
                    document.contentType,
                    document.fileName,
                    deleteUrl,
                    document.id,
                    "/ws-secure/signrequests/get-file/" + document.id,
                    document.fileName
                    );
                previews.push(preview);
            });
        }
        this.input.fileinput({
            language: "fr",
            showCaption: false,
            minFileSize: 1,
            showClose: false,
            showBrowse: !readOnly,
            showUpload: false,
            showRemove: !readOnly,
            dropZoneEnabled: !readOnly && !this.async,
            browseOnZoneClick: !readOnly,
            uploadUrl: this.uploadUrl,
            uploadAsync: this.async,
            theme: 'explorer-fas',
            pdfRendererUrl: 'http://plugins.krajee.com/pdfjs/web/viewer.html',
            initialPreview: urls,
            initialPreviewConfig : previews,
            initialPreviewAsData: true,
            initialPreviewShowDelete: !readOnly,
            overwriteInitial: false,
            preferIconicPreview: true,
            allowedFileTypes : [],
            previewFileIconSettings: {
                'pdf': '<i class="fas fa-file-pdf text-danger fa-2x"></i>',
                'doc': '<i class="fas fa-file-word text-primary fa-2x"></i>',
                'xls': '<i class="fas fa-file-excel text-success fa-2x"></i>',
                'ppt': '<i class="fas fa-file-powerpoint text-danger fa-2x"></i>',
                'zip': '<i class="fas fa-file-archive text-muted fa-2x"></i>',
                'htm': '<i class="fas fa-file-code text-info fa-2x"></i>',
                'txt': '<i class="fas fa-file-alt text-info fa-2x"></i>',
                'mov': '<i class="fas fa-file-video text-warning fa-2x"></i>',
                'mp3': '<i class="fas fa-file-audio text-warning fa-2x"></i>',
                'jpg': '<i class="fas fa-file-image text-danger fa-2x"></i>',
                'gif': '<i class="fas fa-file-image text-muted fa-2x"></i>',
                'png': '<i class="fas fa-file-image text-primary fa-2x"></i>',
                'other': '<i class="fas fa-file text-muted fa-2x"></i>'
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
    }

    fileUpload() {
        console.info("file upload");
        let self = this;
        let title = $("#titleWiz");
        if(this.workflowName === "custom" && title.val() === "") {
            $(window).on('scroll', function(e){
                window.scrollTo(0,0);
            });
            $("#titleWizSubmit").click();

        } else {
            this.input.fileinput('upload');
            this.input.on('filebatchuploadsuccess', function(event, data) {
                console.info("submit form");
                self.fireEvent("uploaded", data.response);
            });
        }
    }

    checkUniqueFile() {
        let nbFiles = this.input.fileinput('getFilesCount', true);
        let compare = 1;
        if(!this.async) {
            compare = 0;
        }
        if(nbFiles > compare) {
            $('#unique').removeClass('d-none');
            $('#forceAllSign').removeClass('d-none');
            $('#forceAllSign2').removeClass('d-none');
        } else {
            $('#unique').addClass('d-none');
            $('#forceAllSign').addClass('d-none');
            $('#forceAllSign2').addClass('d-none');
        }
    }

}
