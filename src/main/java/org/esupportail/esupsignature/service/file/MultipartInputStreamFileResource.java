package org.esupportail.esupsignature.service.file;

import org.springframework.core.io.InputStreamResource;

import java.io.IOException;
import java.io.InputStream;

public class MultipartInputStreamFileResource extends InputStreamResource {

    private final String filename;

    public MultipartInputStreamFileResource(InputStream inputStream, String filename) {
        super(inputStream);
        this.filename = filename;
    }

    @Override
    public String getFilename() {
        return this.filename;
    }

    @Override
    public long contentLength() throws IOException {
        return -1; // we do not want to generally read the whole stream into memory ...
    }
}