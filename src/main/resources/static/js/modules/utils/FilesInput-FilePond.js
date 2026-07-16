import * as FilePond from "/webjars/filepond/4.32.12/dist/filepond.esm.js";
import filePondFrLocale from "/webjars/filepond/4.32.12/locale/fr-fr.js";
import {CsrfToken} from "../../prototypes/CsrfToken.js?version=@version@";
import {EventFactory} from "./EventFactory.js?version=@version@";

const FILES_INPUT_ADAPTER_KEY = "esup-files-input-adapter";

export default class FilesInputFilePond extends EventFactory {

    constructor(input, maxSize, csrf, documents, readOnly, signBookId) {
        super();
        this.input = input;
        this.signBookId = signBookId;
        this.csrf = new CsrfToken(csrf);
        this.title = $("#title-wiz");
        this.maxSizeBytes = maxSize != null ? Number(maxSize) : 1000000000;
        this.readOnly = readOnly === true;
        this.adapter = new FilePondFilesInputAdapter(this);
        this.fileInput = this.adapter.init(documents || []);
        this.input.data(FILES_INPUT_ADAPTER_KEY, this.adapter);
        FilesInputFilePond.installFileInputFacade();
        this.initListeners();
    }

    static installFileInputFacade() {
        if ($.fn.fileinput?.esupFilePondFacade === true) {
            return;
        }
        const originalFileInput = $.fn.fileinput;
        $.fn.fileinput = function(command, ...args) {
            const adapter = this.data(FILES_INPUT_ADAPTER_KEY);
            if (adapter != null && typeof command === "string") {
                return adapter.command(command, ...args);
            }
            if (adapter != null && command == null) {
                return adapter.pond;
            }
            if (typeof originalFileInput === "function") {
                return originalFileInput.apply(this, [command, ...args]);
            }
            return this;
        };
        $.fn.fileinput.esupFilePondFacade = true;
    }

