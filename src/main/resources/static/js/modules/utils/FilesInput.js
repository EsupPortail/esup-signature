import {DocumentPreview} from "../../prototypes/DocumentPreview.js";
import {CsrfToken} from "../../prototypes/CsrfToken.js";
import {EventFactory} from "./EventFactory.js";

export default class FilesInput extends EventFactory {

    constructor(input, workflowName, name, documents, readOnly, csrf, signRequestId) {
        super();
        console.info("enable complete file input for : " + name);
        this.input = input;
        this.name = name;
        if(this.name == null) {
            this.name = "Demande personnalisée"
        }
        this.workflowName = workflowName;
        this.csrf = new CsrfToken(csrf);
        this.async = true;
        this.uploadUrl = null;
        if(signRequestId == null) {
            this.uploadUrl = '/user/signbooks/add-docs-in-sign-book-unique/' + this.workflowName + '/' + this.name + '?'+ this.csrf.parameterName + '=' + this.csrf.token;
        } else {
            this.async = false;
            this.uploadUrl = '/user/signrequests/add-docs/' + signRequestId + '?'+ this.csrf.parameterName + '=' + this.csrf.token;
        }
        this.initListeners();
        this.initFileInput(documents, readOnly);
    }

    initListeners() {
        $("#fileUpload").on('click', e => this.fileUpload());
        if(!this.async) {
            console.info("set async");
            this.input.on('fileloaded', e => this.uploadFile());
        }
        this.input.on('fileloaded', e => this.checkUniqueFile());
        $('#unique :checkbox').change(e => this.changerUploadMethod());

    }

    uploadFile() {
        this.input.fileinput('upload');
    }

    // [# th:if="${signRequest.status != ${T(org.esupportail.esupsignature.entity.enums.SignRequestStatus).draft} || user.eppn != signRequest.createBy || signRequest.signedDocuments.size() > 0}"]
    initFileInput(documents, readOnly) {
        let urls = [];
        let previews = [];
        let csrf = this.csrf
        if (documents != null) {
            documents.forEach(function (document) {
                let type;
                urls.push("/user/signrequests/get-file/" + document.id);
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

                let preview = new DocumentPreview(
                    type,
                    document.size,
                    document.contentType,
                    document.fileName,
                    "/user/signrequests/remove-doc/" + document.id + "/?" + csrf.parameterName + "=" + csrf.token,
                    document.id,
                    "/user/signrequests/get-file/" + document.id,
                    document.fileName
                    );
                previews.push(preview);
            });
        }
        this.input.fileinput({
            language: "fr",
            showCaption: false,
            showClose: false,
            showBrowse: !readOnly,
            showUpload: false,
            showRemove: !readOnly,
            dropZoneEnabled: !readOnly,
            browseOnZoneClick: !readOnly,
            uploadUrl: this.uploadUrl,
            uploadAsync: false,
            theme: 'explorer-fas',
            pdfRendererUrl: 'http://plugins.krajee.com/pdfjs/web/viewer.html',
            initialPreview: urls,
            initialPreviewConfig : previews,
            initialPreviewAsData: true,
            initialPreviewFileType: 'other',
            overwriteInitial: false,
            preferIconicPreview: true,
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
                'png': '<i class="fas fa-file-image text-primary fa-2x"></i>'
            },
            previewFileExtSettings: { // configure the logic for determining icon file extensions
                'doc': function(ext) {
                    return ext.match(/(doc|docx)$/i);
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
                    if (config.type === 'pdf' || config.type === 'image') {
                        return true;
                    }
                    return false;
                },
                showRemove: !readOnly
            }
        });
    }

    fileUpload() {
        console.info("file upload");
        this.input.fileinput('upload');
        let self = this;
        this.input.on('filebatchuploadsuccess', function(event, data) {
            console.info("submit form");
            self.fireEvent("uploaded", data.response);
        });
    }



    checkUniqueFile() {
        if(this.input.fileinput('getFilesCount') > 1) {
            $('#unique').removeClass('d-none');
        } else {
            $('#unique').addClass('d-none');
        }
    }

    changerUploadMethod () {
        console.group('change upload url');
        if ($('#unique :checkbox').is(":checked")){
            console.info('to group mode');
            this.input.fileinput('refresh', {
                uploadUrl: '/user/signbooks/add-docs-in-sign-book-group/' + this.name + '?'+ this.csrf.parameterName + '=' + this.csrf.token
            });
        } else {
            console.info('to unique mode');
            this.input.fileinput('refresh', {
                uploadUrl: '/user/signbooks/add-docs-in-sign-book-unique/' + this.workflowName + '/' + this.name + '?'+ this.csrf.parameterName + '=' + this.csrf.token
        });
        }
        console.groupEnd();
    }

}
