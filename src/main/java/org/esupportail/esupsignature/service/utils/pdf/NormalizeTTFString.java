package org.esupportail.esupsignature.service.utils.pdf;

import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class NormalizeTTFString {

    private static final Logger logger = LoggerFactory.getLogger(PdfService.class);

    private static TrueTypeFont ttf;

    public static NormalizeTTFString getInstance(InputStream ttfInputStream) throws IOException {
        TTFParser ttfParser = new TTFParser(true);
        ttf = ttfParser.parse(ttfInputStream);
        return new NormalizeTTFString(ttf);
    }

    public NormalizeTTFString(TrueTypeFont ttf) {
        NormalizeTTFString.ttf = ttf;
    }

    public String remove(String test) {
        if(test != null) {
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < test.length(); i++) {
                char c = test.charAt(i);
                try {
                    if (ttf.hasGlyph(String.valueOf(c))) {
                        b.append(c);
                    } else {
                        b.append(" ");
                    }
                } catch (IOException e) {
                    logger.debug("remove special char : " + c);
                }

            }
            return b.toString();
        } else {
            return null;
        }
    }
}
