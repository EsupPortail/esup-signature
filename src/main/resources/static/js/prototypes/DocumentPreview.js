export class DocumentPreview {

    _type;
    _size;
    _filetype;
    _caption;
    _url;
    _key;
    _downloadUrl;
    _filename;

    constructor(type, size, filetype, caption, url, key, downloadUrl, filename) {
        this._type = type;
        this._size = size;
        this._filetype = filetype;
        this._caption = caption;
        this._url = url;
        this._key = key;
        this._downloadUrl = downloadUrl;
        this._filename = filename;
    }


    get type() {
        return this._type;
    }

    set type(value) {
        this._type = value;
    }

    get size() {
        return this._size;
    }

    set size(value) {
        this._size = value;
    }

    get filetype() {
        return this._filetype;
    }

    set filetype(value) {
        this._filetype = value;
    }

    get caption() {
        return this._caption;
    }

    set caption(value) {
        this._caption = value;
    }

    get url() {
        return this._url;
    }

    set url(value) {
        this._url = value;
    }

    get key() {
        return this._key;
    }

    set key(value) {
        this._key = value;
    }

    get downloadUrl() {
        return this._downloadUrl;
    }

    set downloadUrl(value) {
        this._downloadUrl = value;
    }

    get filename() {
        return this._filename;
    }

    set filename(value) {
        this._filename = value;
    }
}