    initListeners() {
        this.input.on('fileselect', e => this.checkUniqueFile());
        this.input.on('fileremoved', e => this.checkUniqueFile());
        this.input.on('filecleared', e => this.checkUniqueFile());
        this.input.on('fileclear', e => this.input.fileinput('unlock'));
        this.input.on('change', e => this.updateZipOptionVisibility());
        this.updateZipOptionVisibility();
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
        const adapter = this.input.data(FILES_INPUT_ADAPTER_KEY);
        if (adapter != null) {
            return adapter.getNativeFiles();
        }
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
        const fileName = (file.name || file.filename || '').toLowerCase();
        const contentType = (file.type || file.fileType || '').toLowerCase();
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

class FilePondFilesInputAdapter {

    constructor(filesInput) {
        this.filesInput = filesInput;
        this.input = filesInput.input;
        this.readOnly = filesInput.readOnly;
        this.pond = null;
        this.locked = false;
        this.uploadPromise = null;
    }

    init(documents) {
        console.info("Enable FilePond for : " + this.input.attr("name"));
        this.pond = FilePond.create(this.input[0], {
            ...filePondFrLocale,
            allowBrowse: !this.readOnly,
            allowDrop: !this.readOnly,
            allowPaste: !this.readOnly,
            allowMultiple: this.input.prop("multiple"),
            allowReorder: true,
            allowRemove: !this.readOnly,
            allowRevert: false,
            credits: false,
            files: documents.map(document => this.toInitialFile(document)),
            instantUpload: false,
            maxParallelUploads: 1,
            name: this.input.attr("name") || "multipartFiles",
            server: {
                process: (fieldName, file, metadata, load, error, progress, abort) => {
                    return this.processFile(fieldName, file, load, error, progress, abort);
                }
            },
            beforeAddFile: file => this.validateFile(file),
            beforeRemoveFile: file => this.removeServerFile(file),
            onaddfile: (error, file) => {
                if (error == null) {
                    this.decorateFileIcon(file);
                    if (file?.origin === FilePond.FileOrigin.INPUT) {
                        this.input.trigger("fileselect", [file]);
                        this.input.trigger("filebatchselected", [this.getNativeFiles()]);
                        this.input.trigger("change");
                    }
                }
            },
            onremovefile: (error, file) => {
                if (error == null) {
                    this.input.trigger("fileremoved", [file]);
                    this.input.trigger("change");
                }
            },
            onactivatefile: file => this.openInitialFile(file)
        });
        this.pond.element.classList.add("esup-filepond");
        if (this.readOnly) {
            this.pond.element.classList.add("esup-filepond-readonly");
        }
        return this.pond;
    }

    command(command, ...args) {
        switch(command) {
            case "getFilesCount":
                return this.pond.getFiles().length;
            case "getFileList":
                return this.asBootstrapFileList(this.getNativeFiles());
            case "getFileStack":
                return this.getNativeFiles();
            case "upload":
                return this.upload();
            case "clear":
                return this.clear();
            case "clearFileStack":
                return this.clearFileStack();
            case "readFiles":
                return this.readFiles(args[0]);
            case "cancel":
                return this.cancel();
            case "unlock":
                this.locked = false;
                return this.input;
            case "lock":
                this.locked = true;
                return this.input;
            default:
                console.debug("Unsupported FilePond fileinput command", command);
                return this.input;
        }
    }

    getNativeFiles() {
        return this.pond.getFiles()
            .filter(file => file.origin === FilePond.FileOrigin.INPUT)
            .map(file => file.file)
            .filter(file => file != null);
    }

    asBootstrapFileList(files) {
        const fileList = Array.from(files || []);
        fileList.size = function() {
            return this.length;
        };
        return fileList;
    }

    upload() {
        if (this.locked) {
            return this.uploadPromise;
        }
        this.locked = true;
        this.uploadPromise = this.pond.processFiles()
            .then(files => {
                this.locked = false;
                this.input.trigger("filebatchuploadsuccess", [files]);
                return files;
            })
            .catch(error => {
                this.locked = false;
                this.input.trigger("fileuploaderror", [error]);
                throw error;
            });
        return this.uploadPromise;
    }

    clear() {
        this.input.trigger("fileclear");
        this.pond.removeFiles();
        this.input.trigger("filecleared");
        this.input.trigger("change");
        return this.input;
    }

    clearFileStack() {
        this.pond.getFiles()
            .filter(file => file.origin === FilePond.FileOrigin.INPUT)
            .forEach(file => this.pond.removeFile(file.id));
        return this.input;
    }

    readFiles(files) {
        const nativeFiles = Array.from(files || []);
        if (nativeFiles.length === 0) {
            return this.input;
        }
        this.pond.addFiles(nativeFiles)
            .then(() => this.input.trigger("filebatchselected", [nativeFiles]))
            .catch(error => this.input.trigger("fileuploaderror", [error]));
        return this.input;
    }

    cancel() {
        this.pond.getFiles()
            .filter(file => typeof file.abortProcessing === "function")
            .forEach(file => file.abortProcessing());
        this.locked = false;
        return this.input;
    }

    validateFile(fileItem) {
        const file = fileItem.file || fileItem;
        if (file?.size == null || this.filesInput.maxSizeBytes <= 0) {
            return true;
        }
        if (file.size <= this.filesInput.maxSizeBytes) {
            return true;
        }
        const maxSize = this.formatSize(this.filesInput.maxSizeBytes);
        const message = "Le fichier " + (file.name || "") + " dépasse la taille maximale autorisée (" + maxSize + ").";
        if (typeof bootbox !== "undefined") {
            bootbox.alert(message);
        } else {
            window.alert(message);
        }
        this.input.trigger("fileuploaderror", [message]);
        return false;
    }

    processFile(fieldName, file, load, error, progress, abort) {
        const request = new XMLHttpRequest();
        request.open("POST", this.filesInput.changeUploadUrl(), true);
        request.setRequestHeader(this.filesInput.csrf.headerName, this.filesInput.csrf.token);
        request.upload.onprogress = event => {
            progress(event.lengthComputable, event.loaded, event.total);
        };
        request.onload = () => {
            if (request.status >= 200 && request.status < 300) {
                load(request.responseText);
                return;
            }
            const message = request.responseText || request.statusText;
            this.input.trigger("fileuploaderror", [message]);
            error(message);
        };
        request.onerror = () => {
            const message = request.responseText || "Erreur durant le transfert";
            this.input.trigger("fileuploaderror", [message]);
            error(message);
        };
        const formData = new FormData();
        formData.append(fieldName, file, file.name);
        request.send(formData);
        return {
            abort: () => {
                request.abort();
                abort();
            }
        };
    }

    removeServerFile(file) {
        const deleteUrl = file?.getMetadata?.("deleteUrl");
        if (this.readOnly || !deleteUrl) {
            return true;
        }
        return fetch(deleteUrl, {
            method: "POST",
            headers: {
                [this.filesInput.csrf.headerName]: this.filesInput.csrf.token
            }
        }).then(response => response.ok);
    }

    openInitialFile(file) {
        const downloadUrl = file?.getMetadata?.("downloadUrl");
        if (downloadUrl) {
            window.open(downloadUrl, "_blank");
        }
    }

    decorateFileIcon(file, attempts = 8) {
        window.setTimeout(() => {
            const item = document.getElementById("filepond--item-" + file.id);
            const fileInfo = item?.querySelector(".filepond--file-info");
            if (item == null || fileInfo == null || item.querySelector(".esup-filepond-file-icon") != null) {
                if (attempts > 0 && (item == null || fileInfo == null)) {
                    this.decorateFileIcon(file, attempts - 1);
                }
                return;
            }
            const icon = document.createElement("i");
            icon.className = "esup-filepond-file-icon " + this.getIconClass(file);
            icon.setAttribute("aria-hidden", "true");
            fileInfo.before(icon);
        }, 50);
    }

    getIconClass(file) {
        const ext = (file?.fileExtension || this.getExtension(file?.filename) || "").toLowerCase();
        if (ext.match(/^(sce)$/i)) {
            return "fi fi-rr-document-signed text-success";
        }
        if (ext.match(/^(pdf)$/i)) {
            return "fi fi-rr-file-pdf text-danger";
        }
        if (ext.match(/^(doc|docx|odt|rtf)$/i)) {
            return "fi fi-rr-file-word text-primary";
        }
        if (ext.match(/^(xls|xlsx|ods)$/i)) {
            return "fi fi-rr-file-excel text-success";
        }
        if (ext.match(/^(odp|ppt|pptx)$/i)) {
            return "fi fi-rr-file-powerpoint text-danger";
        }
        if (ext.match(/^(zip|rar|tar|gzip|gz|7z|bz2|xz|tgz)$/i)) {
            return "fi fi-rr-file-zipper text-warning";
        }
        if (ext.match(/^(htm|html|xhtml)$/i)) {
            return "fi fi-rr-file-code text-info";
        }
        if (ext.match(/^(txt|ini|csv|log|md|java|php|js|css|json|xml|yml|yaml)$/i)) {
            return "fi fi-rr-document text-info";
        }
        if (ext.match(/^(avi|mpg|mpeg|mkv|mov|mp4|3gp|webm|wmv)$/i)) {
            return "fi fi-rr-video-camera text-warning";
        }
        if (ext.match(/^(mp3|wav|ogg|oga|m4a|aac|flac)$/i)) {
            return "fi fi-rr-file-audio text-warning";
        }
        if (ext.match(/^(jpg|jpeg|gif|png|svg|bmp|webp|tif|tiff)$/i)) {
            return "fi fi-rr-file-image text-secondary";
        }
        return "fi fi-rr-file text-muted";
    }

    getExtension(filename) {
        if (!filename || !filename.includes(".")) {
            return "";
        }
        return filename.split(".").pop();
    }

    toInitialFile(document) {
        const downloadUrl = "/ws-secure/global/get-file/" + document.id;
        return {
            source: String(document.id),
            options: {
                type: "local",
                file: {
                    name: document.fileName,
                    size: document.size,
                    type: document.contentType
                },
                metadata: {
                    documentId: document.id,
                    deleteUrl: this.readOnly ? "" : "/ws-secure/global/remove-doc/" + document.id + "?" + this.filesInput.csrf.parameterName + "=" + this.filesInput.csrf.token,
                    downloadUrl
                }
            }
        };
    }

    formatSize(size) {
        if (size >= 1000000000) {
            return (size / 1000000000).toFixed(1).replace(".0", "") + " Go";
        }
        if (size >= 1000000) {
            return (size / 1000000).toFixed(1).replace(".0", "") + " Mo";
        }
        return Math.ceil(size / 1000) + " Ko";
    }

}
