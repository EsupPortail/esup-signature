package org.esupportail.esupsignature.service.security.oauth;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

public class JwtHttpMessageConverter extends AbstractGenericHttpMessageConverter<String> {

    public JwtHttpMessageConverter() {
        super(MediaType.valueOf("application/jwt"));
    }

    @Override
    protected String readInternal(Class<? extends String> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        return getBodyAsString(inputMessage.getBody());
    }

    @Override
    public String read(Type type, Class<?> contextClass, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        return readInternal(null, inputMessage);
    }

    private String getBodyAsString(InputStream bodyStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[64];
        int len;
        while ((len = bodyStream.read(chunk)) != -1) {
            buffer.write(chunk, 0, len);
        }
        return buffer.toString(StandardCharsets.US_ASCII);
    }

    @Override
    protected void writeInternal(String stringObjectMap, Type type, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        throw new UnsupportedOperationException();
    }

}