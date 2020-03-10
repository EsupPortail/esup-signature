export class Preview {
    url; //remove
    type;
    size;
    downloadUrl;
    caption;
    filename;
    key;

    constructor(url, type, size, downloadUrl, caption, filename, key) {
        this.url = url;
        this.type = type;
        this.size = size;
        this.downloadUrl = downloadUrl;
        this.caption = caption;
        this.filename = filename;
        this.key = key;
    }
}

