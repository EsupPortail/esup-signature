import {Preview} from "../prototypes/preview.js";

export class FilesInput {

    constructor(input, name, document, readOnly, csrfParameterName, csrfToken) {
        console.info("Enable file input for : " + name);
        this.input = input;
        this.name = name;
        this.csrfParameterName = csrfParameterName;
        this.csrfToken = csrfToken;
        this.uploadUrl = '/ws/add-docs-in-sign-book-unique/' + this.name + '?'+ csrfParameterName + '=' + csrfToken;
        this.initListeners();
        this.initFileInput(document, readOnly);
    }

    initListeners() {
        $("#fileUpload").on('click', e => this.fileUpload());

        //
        // this.input.on('filebatchuploadcomplete', function (event, file, previewId, index, reader) {
        //     location.reload();
        // });
        //
        // this.input.on('fileuploaded', function (event, file, previewId, index, reader) {
        //     location.reload();
        // });

        // this.input.on('fileloaded', function (event, file, previewId, index, reader) {
        //     this.input.fileinput('upload');
        // });
        //
        // this.input.on('filedeleted', function (event, id, index) {
        //     location.reload();
        // });

        this.input.on('fileloaded', e => this.checkUniqueFile());


        $('#unique :checkbox').change(e => this.changerUploadMethod());

    }

    // [# th:if="${signRequest.status.name() != 'draft' || user.eppn != signRequest.createBy || signRequest.signedDocuments.size() > 0}"]
    initFileInput(documents, readOnly) {
        let urls = [];
        let previews = [];
        let type = 'other';
        if (documents != null) {
            documents.forEach(function (document) {
                console.log(document);
                urls.push('/user/documents/getfile/' + document.id);
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

                let preview = new Preview(
                    '/user/signrequests/remove-doc/' + document.id,
                    document.contentType,
                    document.size,
                    document.fileName,
                    document.id);
                previews.push(preview);
                console.info("addind :" + document.fileName + ", " + type + ", " + JSON.stringify(preview));
            });
        }

        console.log(urls);

        this.input.fileinput({
            language: "fr",
            showCaption: false,
            showClose: false,
            showBrowse: !readOnly,
            showUpload: !readOnly,
            showRemove: !readOnly,
            dropZoneEnabled: !readOnly,
            browseOnZoneClick: !readOnly,
            uploadUrl: this.uploadUrl,
            uploadAsync: false,
            overwriteInitial: false,
            theme: 'explorer-fas',
            pdfRendererUrl: 'http://plugins.krajee.com/pdfjs/web/viewer.html',
            initialPreview: [urls],
            initialPreviewConfig: [previews],
            initialPreviewAsData: true,
            initialPreviewFileType: type,
            preferIconicPreview: true,
            previewFileIconSettings: { // configure your icon file extensions
                'doc': '<i class="fas fa-file-word text-primary  fa-2x"></i>',
                'xls': '<i class="fas fa-file-excel text-success  fa-2x"></i>',
                'docx': '<i class="fas fa-file-word text-primary  fa-2x"></i>',
                'xlsx': '<i class="fas fa-file-excel text-success  fa-2x"></i>',
                'ppt': '<i class="fas fa-file-powerpoint text-danger  fa-2x"></i>',
                'pdf': '<i class="fas fa-file-pdf text-danger  fa-2x"></i>',
                'zip': '<i class="fas fa-file-archive text-muted  fa-2x"></i>',
                'htm': '<i class="fas fa-file-code text-info  fa-2x"></i>',
                'txt': '<i class="fas fa-file-alt text-info fa-2x"></i>',
                'mov': '<i class="fas fa-file-video text-warning fa-2x"></i>',
                'mp3': '<i class="fas fa-file-audio text-warning  fa-2x"></i>',
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
                    return ext.match(/(ppt|pptx)$/i);
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
        this.input.on('filebatchuploadsuccess', function(event, previewId, index, fileId) {
            console.info("submit form");
            $('#wiz2Form').submit();
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
        console.log('change');
        if ($('#unique :checkbox').is(":checked")){
            $('#multipartFile').fileinput('refresh', {
                uploadUrl: '/ws/add-docs-in-sign-book-group/' + this.name + '?'+ this.csrfParameterName + '=' + this.csrfToken
            });
        } else {
            $('#multipartFile').fileinput('refresh', {
                uploadUrl: '/ws/add-docs-in-sign-book-unique/' + this.name + '?'+ this.csrfParameterName + '=' + this.csrfToken
        });
        }
    }

}
